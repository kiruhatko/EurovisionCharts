# Deployment

## Known deviations from the spec (please read)

1. **Java version.** The spec targets Java 26. This project was built in a
   sandboxed environment whose network policy blocked Adoptium and whose
   Ubuntu apt repositories did not yet carry an `openjdk-26` package, so a
   JDK 26 toolchain could not be obtained to empirically verify the build (a
   hard requirement the spec itself sets: verify library/API compatibility
   rather than guessing). `pom.xml`'s `<java.version>` is pinned to **21**,
   the highest JDK actually available for verification, and the `Dockerfile`
   matches it (`eclipse-temurin:21-*-alpine`). Nothing in the codebase uses a
   Java-26-only language feature (no preview APIs), so upgrading is a
   two-line change once a JDK 26 toolchain is available in your build
   environment: bump `<java.version>` in `pom.xml` and both image tags in
   `Dockerfile`, then rebuild.
2. **TelegramBots version.** The spec suggested the "9.x" line; the actual
   latest stable release verified live against Maven Central at build time
   was **10.3.0**, which is what this project uses.

Every other library version in `pom.xml` (Spring Boot 4.1.1 / Spring
Framework 7, Jackson 3 under `tools.jackson`, nimbus-jose-jwt 10.9.1,
Testcontainers 2.0.5, etc.) was resolved against the live Maven Central index
at build time, not from memory.

## Prerequisites

- Docker + Docker Compose
- A public HTTPS domain pointed at the app container (behind your own
  reverse proxy / TLS terminator -- not included in `docker-compose.yml`).
  This is a **new** requirement versus a Last.fm-only bot: Spotify, Apple
  Music, and SoundCloud all need a real callback URL.
- A Telegram bot token from @BotFather.
- One offline machine (never the app server) to run
  `scripts/generate-binding.sh`.

## 1. Generate the deployment binding

On a separate, offline machine:

```bash
./scripts/generate-binding.sh <authorized_group_chat_id> production
```

This produces:

- `deployment-binding.json` -- copy it next to `docker-compose.yml` on the
  app server (the compose file mounts it read-only into the container).
- `deployment-binding-public-key.b64` -- its contents become
  `DEPLOYMENT_BINDING_PUBLIC_KEY` in `.env`.
- `<uuid>-ed25519-private.pem` -- **keep this offline forever.** It is only
  needed again if you rotate `AUTHORIZED_GROUP_CHAT_ID`.

`AUTHORIZED_GROUP_CHAT_ID` in `.env` must be the exact same numeric chat ID
you passed to the script: the app cross-checks the env var against the
signed value inside `deployment-binding.json` and refuses to start (fail
closed) if they disagree.

For a personal, single-user deployment with no group lock, set
`DEPLOYMENT_BINDING_ENABLED=false` instead and skip this section entirely.

## 2. Fill in `.env`

Copy `.env.example` to `.env`. Every one of the four provider integrations
(Spotify, Apple Music, SoundCloud, Last.fm) is fully implemented and
activates automatically the moment its credentials are non-empty; leaving a
provider's variables blank makes its `/connect` command reply "not
configured" instead of erroring, and every other command keeps working.

Generate `TOKEN_ENCRYPTION_KEY` with `openssl rand -base64 32`.

## 3. Start the stack

```bash
docker compose up -d --build
```

Postgres and Redis are only reachable on the internal Docker network; only
the `app` service's port 8080 is published (put your TLS-terminating reverse
proxy in front of it).

## 4. Bulk artist import (optional)

`/reloadartists` reads a JSON array (default `classpath:eurovision-artists.json`,
or pass a `file:`/`classpath:` path as an argument) shaped like:

```json
[
  {
    "canonicalName": "Jamala",
    "countryIso": "UA",
    "editionYear": 2016,
    "urls": [
      "https://open.spotify.com/artist/...",
      "https://music.apple.com/us/artist/jamala/...",
      "https://soundcloud.com/jamala",
      "https://www.last.fm/music/Jamala"
    ]
  }
]
```

Unlike the interactive `/addartist <URL>` flow (which always requires an
admin's explicit Confirm/Cancel), a bulk-imported entry goes straight to
`VERIFIED`: the file itself is the admin's confirmation, curated offline
before being loaded.

## Operational notes

- Chart snapshots are written daily and never deleted.
- `TokenRefreshScheduler` proactively refreshes Spotify/SoundCloud tokens
  ~5 minutes before expiry; Apple Music and Last.fm have nothing to refresh.
- Each provider's sync scheduler is isolated: one user's failing token never
  blocks another user's sync, and an account skips syncing after 5
  consecutive failures until the user reconnects.

## What was actually run, not just compiled

Beyond `mvn test` (34 passing JUnit/Mockito/MockWebServer tests), the full
application was booted end-to-end against a real PostgreSQL 16 and Redis
(with a real `scripts/generate-binding.sh`-produced binding file) during
development, which caught two bugs no unit test would have:

- Spring Boot 4.1 split Flyway's autoconfiguration into its own
  `spring-boot-starter-flyway` artifact, separate from the `flyway-core`
  library dependency itself -- without it, migrations silently never run
  and Hibernate schema validation fails on every table.
- A JPQL `(:from is null or ...)` pattern for `ALL_TIME`'s unbounded window
  fails against PostgreSQL with "could not determine data type of parameter
  $1"; the fix binds a concrete `Instant.EPOCH` instead of `null` and drops
  the `is null` branch entirely.

That boot also confirmed: all Flyway migrations apply cleanly and match the
JPA entity mappings exactly (`hibernate.ddl-auto: validate` passes), the
daily chart-snapshot job runs without error on an empty database, the
deployment-binding verifier's audit row is written correctly, the OAuth
callback endpoints return clean 4xx responses (never a stack trace) for
invalid/expired state, and `/actuator/health` reports `UP`.
