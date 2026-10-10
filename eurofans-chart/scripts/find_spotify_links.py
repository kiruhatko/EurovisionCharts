#!/usr/bin/env python3
"""One-off: find the Spotify artist profile for every entry in eurovision-artists.json.

Run on a machine with internet access and the app's Spotify credentials (read from the
environment or from eurofans-chart/.env):

    python3 scripts/find_spotify_links.py

Nothing in the repo is modified. The result goes to spotify-report.json; that file is then
reviewed and the confirmed links are written into eurovision-artists.json as plain URLs, so the
bot itself never searches Spotify. Safe to re-run: entries already decided in an existing report
are skipped, so an interrupted run (rate limit, network) resumes where it stopped.

A profile is only accepted when its name matches exactly (same normalization as the bot) AND it
is the only such profile, or the only one with releases around the entry's Eurovision year.
Everything else is reported as "ambiguous" or "not_found" with candidates, never guessed.
"""

import base64
import json
import os
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
ARTISTS_JSON = os.path.join(PROJECT, "src", "main", "resources", "eurovision-artists.json")
REPORT = os.path.join(PROJECT, "spotify-report.json")
DELAY_SECONDS = 0.25
SEARCH_LIMIT = 10
DECIDED = {"exact", "year", "split", "existing"}


def norm(text):
    text = unicodedata.normalize("NFKD", text or "")
    out = []
    for ch in text:
        if unicodedata.category(ch) == "Mn":
            continue
        if ch.isalnum():
            out.append(ch.lower())
        elif ch.isspace() and out and out[-1] != " ":
            out.append(" ")
    return "".join(out).strip()


def load_credentials():
    values = {k: os.environ.get(k) for k in ("SPOTIFY_CLIENT_ID", "SPOTIFY_CLIENT_SECRET")}
    env_file = os.path.join(PROJECT, ".env")
    if (not values["SPOTIFY_CLIENT_ID"] or not values["SPOTIFY_CLIENT_SECRET"]) and os.path.exists(env_file):
        with open(env_file, encoding="utf-8") as fh:
            for line in fh:
                key, sep, value = line.strip().partition("=")
                if sep and key in values and not values[key]:
                    values[key] = value.strip().strip('"').strip("'")
    if not values["SPOTIFY_CLIENT_ID"] or not values["SPOTIFY_CLIENT_SECRET"]:
        sys.exit("SPOTIFY_CLIENT_ID / SPOTIFY_CLIENT_SECRET not found in the environment or .env")
    return values["SPOTIFY_CLIENT_ID"], values["SPOTIFY_CLIENT_SECRET"]


class Spotify:
    def __init__(self, client_id, client_secret):
        self.basic = base64.b64encode(f"{client_id}:{client_secret}".encode()).decode()
        self.token = None
        self.token_expires = 0

    def _refresh_token(self):
        req = urllib.request.Request(
            "https://accounts.spotify.com/api/token",
            data=b"grant_type=client_credentials",
            headers={"Authorization": f"Basic {self.basic}", "Content-Type": "application/x-www-form-urlencoded"},
        )
        with urllib.request.urlopen(req, timeout=30) as resp:
            body = json.load(resp)
        self.token = body["access_token"]
        self.token_expires = time.time() + body.get("expires_in", 3600) - 60

    def get(self, path, params):
        url = "https://api.spotify.com/v1" + path + "?" + urllib.parse.urlencode(params)
        for attempt in range(6):
            if not self.token or time.time() > self.token_expires:
                self._refresh_token()
            time.sleep(DELAY_SECONDS)
            req = urllib.request.Request(url, headers={"Authorization": f"Bearer {self.token}"})
            try:
                with urllib.request.urlopen(req, timeout=30) as resp:
                    return json.load(resp)
            except urllib.error.HTTPError as e:
                if e.code == 401:
                    self.token = None
                    continue
                if e.code == 429:
                    wait = int(e.headers.get("Retry-After", "5")) + 1
                    if wait > 600:
                        raise RateLimitedForLong(wait)
                    print(f"  rate limited, waiting {wait}s", flush=True)
                    time.sleep(wait)
                    continue
                if e.code >= 500:
                    time.sleep(2 ** attempt)
                    continue
                raise
            except urllib.error.URLError:
                time.sleep(2 ** attempt)
        raise RuntimeError(f"Spotify request kept failing: {url}")

    def search_artists(self, query):
        data = self.get("/search", {"q": query, "type": "artist", "limit": SEARCH_LIMIT})
        return [a for a in (data.get("artists") or {}).get("items") or [] if a]

    def search_tracks(self, query):
        data = self.get("/search", {"q": query, "type": "track", "limit": SEARCH_LIMIT})
        return [t for t in (data.get("tracks") or {}).get("items") or [] if t]


