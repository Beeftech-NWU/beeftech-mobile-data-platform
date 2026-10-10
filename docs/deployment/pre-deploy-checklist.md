# Pre-deployment checklist

Blockers found in the 2026-10-10 audit. None of these is fixed yet. Tick an item and note the PR when it is.

## Backend (`render.yaml`, `backend/api`)

- [ ] `BEEFTECH_SEED_DEV=true` in `render.yaml` seeds the dev users (`admin`/10001, `fmanager`/20002, `jvdm`/30003), the `dev-site-1` site and dev feed cribs on the hosted backend. Set it to `false`.
- [ ] No persistent disk. SQLite (`./data/beeftech-backend.db`) and the calf photo store are wiped on every deploy. Add a disk, or move to a hosted database.
- [ ] `BEEFTECH_SMTP_SECURITY=none` on port 2525 sends mail unencrypted.
- [ ] `JwtService.kt` falls back to the secret `beeftech-secret`. Fail fast when `BEEFTECH_JWT_SECRET` is missing outside dev.
- [ ] Only `ContentNegotiation` is installed: no CORS, `StatusPages` or rate limiting.
- [ ] JWT issuer, token lifetime and the `0.0.0.0` host are hard-coded.

## Android (`demoapp`, `android/calf-registration`)

- [ ] `usesCleartextTraffic="true"` in the `demoapp` and `calf-registration` manifests applies to release builds. Limit it to debug and add a network security config.
- [ ] `allowBackup="true"` lets the database and preferences be copied out through backup.
- [ ] `MainActivity.kt` (around lines 173-210) seeds a demo calf (`Blu0000001`, device `demo-device`) in every build, release included. Put it behind `BuildConfig.DEBUG`.
- [ ] `MainActivity.kt` (around line 111) uses the hard-coded demo SQLCipher passphrase. `SecureDatabaseInitializer`, `DatabaseKeyProvider` and `AndroidKeyStoreSecurityProvider` exist but are not wired in. Wire them in and plan the migration of an existing demo-passphrase database, without losing data (see `docs/database/database-rules.md`).
- [ ] Release build: `isMinifyEnabled=false`, no signing config, `versionCode=1`, and the applicationId is still `com.beeftech.demoapp`.
- [ ] `database/BackendConfig.kt` and `demoapp/build.gradle.kts` both hard-code the `onrender.com` URL.
- [ ] `FarmerSyncWorker` logs farmer and queue ids at info level.

## Process

- [ ] Make the "Unit tests" and "Database migration tests (emulator)" CI jobs required checks (see `docs/database/future-checks.md`, item 2).
