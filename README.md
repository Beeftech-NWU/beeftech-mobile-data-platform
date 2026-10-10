# 🐂 BeefTech Mobile Data Collection Platform

An offline-first Android platform for **BeefTech (Pty) Ltd.** — feedlot management,
livestock traceability and farm data capture for field operations with limited or no
connectivity, plus a Kotlin/Ktor backend that field devices sync into.

This repository is a single Gradle build containing **eight Android library modules**,
one installable **demo app** that wires them together, and a **Ktor backend**
(plus two empty backend placeholder modules).

---

## Table of contents

- [Quick start](#quick-start)
- [Prerequisites](#prerequisites)
- [Project structure](#project-structure)
- [Module reference](#module-reference)
- [Developer workflow](#developer-workflow)
  - [Building](#building)
  - [Running the demo app](#running-the-demo-app)
  - [Running the backend](#running-the-backend)
  - [Connecting the app to the backend](#connecting-the-app-to-the-backend)
  - [Testing the app step-by-step](#testing-the-app-step-by-step)
  - [Testing](#testing)
  - [Useful Gradle commands](#useful-gradle-commands)
- [Architecture](#architecture)
- [Database & migrations](#database--migrations)
- [Backend API reference](#backend-api-reference)
- [Toolchain & versions](#toolchain--versions)
- [Contributing](#contributing)
- [Troubleshooting](#troubleshooting)
- [Known gaps](#known-gaps)

---

## Quick start

```bash
git clone https://github.com/Beeftech-NWU/beeftech-mobile-data-platform.git
cd beeftech-mobile-data-platform

# 1. Point Gradle at your Android SDK
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # macOS: $HOME/Library/Android/sdk

# 2. Start a local backend with dev users seeded (terminal 1).
#    Debug builds of the demo app talk to http://10.0.2.2:8081/ (this backend, seen from
#    the emulator); see "Connecting the app to the backend".
./gradlew :backend:api:run -Dbeeftech.seed.dev=true   # serves on http://0.0.0.0:8081 (admin / 10001)

# 3. Build and install the demo app on a running emulator (terminal 2)
./gradlew :demoapp:installDebug

# 4. Run the fast checks (same as CI)
./gradlew testDebugUnitTest test
```

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| JDK | **17** | The Gradle daemon toolchain is pinned to Java 17 in `gradle/gradle-daemon-jvm.properties`. All modules compile to JVM target 17. A newer JDK on `PATH` is fine as long as a JDK 17 is *installed* — otherwise Gradle tries to download one from foojay.io and fails on restricted networks. |
| Android SDK | API 35 installed, build-tools for AGP 8.13.2 | Modules use `compileSdk` 34/35, `minSdk` 24 (23 for `:android:database`). |
| Android Studio | Ladybug or newer | Needed for AGP 8.13.2 / Kotlin 2.0.21 support. Optional if you only use the CLI. |
| Gradle | Provided by the wrapper (8.13) | Always use `./gradlew`, never a system `gradle`. |

`local.properties` is git-ignored and **must** be created locally — the Android
modules will not configure without `sdk.dir`.

> Verify your setup with `./gradlew --version`. "Daemon JVM: Compatible with Java 17"
> means the toolchain pin was satisfied.

---

## Project structure

```text
beeftech-mobile-data-platform/
├── android/                      # Android library modules (no launchable app here)
│   ├── authentication/           # PIN login, AuthGate, SessionStore, AuthApiClient
│   ├── calf-registration/        # 4-step calf capture flow, photos, sync client + worker
│   ├── database/                 # Room + SQLCipher: entities, DAOs, repositories, security, BackendConfig
│   ├── farm-traceability/        # Animal records, movements, treatments, mortalities, costs, purchases, feed location
│   ├── farmer-registration/      # Farmer onboarding Activities, farmer list with sync pills
│   ├── feed-crib/                # Feed bunk reading screens (UI only, in-memory)
│   ├── management/               # Manager/admin screens: dashboard, reports, records review/void, team, admin
│   └── tag-scanner/              # CameraX + ML Kit ear-tag scanning, colour detection, tag parsing
├── backend/
│   ├── api/                      # ✅ Ktor server (see "Backend API reference")
│   ├── authentication/           # ⚠️ placeholder — empty
│   └── sync/                     # ⚠️ placeholder — empty
├── demoapp/                      # ✅ The installable Android app used for demos/testing
├── docs/
│   ├── database/                 # incident write-up, future-checks, integrity audit, schema diagram, R4 plans
│   ├── design/                   # field-ready UI redesign plan
│   └── requirements/             # client feedback gaps, Req 6 farmer sales email plan
├── gradle/
│   ├── libs.versions.toml        # Version catalog
│   ├── gradle-daemon-jvm.properties
│   └── wrapper/                  # Gradle 8.13 wrapper
├── .github/workflows/ci.yml      # Unit tests, Android Lint, database migration tests (emulator)
├── Dockerfile / render.yaml      # Backend deployment to Render
├── build.gradle.kts              # Root build — declares plugin versions, applies none
├── settings.gradle.kts           # Module list + repository configuration
└── gradlew / gradlew.bat
```

**Only `:demoapp` is an Android application.** Everything under `android/` is a library
module. `backend/authentication` and `backend/sync` are empty `.gitkeep` placeholders
declared in `settings.gradle.kts` — they configure but build nothing.
`docs/architecture/` and `docs/testing/` are still empty.

> **Note:** `android/` also contains a leftover standalone Gradle setup
> (`android/gradlew`, `android/gradlew.bat`, `android/gradle/wrapper/` pointing at
> Gradle 9.3.0, `android/settings.gradle.kts.backup`, `android/gradle.properties`) from
> before the modules were merged into this root build. **Ignore them** — always build
> from the repository root.

---

## Module reference

| Gradle path | Type | Namespace | Depends on | Status |
|---|---|---|---|---|
| `:demoapp` | app | `com.beeftech.demoapp` | `authentication`, `calf-registration`, `database`, `farm-traceability`, `farmer-registration`, `feed-crib`, `management` | Runnable (gated behind PIN login and the Day-7 sync policy) |
| `:android:database` | library | `com.beeftech.database` | — | Core; Room + SQLCipher, repositories, security, `BackendConfig` |
| `:android:authentication` | library | `com.beeftech.authentication` | `database` | Online/offline PIN login, `AuthGate`, `SessionStore` / `EncryptedSessionStore` |
| `:android:calf-registration` | library | `com.beeftech.calfregistration` | `database`, `tag-scanner` | Room-backed; authenticated sync incl. photo upload |
| `:android:farm-traceability` | library | `com.beeftech.farmtraceability` | `database`, `tag-scanner` | Room-backed; authenticated sync (movements, treatments, mortalities, costs, outbox, farmer–animal assignments) |
| `:android:farmer-registration` | library | `com.beeftech.farmerregistration` | `database` | Room-backed; farmer list, authenticated sync (triggers the sales email) |
| `:android:feed-crib` | library | `com.beeftech.feedcrib` | `database` (declared, unused) | UI only; hard-coded pens, readings live in Compose state |
| `:android:management` | library | `com.beeftech.management` | `database` | Online-only manager/admin screens over `ManagementApiClient` |
| `:android:tag-scanner` | library | `com.beeftech.tagscanner` | `database` | Ear-tag scanner screen/dialog, text parser, colour classifier |
| `:backend:api` | JVM app | `com.beeftech.backend.api` | — | Runnable Ktor server |
| `:backend:authentication`, `:backend:sync` | — | — | — | Empty placeholders |

### `:android:database` — the core module

Everything persistent lives here:

- `BeefTechDatabase` — Room database, **schema version 46** (`BeefTechDatabase.VERSION`; see
  [Database & migrations](#database--migrations))
- `entity/` — 45 Room entities: animals and their identifiers, media, weights,
  ownership, purchases, groups and memberships, farmer–animal links; calf registrations, treatments,
  mortalities, movements, costs; farmers, addresses and roles; locations and pens;
  feed cribs, readings and rations; users and roles; lookups (breeds, hide colours,
  diseases, medications and batches, necropsy codes, countries, provinces, devices,
  identifier types, reference items, device config); sync bookkeeping (pending sync,
  batches, backups, runs, policy state, security events)
- `dao/` — 38 DAOs (most one per file; the lookup DAOs share `LookupDaos.kt`)
- `repository/` — `AnimalHistoryRepository`, `AnimalManagementRepository`,
  `FarmerRepository`, `FeedingRepository`, `LocationRepository`,
  `PendingSyncRepository`, `SyncRepository`, `SyncRunRepository`, `SyncRunSummary`,
  `SyncPolicyEnforcer`, `SyncPolicyStore`, `SyncWarningPolicy`
- `security/` — `AndroidKeyStoreSecurityProvider`, `DatabaseKeyProvider`,
  `DatabaseSecurityProvider`, `SecureDatabaseInitializer`,
  `SecureDatabasePassphraseStore`, `CredentialHasher`, `PinLockoutManager`,
  `TokenProvider` / `TokenProviderRegistry`, `CurrentUserIdRegistry`,
  `SyncIdentityRegistry`, `UnauthorizedReason`
- `BackendConfig` — the process-wide backend base URL every API client defaults to
- `DatabaseProvider.initialize(context, passphrase: ByteArray)` — the single entry point;
  returns a `DatabaseResult`

### `:android:management`

Screens reached from **More** in the demo app. They call the backend directly through
`ManagementApiClient` and show a "needs connection" notice when offline.

- **Managers:** Dashboard, Reports (JSON/CSV/PDF, shareable), Records (review and
  **void with a reason** — the only correction mechanism; there are no edits), Team
  (workers on their site: create, reset PIN, unlock, unbind phone).
- **Admins:** everything above plus **Admin**: Sites, Phones (revoke/reinstate), Login
  security, Reference data, Sync policy, Sync security, Audit log.
- `data/` also holds the device-side pulls used by `DeviceCheckInWorker`:
  `ReferenceDataSync`, `SyncPolicySync`, `SecurityEventSync`, `SyncLockRelease`.

---

## Developer workflow

All commands run from the **repository root** and use the wrapper.
On Windows use `gradlew.bat` in place of `./gradlew`.

### Building

```bash
./gradlew build                        # everything: compile + unit tests + lint
./gradlew assembleDebug                # all debug artifacts
./gradlew :demoapp:assembleDebug       # just the demo APK
./gradlew :android:database:assemble   # a single library module
./gradlew clean                        # wipe build outputs
```

The debug APK lands in `demoapp/build/outputs/apk/debug/`.

### Running the demo app

```bash
# Start an emulator or connect a device first, then:
./gradlew :demoapp:installDebug
adb shell am start -n com.beeftech.demoapp/.MainActivity
```

Or open the repository root in Android Studio and run the **demoapp** configuration.

On start-up, `BeefTechApplication`:

1. sets `BackendConfig` from `BuildConfig.BACKEND_BASE_URL`;
2. registers `EncryptedSessionStore` with `TokenProviderRegistry` / `CurrentUserIdRegistry`;
3. installs `SyncPolicyActivityGuard`, which enforces the Day-7 lock in every Activity;
4. schedules `ScheduledBatchSyncWorker` twice a day (05:00 and 18:00) via
   `ScheduledSyncScheduler`.

`MainActivity` then opens the encrypted database on a background thread, seeds one demo
calf (tag `Blu0000001`) if it does not exist, and shows `PolicyAwareAuthGate`: PIN login
(`AuthGate`), a device check-in, then the `SyncPolicyGate`, which re-evaluates the
unsynced-data policy every 60 seconds.

After login the app has a bottom navigation bar with **Home, Calves, Traceability,
Feed, More**:

- **Home** — quick actions, pending records per module and a **Sync now** button
  (dispatches every sync worker).
- **Calves** — calf registration (tag scan, details, condition, review; registered list
  and detail with retry).
- **Traceability** — farmers (opens the farmer-registration Activities) with **Assign
  animals** (link registered animals to a farmer; assignments sync as
  `farmer_animal_links`), find animal, movements, purchases/suppliers, location & feed,
  treatments, costs, mortalities, and a sync status card with **Retry Sync**.
- **Feed** — the feed crib screens (not persisted).
- **More** — My activity and Log out; managers also get Dashboard, Reports, Records and
  Team; admins additionally get Admin.

> ⚠️ The demo app initialises SQLCipher with the hard-coded passphrase
> `beeftech-demo-passphrase`. This is **demo-only** and must be replaced with the
> Android KeyStore–backed key before any production use.

### Running the backend

```bash
./gradlew :backend:api:run -Dbeeftech.seed.dev=true
```

- Listens on **`0.0.0.0:8081`**
- Creates a SQLite database at **`./data/beeftech-backend.db`** (relative to the working directory)
  and stores calf photos in a `media/` folder next to it
- Seeds reference data (diseases, treatment types, cost types) on **every** start;
  rows an admin deactivated stay deactivated
- `-Dbeeftech.seed.dev=true` also seeds a dev site and dev users if they do not exist:

| Username | 5-digit PIN | Role | Site |
|---|---|---|---|
| **`admin`** | `10001` | 1 — Administrator | none (sees all sites) |
| **`fmanager`** | `20002` | 2 — Manager | `dev-site-1` "Dev Feedlot" (farm code `S001`) |
| **`jvdm`** | `30003` | 3 — Worker | `dev-site-1` |

- Override the database location with:
  ```bash
  ./gradlew :backend:api:run -Dbeeftech.seed.dev=true -Dbeeftech.db.url="jdbc:sqlite:/tmp/beeftech.db"
  ```
  `beeftech.*` `-D` flags are forwarded to `run`. For the installed start script
  (`build/install/api/bin/api`) pass them through `JAVA_OPTS`.

#### Backend configuration

An environment variable wins over the matching `-D` system property. There is no
`application.conf`.

| Env var | System property | Default / purpose |
|---|---|---|
| `PORT` | — | `8081` |
| `BEEFTECH_DB_URL` | `beeftech.db.url` | `jdbc:sqlite:./data/beeftech-backend.db` |
| `BEEFTECH_SEED_DEV` | `beeftech.seed.dev` | Seed the dev site and users when `true`. If the env var is set at all it wins, so `BEEFTECH_SEED_DEV=false` disables seeding even with `-D…=true` |
| `BEEFTECH_MEDIA_DIR` | `beeftech.media.dir` | Where uploaded calf photos are stored; defaults to a `media` folder next to the SQLite database |
| `BEEFTECH_JWT_SECRET` | — | Falls back to a dev secret with a warning; always set in production |
| `BEEFTECH_SMTP_PROVIDER` | — | `custom` (default), `gmail`, or `outlook` (aliases `microsoft`, `microsoft365`, `office365`). `gmail`/`outlook` fix the host |
| `BEEFTECH_SMTP_HOST` | — | SMTP host; used only with `custom` |
| `BEEFTECH_SMTP_SECURITY` | — | `starttls` (default, alias `tls`), `ssl` (alias `smtps`) or `none` (alias `plain`) |
| `BEEFTECH_SMTP_PORT` | — | Defaults to 587 / 465 / 25 for starttls / ssl / none |
| `BEEFTECH_SMTP_USERNAME` / `BEEFTECH_SMTP_PASSWORD` | — | Set both or neither; required for `gmail` / `outlook` |
| `BEEFTECH_SMTP_FROM` | — | Sender address; defaults to the username |
| `BEEFTECH_SALES_REP_EMAIL` | — | Fallback recipient of the farmer sales notification, used when the farmer's site has no sales rep email |

If the SMTP settings are incomplete, the farmer sales notification is **logged instead
of sent** and the farmer stays un-notified, so a later sync retries it. Each farmer is
emailed at most once (claimed via `farmers.sales_notified_at`); the email carries a JSON
attachment named after the site's farm code. A separate "BeefTech Calf Registration"
email (JSON attachment `calf-registration-<guid>.json`) is sent to the same recipient
when a calf is registered.

Deployment: the `Dockerfile` builds `:backend:api:installDist` on Temurin 17 and runs
`/app/bin/api`; `render.yaml` deploys it to Render as `beeftech-backend` (health check
`/health`, generated `BEEFTECH_JWT_SECRET`, Brevo SMTP relay, `BEEFTECH_SEED_DEV=true`).
No persistent disk is declared, so the hosted SQLite database and photos reset on every
redeploy or restart.

Smoke-test it:

```bash
curl http://localhost:8081/
# BeefTech Backend API is running

curl -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"jvdm","pin":"30003","device_id":"TEST_DEV"}'
```

`device_id` is required. The first successful login binds the user to that device; a
login from another device gets `409` until a manager unbinds it. Five wrong PINs lock
the account on the server for 5 minutes (`423`).

### Connecting the app to the backend

Every API client defaults its `baseUrl` to `BackendConfig.baseUrl`, which the demo app
sets from `BuildConfig.BACKEND_BASE_URL` (`demoapp/build.gradle.kts`):

| Build type | Backend |
|---|---|
| `debug` | `http://10.0.2.2:8081/` — the host machine's local backend, seen from the **emulator** |
| `release` | `https://beeftech-backend.onrender.com/` — the Render deployment |

From a **physical device**, point a debug build at your workstation's LAN address:

```bash
./gradlew :demoapp:installDebug -Pbeeftech.baseUrl=http://192.168.1.20:8081/
```

To use the hosted backend from a debug build, pass
`-Pbeeftech.baseUrl=https://beeftech-backend.onrender.com/`. Cleartext HTTP is allowed
by `android:usesCleartextTraffic="true"` in the `demoapp` and `calf-registration`
manifests.

API clients obtain their JWT from the logged-in session (`SessionStore` via
`TokenProviderRegistry`). A `401` from the server ends the session and returns the user
to the login screen.

### Testing the app step-by-step

Follow these steps to test online PIN login, data capture, backend synchronisation,
offline login, lockout and the manager features.

#### Step 1: Start the backend server with dev user seeding
In terminal 1:
```bash
./gradlew :backend:api:run -Dbeeftech.seed.dev=true
curl http://localhost:8081/
# Output: BeefTech Backend API is running
```

#### Step 2: Launch the demo app
Start an Android emulator (or connect a device and pass `-Pbeeftech.baseUrl`, see
above) and in terminal 2:
```bash
./gradlew :demoapp:installDebug
adb shell am start -n com.beeftech.demoapp/.MainActivity
```
*(Or click **Run demoapp** in Android Studio.)*

#### Step 3: Log in via PIN authentication (`AuthGate`)
1. The app opens the encrypted database and shows the **PIN login screen**.
2. Enter a seeded user, e.g. `jvdm` / `30003` (worker), `fmanager` / `20002` (manager)
   or `admin` / `10001` (admin).
3. Tap **Sign In**. The app posts to `POST /api/auth/login`, stores the JWT in
   `EncryptedSessionStore` and caches the BCrypt PIN hash in Room.

#### Step 4: Capture data & sync
1. **Calves**: register a calf (scan or type a tag, details, condition, review, save).
   The record is written to SQLCipher and queued in `PendingSync`.
2. **Traceability**: record a movement, treatment, cost or mortality; register a farmer
   from *Farmer Registration*.
3. Tap **Sync now** on Home (or **Retry Sync** in Traceability). The sync workers post
   batches to the `/sync` endpoints with `Authorization: Bearer <jwt>` and mark records
   `SYNCED`. The farmer list shows *Pending Sync → Processing → Registered*.

#### Step 5: Test offline support
1. Enable **Airplane Mode** or stop the backend (`Ctrl+C`), then relaunch the app.
2. Log in as `jvdm` / `30003`. With no network the PIN is verified against the cached
   BCrypt hash (offline sessions last up to 7 days; a user must log in online once first).
3. Capture a record; it stays `PENDING`.
4. Restore the network. Sync runs on **Sync now**, on the 05:00/18:00 schedule, or from
   the per-feature workers once the network constraint is met.

Unsynced data raises warnings on days 2, 4 and 6 (the server can move these). On
**day 7** the app wipes the pending local operations and locks the account until an
admin clears the lock (`POST /api/users/{id}/clear-sync-lock`) — the day is fixed in
`SyncWarningPolicy.WIPE_DAY` and cannot be changed by the server.

#### Step 6: Test PIN lockout
1. Enter 5 wrong PINs in a row.
2. The device blocks further attempts for **1 minute** (`PinLockoutManager`). Online, the
   server separately locks the account for **5 minutes** after 5 failures (`423`); a
   manager can lift it from Team (unlock or reset PIN).

#### Step 7: Manager and admin features
1. Log in as `fmanager` (needs the network). Under **More** open Dashboard, Reports,
   Records and Team.
2. In **Records**, void a record with a reason. It disappears from scoped reads and is
   written to the audit log.
3. Log in as `admin` to reach **More → Admin** (sites and their sales rep email, phones,
   login security, reference data, sync policy, sync security, audit log).

### Testing

| Command | What it runs | Needs a device? |
|---|---|---|
| `./gradlew testDebugUnitTest test` | All JVM unit tests (exactly what CI runs) | No |
| `./gradlew :backend:api:test` | Ktor route, auth, admin, email and schema-migration tests (JUnit Platform) | No |
| `./gradlew :android:calf-registration:testDebugUnitTest` | Repository, mapper, ViewModel, API client, image and tag-naming tests | No |
| `./gradlew lintDebug` | Android Lint (a CI job) | No |
| `./gradlew connectedAndroidTest` | All instrumented tests | **Yes** |
| `./gradlew :android:database:connectedAndroidTest` | SQLCipher, keystore, DAO, sync and migration tests (50 files) | **Yes** |
| `./gradlew :android:tag-scanner:connectedAndroidTest` | Ear-tag recognition over sample images | **Yes** |

Where the tests live:

```text
src/test/        → JVM unit tests: every Android module except feed-crib, plus demoapp
                   and backend/api (~37 classes)
src/androidTest/ → instrumented tests: database (the real suite), tag-scanner (sample
                   images), authentication (unused biometric manager), and template
                   tests in farmer-registration and demoapp
```

The backend test `FarmerSalesNotificationLiveTest` sends a real email and is skipped
unless `BEEFTECH_SMTP_LIVE_TEST=true`.

CI (`.github/workflows/ci.yml`, on push and PR to `main`) runs three jobs:
**Unit tests** (`./gradlew testDebugUnitTest test --continue`), **Android Lint**
(`./gradlew lintDebug --continue`) and **Database migration tests (emulator)** (API 30,
`:android:database:connectedAndroidTest`). None is a required check yet.

Run a single test class:

```bash
./gradlew :android:calf-registration:testDebugUnitTest \
  --tests "com.beeftech.calfregistration.util.TagNamingUtilsTest"
./gradlew :backend:api:test --tests "com.beeftech.backend.api.VoidRoutesTest"
```

### Useful Gradle commands

```bash
./gradlew projects                     # list every module in the build
./gradlew :demoapp:dependencies        # dependency tree for a module
./gradlew :demoapp:lintDebug           # Android Lint
./gradlew tasks --all                  # every available task
./gradlew build --scan                 # publish a build scan for debugging
./gradlew --stop                       # kill the Gradle daemon
```

---

## Architecture

```text
┌──────────────────────── Android device (offline-capable) ────────────────────────┐
│                                                                                  │
│  Compose UI          ViewModel            Repository           Room + SQLCipher  │
│  (feature module) →  (feature module) →  (:android:database) → encrypted SQLite  │
│                                                  │                               │
│                                                  ▼                               │
│                                          PendingSync table (status = PENDING)    │
│                                                  │                               │
│                    SyncAllDispatcher → per-feature WorkManager sync workers      │
└──────────────────────────────────────────────────┬───────────────────────────────┘
                                                   │ HTTPS/JSON (Ktor client, Bearer JWT)
                                                   ▼
┌────────────────────────── backend:api (Ktor + Netty, :8081) ─────────────────────┐
│  JWT + live user check → routes → service → Exposed → SQLite                     │
│  Idempotent upsert by record GUID, stamped with the caller's user and site       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

1. **Capture** — Compose screens validate at the point of entry.
2. **Encrypted local write** — records are written immediately through Room to a
   SQLCipher-encrypted SQLite file; audit metadata (GPS, capture timestamp, device ID,
   record GUID) is embedded on the row.
3. **Queue** — unsynced rows are tracked in `PendingSync` with status `PENDING`.
4. **Sync** — `SyncAllDispatcher` (Sync now, Retry Sync, and the 05:00/18:00
   `ScheduledBatchSyncWorker`) enqueues `FarmerSyncWorker`,
   `CalfRegistrationSyncWorker`, `TreatmentSyncWorker`, `AnimalMovementSyncWorker`,
   `MortalitySyncWorker`, `CostSyncWorker`, `FarmerAnimalLinkSyncWorker` and
   `TraceabilityOutboxWorker`. Each posts
   batches with the session JWT and an optional batch name
   `[FarmCode]-[Project]-[YYYYMMDD]-[HHMMSS]-[DeviceID]`, which the server validates.
5. **Acknowledge** — the backend upserts by GUID (so retries cannot duplicate), stamps
   the submitter and site from the token, and the device flips the record to `SYNCED`.
6. **Check-in** — `DeviceCheckInWorker` pulls reference data and the sync policy and
   uploads sync security events.

On the server, reads are **scoped by role**: an admin sees everything, a manager sees
their own site, and a worker sees only their own records. Voided records are hidden from
scoped reads.

---

## Database & migrations

- **Room schema version: 46** (`BeefTechDatabase.VERSION` is the source of truth)
- Migrations `1→2` … `8→9` are defined in
  `android/database/src/main/java/com/beeftech/database/DatabaseFactory.kt`.
  Migrations `9→10` through `45→46` live in the `BeefTechDatabase` companion object in
  `BeefTechDatabase.kt`. Every migration is registered in `DatabaseFactory.kt`
  `.addMigrations(...)` wrapped in `guarded(...)`.
  Version 15 is deliberately unused (see the comment above `MIGRATION_14_16`).
- Exported schema JSON is committed under
  `android/database/schemas/com.beeftech.database.BeefTechDatabase/` for versions 14
  and 16–46.
- Encryption: SQLCipher for Android 4.17.0, key material via Android KeyStore
  (`AndroidKeyStoreSecurityProvider`)

**Non-negotiable rules.** Field devices can hold unsynced data for long periods, and
losing it is the worst failure this app can have (a real incident is written up in
[`docs/database/incident-2026-09-r0-migration-data-loss.md`](docs/database/incident-2026-09-r0-migration-data-loss.md)).
The only deliberate deletion of unsynced data is the audited Day-7 policy wipe
(`SyncSecurityDao`); migrations must never lose data.

- Never use a destructive migration fallback. Every version step needs an explicit
  migration that preserves data.
- Values that cannot be mapped go into quarantine/legacy tables; never drop them.
- Backfill GUIDs before adding a unique index on them.
- Feature modules reach persistence through repositories in `:android:database`, never
  through DAOs directly.

**When you change an entity you must:**

1. Add or edit the entity in `entity/` and its DAO in `dao/`.
2. Bump `BeefTechDatabase.VERSION`.
3. Add `MIGRATION_n_n+1` to the `BeefTechDatabase` companion object.
4. Register it in `DatabaseFactory.kt`'s `.addMigrations(...)`, wrapped in `guarded(...)`.
5. Commit the exported schema JSON under
   `android/database/schemas/com.beeftech.database.BeefTechDatabase/`.
6. Add a migration test using `MigrationTestHelper`, following the templates
   `Migration30To31Test.kt` and `Migration16To17Test.kt` in
   `android/database/src/androidTest/java/com/beeftech/database/`.
7. Run `./gradlew :android:database:connectedAndroidTest` (this also runs in CI, on an
   emulator, as the "Database migration tests (emulator)" job).

Deferred follow-ups are tracked in
[`docs/database/future-checks.md`](docs/database/future-checks.md).

---

## Backend API reference

Base URL: `https://beeftech-backend.onrender.com` (hosted) or `http://<host>:8081`
(local). Code lives in `backend/api/src/main/kotlin/com/beeftech/backend/api/`: one
`*Routes.kt` → `*Service.kt` → `*Repository.kt` → `*Table.kt` set per feature, with auth
in `auth/` and feed crib in `feedCrib/`.

**Auth model.** Every route except the four public ones needs
`Authorization: Bearer <jwt>` (HMAC256, 24 h expiry). There is no Ktor `authenticate {}`
block: each handler validates the token *and* the live user state (inactive user,
revoked device or session → `401`), reloading role and site from the database on every
request. **Role** below means: *any* — any logged-in user; *mgr* — manager or admin
(managers limited to their own site); *admin* — admin only (`403` otherwise).

### Public

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/` | Liveness text response |
| `GET` | `/health` | Health check (Render) |
| `GET` | `/api/farm-traceability` | Liveness text response |
| `POST` | `/api/auth/login` | `{username, pin, device_id, device_model?, app_version?}` → JWT and user (`401` bad credentials, `409` bound to another device, `403` device revoked, `423` locked) |

### Field data

| Method | Path | Role | Purpose |
|---|---|---|---|
| `GET` | `/api/profile` | any | Current username |
| `POST` | `/api/calf-registrations/sync` | any | Batch upsert of calf registrations by GUID |
| `GET` | `/api/calf-registrations` | any | List (scoped) |
| `GET` | `/api/calf-registrations/{tagNumber}` | any | Single calf registration |
| `GET` | `/api/calf-registrations/assignment-candidates` | mgr (site-assigned manager) | Calves on the manager's site that can be assigned to `?farmerId=` (the farmer must be on the same site) |
| `PUT` | `/api/calf-registrations/{tagNumber}/photo` | any | Upload photo: raw `image/jpeg` body, max 5 MB (`413`/`415`); record must be synced and in scope |
| `GET` | `/api/calf-registrations/{tagNumber}/photo` | any | Download the stored photo |
| `GET` | `/api/calf-registrations/{tagNumber}/certificate` | any | Birth-certificate PDF (PDFBox) |
| `POST` | `/api/farmers/sync` | any | Batch upsert of farmers; sends the sales email once per farmer |
| `GET` | `/api/farmers` · `/api/farmers/{farmerId}` | any | List / single farmer |
| `POST` | `/api/treatments/sync` | any | Batch upsert of treatments |
| `GET` | `/api/treatments` · `/api/treatments/{animalId}` | any | List / per animal |
| `GET` | `/api/treatments/reference-data` | any | Legacy treatment reference data (older apps) |
| `POST` | `/api/animal-movements/sync` | any | Batch upsert of animal movements |
| `GET` | `/api/animal-movements` · `/api/animal-movements/{animalId}` | any | List / per animal |
| `POST` | `/api/mortalities/sync` | any | Batch upsert of mortalities |
| `GET` | `/api/mortalities` · `/api/mortalities/{animalId}` | any | List / per animal |
| `POST` | `/api/costs/sync` | any | Batch upsert of animal costs |
| `GET` | `/api/costs` · `/api/costs/{animalId}` | any | List / per animal |
| `POST` | `/api/farmer-animal-links/sync` | any | Upload one farmer–animal assignment (`recordGuid`, `linkId`, `farmerId`, `animalId`, `effectiveFrom`, `effectiveTo?`); checked against the caller's site and the saved link |
| `POST` | `/api/farmer-animal-links/diagnose` | any | Explain why pending assignments (max 50) are not syncing; only reveals records the caller can see |
| `POST` | `/api/traceability-events/sync` | any | Generic outbox (`entityType`, `recordGuid`, `payload`, …), upsert by `recordGuid` |
| `POST` | `/api/feed-crib` | any | Submit one feed crib reading |
| `GET` | `/api/feed-crib` · `/api/feed-crib/{penName}` | any | List / per pen |

### Reference data, sync policy and sync security

| Method | Path | Role | Purpose |
|---|---|---|---|
| `GET` | `/api/reference-data` | any | Diseases, treatment types, cost types; `?ifVersion=N` returns `unchanged` when current |
| `POST` | `/api/reference-data/{kind}` | admin | Add a `diseases` / `treatment-types` / `cost-types` value |
| `PATCH` | `/api/reference-data/{kind}/{id}` | admin | Activate/deactivate a value |
| `GET` | `/api/sync-policy` | any | Warning days and stale-sync hours |
| `PUT` | `/api/sync-policy` | admin | Change warning days (1–6; the day-7 wipe is fixed) and stale-sync hours (12–336) |
| `POST` | `/api/sync-security-events` | any | Device uploads its sync security events (max 200) |
| `GET` | `/api/sync-security-events` · `/api/sync-security-events/locked` | admin | Event log / locked users |
| `POST` | `/api/users/{id}/clear-sync-lock` | admin | Clear a Day-7 lock (audited) |

### Administration

| Method | Path | Role | Purpose |
|---|---|---|---|
| `GET` · `POST` | `/api/users` | mgr | List / create users (managers: workers on their own site) |
| `PATCH` | `/api/users/{id}` | mgr | Update a user (only admins change role or site) |
| `POST` | `/api/users/{id}/reset-pin` | mgr | Reset (or generate) a PIN; also lifts the login lockout |
| `POST` | `/api/users/{id}/unlock-login` | mgr | Lift the login lockout |
| `POST` | `/api/users/{id}/unbind-device` | mgr | Release the user's device binding |
| `GET` | `/api/sites` | mgr | List sites (managers: their own) |
| `POST` · `PATCH` | `/api/sites` · `/api/sites/{id}` | admin | Create / update a site (name, 4-char farm code, `salesRepEmail`) |
| `GET` | `/api/devices` | mgr | List devices |
| `POST` | `/api/devices/{id}/revoke` · `/reinstate` | admin | Revoke / reinstate a device (`{reason}`) |
| `GET` | `/api/login-events` · `/api/login-security/lockouts` | admin | Login history / current lockouts |
| `POST` | `/api/farmer-animal-links/restore-missing-parent` | mgr (site-assigned manager) | Recreate a missing farmer or calf registration that an assignment references, in the manager's site; needs the original record and a `verificationReason` (15–500 chars) |
| `POST` | `/api/records/{type}/{id}/assign-site` | admin | Assign a legacy record without a site to a site |
| `POST` | `/api/records/{type}/{id}/void` | mgr | Void a record with a `{reason}` (≤ 500 chars). `type`: `calf-registrations`, `treatments`, `farmers`, `animal-movements`, `mortalities` |
| `GET` | `/api/records/{type}` | mgr | Review list, including voided records |
| `GET` | `/api/audit-log` | mgr | Audit log with filters |
| `GET` | `/api/dashboard/summary` | mgr | Dashboard figures (`?siteId`) |
| `GET` | `/api/reports/{report}` | mgr | `mortality`, `treatment-cost`, `cost-per-animal`, `calf-registrations`, `worker-productivity`; `?format=json\|csv\|pdf` |

`/api/auth/register` no longer exists; users are created through `POST /api/users`.

---

## Toolchain & versions

| Component | Version |
|---|---|
| Gradle (wrapper) | 8.13 |
| Gradle daemon JVM | Java 17 (pinned) |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.0.21 |
| KSP | 2.0.21-1.0.28 (KSP2 enabled in `gradle.properties`) |
| Compose BOM | 2024.02.00 (hard-coded in most modules) / 2026.02.01 (catalog: `farmer-registration`, `management`) |
| Room | 2.8.4 |
| SQLCipher for Android | 4.17.0 |
| Ktor client | 3.0.3 (2.3.8 in `feed-crib`) |
| Ktor server | 3.0.3 |
| Exposed | 0.56.0 |
| SQLite JDBC | 3.46.1.3 |
| PDFBox | 2.0.30 |
| Jakarta Mail (Angus) | 2.0.3 |
| java-jwt / jBCrypt | 4.4.0 / 0.4 |
| CameraX + ML Kit text recognition | `tag-scanner` (ML Kit 16.0.1) |
| `compileSdk` | 35 (`database`, `farm-traceability`, `farmer-registration`, `management`, `demoapp`) · 34 (`authentication`, `calf-registration`, `feed-crib`, `tag-scanner`) |
| `minSdk` | 24 (23 for `:android:database`) |
| JVM target | 17 everywhere |

Dependency declarations are currently **mixed**: `:android:farmer-registration` and
`:android:management` use the `gradle/libs.versions.toml` version catalog for plugins
and libraries; most other Android modules use it only for plugin aliases and hard-code
library coordinates; `:backend:api` hard-codes everything. New code should prefer the
version catalog.

---

## Contributing

- Branch from `main`; branch names follow `feature/<short-description>`.
- Commit messages follow Conventional Commits with the module as scope, e.g.
  `feat(calf-registration): add photo attachment support`.
- Before opening a PR:
  ```bash
  ./gradlew build                    # compiles every module + runs unit tests
  ./gradlew testDebugUnitTest test   # the unit-test command CI runs
  ./gradlew lintDebug                # Android Lint (also a CI job)
  ```
- Instrumented tests (`connectedAndroidTest`) need an emulator or device; run them
  for any change under `:android:database`.
- Keep new persistence code behind a repository in `:android:database` rather than
  calling DAOs from feature modules directly.
- Add dependencies through `gradle/libs.versions.toml`. Repositories can only be
  declared in `settings.gradle.kts` (`FAIL_ON_PROJECT_REPOS`).

---

## Troubleshooting

**`Unable to download toolchain matching the requirements ({languageVersion=17…})`**
No JDK 17 is installed and Gradle cannot reach foojay.io. Install a JDK 17, or point
Gradle at an existing one:
```bash
./gradlew build -Porg.gradle.java.installations.paths=/path/to/jdk-17
```

**`SDK location not found`**
Create `local.properties` at the repository root with
`sdk.dir=/path/to/Android/Sdk`.

**`Plugin [id: 'com.android.application', version: '8.13.2'] was not found`**
Gradle cannot reach `dl.google.com` / Maven Central. Check your proxy or VPN — the
build declares `FAIL_ON_PROJECT_REPOS`, so repositories can only be configured in
`settings.gradle.kts`.

**Duplicate `META-INF` resource errors**
Already excluded in `:android:database` and `:demoapp` via `packaging { resources { excludes += … } }`.
Add the same exclusion to a new module if it consumes SQLCipher or BouncyCastle.

**App cannot reach the backend**
A debug build targets `http://10.0.2.2:8081/`, which only works on the emulator and only
while the local backend is running. On a physical device, reinstall with
`-Pbeeftech.baseUrl=http://<lan-ip>:8081/` and make sure both machines are on the same
network.

**Login fails with "bound to another device" (`409`)**
The user's first login bound them to a different device (or emulator). Unbind it as a
manager/admin (More → Team, or `POST /api/users/{id}/unbind-device`), or wipe the local
backend database.

**Gradle behaves strangely after a branch switch**
```bash
./gradlew --stop && ./gradlew clean
```

---

## Known gaps

- `backend/authentication` and `backend/sync` are empty placeholders;
  `docs/architecture/` and `docs/testing/` are empty.
- CI runs (see [Testing](#testing)), but no job is a required check yet.
- `:backend:api:test` is flaky: a different single test fails on each run, probably
  because tests share state (N5 in `docs/database/future-checks.md`). Re-run before
  assuming a regression.
- `CalfRegistrationViewModel` and `TreatmentViewModel` re-enqueue the legacy 15-minute
  periodic sync jobs that `ScheduledSyncScheduler` cancels.
- `usesCleartextTraffic="true"` is set in the main manifests, so release builds allow
  cleartext HTTP too.
- The Render deployment has no persistent disk (data resets on redeploy) and seeds the
  dev users.
- The login response includes the user's `pin_hash` (used for offline login).
- `:android:feed-crib` is not persisted or synced; the backend `/api/feed-crib` routes
  are unused by the app.
- The biometric prompt code in `:android:authentication` is not used by the app.
- `backend/api/requests.http` is stale (sends `password`, calls `/api/auth/register`).
- Some `docs/database/*.md` files cite an `AGENT.md` that does not exist; the database
  rules are summarised in [Database & migrations](#database--migrations).
- Demo credentials, the JWT fallback secret and the demo SQLCipher passphrase are
  hard-coded for development.
- Stale standalone Gradle files remain under `android/`.
- Deferred database and backend follow-ups are listed in
  [`docs/database/future-checks.md`](docs/database/future-checks.md).