class RateLimitedForLong(Exception):
    def __init__(self, seconds):
        super().__init__(seconds)
        self.seconds = seconds


def brief(artist):
    return {
        "id": artist["id"],
        "name": artist.get("name"),
        "url": f"https://open.spotify.com/artist/{artist['id']}",
        "followers": (artist.get("followers") or {}).get("total"),
        "popularity": artist.get("popularity"),
        "genres": artist.get("genres"),
    }


def ids_with_releases_near(sp, name, year):
    """Artist ids, named exactly `name`, credited on tracks released in the two years up to `year`."""
    target = norm(name)
    found = {}
    query_name = name.replace('"', "")
    for track in sp.search_tracks(f'artist:"{query_name}" year:{year - 1}-{year}'):
        for artist in track.get("artists") or []:
            if artist.get("id") and norm(artist.get("name")) == target:
                found[artist["id"]] = artist
    return found


def resolve_name(sp, name, year):
    """-> (status, [artist dicts], candidates)"""
    target = norm(name)
    if not target:
        return "not_found", [], []
    results = sp.search_artists(name)
    exact = {a["id"]: a for a in results if norm(a.get("name")) == target}
    near = ids_with_releases_near(sp, name, year) if year else {}

    if len(exact) == 1:
        only = next(iter(exact))
        if near and only not in near:
            # the releases around the Eurovision year belong to a different same-named profile
            merged = {**near, **exact}
            return "ambiguous", [], [brief(a) for a in merged.values()]
        return "exact", [exact[only]], []
    if exact:
        both = [exact[i] for i in exact if i in near]
        if len(both) == 1:
            return "year", both, []
        return "ambiguous", [], [brief(a) for a in exact.values()]
    if len(near) == 1:
        return "year", list(near.values()), []
    if len(near) > 1:
        return "ambiguous", [], [brief(a) for a in near.values()]
    return "not_found", [], [brief(a) for a in results[:5]]


SPLIT = re.compile(r"\s+(?:&|and|feat\.?|ft\.?|featuring|with|x|vs\.?)\s+", re.IGNORECASE)


def resolve_entry(sp, entry):
    name = entry["canonicalName"]
    year = entry.get("editionYear")
    status, artists, candidates = resolve_name(sp, name, year)
    if status in ("exact", "year"):
        return {"status": status, "spotify": [brief(a) for a in artists], "candidates": []}

    parts = [p.strip() for p in SPLIT.split(name) if p.strip()]
    if status == "not_found" and len(parts) > 1:
        resolved = []
        for part in parts:
            part_status, part_artists, _ = resolve_name(sp, part, year)
            if part_status not in ("exact", "year"):
                resolved = None
                break
            resolved.extend(part_artists)
        if resolved:
            return {"status": "split", "spotify": [brief(a) for a in resolved], "candidates": []}

    return {"status": status, "spotify": [], "candidates": candidates}


def main():
    client_id, client_secret = load_credentials()
    sp = Spotify(client_id, client_secret)
    with open(ARTISTS_JSON, encoding="utf-8") as fh:
        entries = json.load(fh)

    report = {}
    if os.path.exists(REPORT):
        with open(REPORT, encoding="utf-8") as fh:
            report = {r["canonicalName"] + "|" + str(r["editionYear"]): r for r in json.load(fh)}

    def save():
        with open(REPORT, "w", encoding="utf-8") as fh:
            json.dump(list(report.values()), fh, ensure_ascii=False, indent=1)

    try:
        for index, entry in enumerate(entries, 1):
            key = entry["canonicalName"] + "|" + str(entry.get("editionYear"))
            if key in report and report[key]["status"] in DECIDED:
                continue
            existing = [u for u in entry.get("urls") or [] if "open.spotify.com/artist/" in u]
            if existing:
                result = {"status": "existing", "spotify": [{"url": u} for u in existing], "candidates": []}
            else:
                result = resolve_entry(sp, entry)
            report[key] = {"canonicalName": entry["canonicalName"], "editionYear": entry.get("editionYear"), **result}
            found = ", ".join(a.get("name") or a.get("url") for a in result["spotify"]) or "-"
            print(f"[{index}/{len(entries)}] {result['status']:<9} {entry['canonicalName']} -> {found}", flush=True)
            if index % 20 == 0:
                save()
    except RateLimitedForLong as e:
        save()
        sys.exit(f"Spotify asked to wait {e.seconds}s (quota). Progress saved; re-run later to continue.")
    except KeyboardInterrupt:
        save()
        sys.exit("Interrupted. Progress saved; re-run to continue.")
    save()

    counts = {}
    for r in report.values():
        counts[r["status"]] = counts.get(r["status"], 0) + 1
    print("\nDone:", ", ".join(f"{k}={v}" for k, v in sorted(counts.items())))
    print("Report:", REPORT)


if __name__ == "__main__":
    main()
