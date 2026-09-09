# Eurofans UA Chart

Eurovision multi-platform listening analytics, as a Telegram bot. A modular
monolith (Java / Spring Boot) tracking Eurovision-artist listens across
Spotify, Apple Music, SoundCloud, and Last.fm, with a group-locked Telegram
bot, per-user chart participation, and a strict identity-resolution engine
that never guesses which real-world artist a play belongs to.

See [deployment.md](deployment.md) for setup, and note its "Known deviations
from the spec" section up top before deploying.

## Module layout

```
com.eurovision.analytics
├── deployment      Ed25519 group-binding verification (fail-closed)
├── security        token encryption, SSRF guard, security events, rate limiting
├── telegram        bot wiring, command dispatch, keyboards
├── user            Telegram-identified users
├── oauth           PKCE/state machinery + one package per provider
├── connectedaccount per-user, per-provider connection state
├── musicbrainz     crosscheck + recording/release lookups
├── artwork         Cover Art Archive + provider-native artwork resolution
├── eurovision       editions/countries/artists/aliases/external ids
│   └── identity     import pipeline + resolution engine + unresolved queue
├── listening        provider-agnostic ingestion + one sync scheduler per provider
├── nowplaying       the Spotify > Apple Music > SoundCloud > Last.fm priority chain
├── chart            ranking, growth %, snapshots, per-user standing
└── admin            audit log
```

## Building

```bash
mvn clean verify
```

Requires a JDK matching `pom.xml`'s `<java.version>` (currently 21 -- see
deployment.md for why, and how to move to the spec's target of 26).
