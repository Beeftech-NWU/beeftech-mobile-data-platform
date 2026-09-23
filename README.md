# 🐂 BeefTech Mobile Data Collection Platform

An offline-first Android platform for **BeefTech (Pty) Ltd.** — feedlot management,
livestock traceability and farm data capture for field operations with limited or no
connectivity, plus a local Kotlin/Ktor backend that field devices sync into.

This repository is a single Gradle build containing **seven Android modules**, a
**Ktor backend**, and a **demo app** that wires the finished modules together.

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

---

## Quick start

```bash
git clone https://github.com/Beeftech-NWU/beeftech-mobile-data-platform.git
cd beeftech-mobile-data-platform

# 1. Point Gradle at your Android SDK
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # macOS: $HOME/Library/Android/sdk

# 2. Start the backend with dev users seeded (terminal 1)
./gradlew :backend:api:run -Dbeeftech.seed.dev=true   # serves on http://0.0.0.0:8081 (admin / 10001)

# 3. Build and install the demo app on a running emulator (terminal 2)
./gradlew :demoapp:installDebug

# 4. Run the fast checks
./gradlew test
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
│   ├── app/                      # ⚠️ placeholder — empty, reserved for the production app
│   ├── authentication/           # Biometric / device authentication UI + manager
│   ├── calf-registration/        # Calf capture flow, sync client, tag utilities
│   ├── database/                 # Room + SQLCipher: entities, DAOs, repositories, keystore
│   ├── farm-traceability/        # Animal records, movements, treatments, mortalities, costs
│   ├── farmer-registration/      # Farmer/client onboarding screens
│   └── feed-crib/                # Feed bunk reading screens
├── backend/
│   ├── api/                      # ✅ Ktor server: auth, calf registration, feed crib, PDF
│   ├── authentication/           # ⚠️ placeholder — empty
│   └── sync/                     # ⚠️ placeholder — empty
├── demoapp/                      # ✅ The installable Android app used for demos/testing
├── docs/                         # architecture / database / requirements / testing (empty)
├── gradle/
│   ├── libs.versions.toml        # Version catalog
│   ├── gradle-daemon-jvm.properties
│   └── wrapper/                  # Gradle 8.13 wrapper
├── build.gradle.kts              # Root build — declares plugin versions, applies none
├── settings.gradle.kts           # Module list + repository configuration
└── gradlew / gradlew.bat
```

**Only `:demoapp` is an Android application.** Everything under `android/` is a library
module. `android/app`, `backend/authentication` and `backend/sync` are empty
`.gitkeep` placeholders declared in `settings.gradle.kts` — they configure but build
nothing.

> **Note:** `android/` also contains a leftover standalone Gradle setup
> (`android/gradlew`, `android/gradle/wrapper/`, `android/settings.gradle.kts.backup`,
> `android/gradle.properties`) from before the modules were merged into this root
> build. **Ignore them** — always build from the repository root.

---

## Module reference

| Gradle path | Type | Namespace | Depends on | Status |
|---|---|---|---|---|
| `:demoapp` | app | `com.beeftech.demoapp` | `farm-traceability`, `calf-registration`, `farmer-registration`, `database`, `feed-crib`, `authentication` | Runnable (gated behind PIN login) |
| `:android:database` | library | `com.beeftech.database` | — | Core; Room + SQLCipher |
| `:android:calf-registration` | library | `com.beeftech.calfregistration` | `database` | Wired into demoapp; authenticated backend sync |
| `:android:farm-traceability` | library | `com.beeftech.farmtraceability` | `database` | Wired into demoapp; authenticated backend sync |
| `:android:farmer-registration` | library | `com.beeftech.farmerregistration` | `database` | Authenticated backend sync |
| `:android:feed-crib` | library | `com.beeftech.feedcrib` | — (UI only, in-memory data) | Wired into demoapp |
| `:android:authentication` | library | `com.beeftech.authentication` | `database` | PIN auth, `AuthGate`, `SessionStore` (wired into demoapp) |
| `:backend:api` | JVM app | `com.beeftech.backend.api` | — | Runnable Ktor server |
| `:android:app`, `:backend:authentication`, `:backend:sync` | — | — | — | Empty placeholders |

### `:android:database` — the core module

Everything persistent lives here:

- `BeefTechDatabase` — Room database, **schema version 8**
- `entity/` — Room entities (animals, groups, weights, treatments, mortalities,
  movements, costs, farmers, feed cribs, pens, locations, suppliers, users, roles,
  pending sync records)
- `dao/` — one DAO per aggregate
- `repository/` — `AnimalManagementRepository`, `AnimalTraceabilityRepository`,
  `FarmerRepository`, `FeedingRepository`, `LocationRepository`,
  `PendingSyncRepository`, `SyncRepository`
- `security/` — `AndroidKeyStoreSecurityProvider`, `DatabaseKeyProvider`,
  `SecureDatabaseInitializer`, `SecureDatabasePassphraseStore`, `CredentialHasher`,
  `PinLockoutManager`
- `DatabaseProvider.initialize(context, passphrase)` — the single entry point; returns
  a `DatabaseResult`

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

The demo app shows a tab row over the calf-registration and farm-traceability flows,
initialises the encrypted database on a background thread, and seeds a demo animal
(`TEST-001`) on first launch.

> ⚠️ The demo app initialises SQLCipher with the hard-coded passphrase
> `beeftech-demo-passphrase`. This is **demo-only** and must be replaced with the
> Android KeyStore–backed key before any production use.

### Running the backend

```bash
./gradlew :backend:api:run -Dbeeftech.seed.dev=true
```

- Listens on **`0.0.0.0:8081`**
- Creates a SQLite database at **`./data/beeftech-backend.db`** (relative to the working directory)
- Passing `-Dbeeftech.seed.dev=true` seeds default development users into the backend database if they do not exist:

| Username | 5-Digit PIN | Role ID | Role Description |
|---|---|---|---|
| **`admin`** | `10001` | 1 | Administrator |
| **`fmanager`** | `20002` | 2 | Farm Manager |
| **`jvdm`** | `30003` | 3 | Worker / User |

- Override the database location with:
  ```bash
  ./gradlew :backend:api:run -Dbeeftech.seed.dev=true -Dbeeftech.db.url="jdbc:sqlite:/tmp/beeftech.db"
  ```

Smoke-test it:

```bash
curl http://localhost:8081/
# BeefTech Backend API is running

curl -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"jvdm","pin":"30003","device_id":"TEST_DEV"}'
```

### Connecting the app to the backend

All feature API clients (`CalfRegistrationApiClient`, `TreatmentApiClient`, `AnimalMovementApiClient`, `FarmerApiClient`) default to `http://10.0.2.2:8081/` — the loopback alias an **Android emulator** uses to reach the host machine. On a physical device, pass your workstation's LAN address to the client's `baseUrl` constructor parameter (e.g. `http://192.168.1.20:8081/`).

API clients obtain their JWT token dynamically from the logged-in session (`SessionStore` / `TokenProviderRegistry`), so feature operations communicate securely under the authenticated user's credentials.

### Testing the app step-by-step

Follow these steps to test online PIN login, data capture, backend synchronization, offline caching, and security lockout:

#### Step 1: Start the backend server with dev user seeding
In terminal 1, start the Ktor backend with dev seeding enabled:
```bash
./gradlew :backend:api:run -Dbeeftech.seed.dev=true
```
Verify the server is running:
```bash
curl http://localhost:8081/
# Output: BeefTech Backend API is running
```

#### Step 2: Launch the demo app
Start an Android emulator (or connect a USB-debugging-enabled device) and run terminal 2:
```bash
./gradlew :demoapp:installDebug
adb shell am start -n com.beeftech.demoapp/.MainActivity
```
*(Or click **Run demoapp** in Android Studio).*

#### Step 3: Log in via PIN authentication (`AuthGate`)
1. On launch, the app initializes the encrypted SQLCipher database and displays the **PIN Login Screen**.
2. Enter one of the seeded test credentials:
   - **Username**: `jvdm` | **PIN**: `30003` (Worker)
   - **Username**: `fmanager` | **PIN**: `20002` (Farm Manager)
   - **Username**: `admin` | **PIN**: `10001` (Administrator)
3. Tap **Sign In**. The app posts credentials to `POST /api/auth/login`.
4. Upon successful authentication, the backend returns a JWT token. The token and BCrypt PIN hash are stored securely in `EncryptedSessionStore` / Room, opening the main tab interface.

#### Step 4: Capture data & test backend sync
1. **Calf Registration**: Select the *Calf Registration* tab, fill in animal tag/breed details, and tap **Save**.
   - The record is persisted locally to SQLCipher and automatically posted to `POST /api/calf-registrations/sync` using `Authorization: Bearer <jwt>`.
2. **Farm Traceability**: Select the *Farm Traceability* tab to record animal movements, treatments, and mortalities.
3. **Manual Sync Retry**: Tap **Retry Sync** in the top menu to manually process any queued `PENDING` records.

#### Step 5: Test offline support
1. Enable **Airplane Mode** on the emulator or stop the backend server (`Ctrl+C`).
2. Close and relaunch the app.
3. Enter username `jvdm` and PIN `30003`.
4. The app verifies the PIN locally against the Room-cached BCrypt hash and grants offline access immediately.
5. Save a new calf or treatment record. The record is stored locally with status `PENDING` and queued in `PendingSync`.
6. Re-enable network connectivity / restart the backend server. Background workers (`CalfRegistrationSyncWorker`, `TreatmentSyncWorker`, etc.) auto-sync using the session token registered in `TokenProviderRegistry`.

#### Step 6: Test PIN lockout protection
1. On the PIN login screen, enter 5 consecutive wrong PINs.
2. The app enforces a **5-minute lockout period**, displaying a security notification and blocking further PIN attempts until the timer expires.

### Testing

| Command | What it runs | Needs a device? |
|---|---|---|
| `./gradlew test` | All JVM unit tests | No |
| `./gradlew :backend:api:test` | Ktor route tests (JUnit Platform) | No |
| `./gradlew :android:calf-registration:testDebugUnitTest` | Repository, mapper, ViewModel, API client and tag-utils tests | No |
| `./gradlew connectedAndroidTest` | All instrumented tests | **Yes** |
| `./gradlew :android:database:connectedAndroidTest` | SQLCipher, keystore, DAO and migration tests (16 suites) | **Yes** |
| `./gradlew :android:authentication:connectedAndroidTest` | Biometric prompt manager | **Yes** |

Where the tests live:

```text
src/test/       → JVM unit tests (calf-registration, farmer-registration, demoapp, backend/api)
src/androidTest/→ instrumented tests (database, authentication, farmer-registration, demoapp)
```

Run a single test class:

```bash
./gradlew :android:calf-registration:testDebugUnitTest \
  --tests "com.beeftech.calfregistration.util.TagNamingUtilsTest"
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
└──────────────────────────────────────────────────┬───────────────────────────────┘
                                                   │ HTTPS/JSON (Ktor client)
                                                   ▼
┌────────────────────────── backend:api (Ktor + Netty, :8081) ─────────────────────┐
│  JWT auth → routes → service → Exposed → SQLite (./data/beeftech-backend.db)     │
│  Idempotent upsert by record GUID → 200 OK → device marks record SYNCED          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

1. **Capture** — Compose screens validate at the point of entry.
2. **Encrypted local write** — records are written immediately through Room to a
   SQLCipher-encrypted SQLite file; audit metadata (GPS, capture timestamp, device ID,
   record GUID) is embedded on the row.
3. **Queue** — unsynced rows are tracked in `PendingSync` with status `PENDING`.
4. **Sync** — `CalfRegistrationApiClient` logs in, caches the JWT
   (mutex-guarded, single login under concurrency) and posts batches.
5. **Acknowledge** — the backend upserts by GUID (so retries cannot duplicate) and the
   device flips the record to `SYNCED`.

---

## Database & migrations

- **Room schema version: 8** (`BeefTechDatabase`)
- Migrations `1→2` … `7→8` are defined explicitly in
  `android/database/src/main/java/com/beeftech/database/DatabaseFactory.kt`
- Encryption: SQLCipher for Android 4.17.0, key material via Android KeyStore
  (`AndroidKeyStoreSecurityProvider`)

**When you change an entity you must:**

1. Add or edit the entity in `entity/` and its DAO in `dao/`.
2. Bump `version` in the `@Database` annotation on `BeefTechDatabase`.
3. Add a `Migration(n, n+1)` object in `DatabaseFactory.kt` and register it in
   `.addMigrations(...)`.
4. Add a migration test alongside `DatabaseMigration6To7Test.kt`.
5. Run `./gradlew :android:database:connectedAndroidTest`.

Never rely on destructive migration — field devices hold up to 30 days of unsynced data.

---

## Backend API reference

Base URL: `http://<host>:8081`. All routes except `/`, `/api/auth/login` and
`/api/auth/register` require an `Authorization: Bearer <jwt>` header.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/` | Liveness text response |
| `POST` | `/api/auth/login` | Exchange credentials for a JWT (24 h expiry) |
| `POST` | `/api/auth/register` | Register a user |
| `GET` | `/api/profile` | Current user profile |
| `POST` | `/api/calf-registrations/sync` | Batch upsert of calf registrations by GUID |
| `GET` | `/api/calf-registrations` | List calf registrations |
| `GET` | `/api/calf-registrations/{animalId}` | Single calf registration |
| `POST` | `/api/calf-registrations/{animalId}/media` | Attach a photo/video |
| `GET` | `/api/calf-registrations/{animalId}/certificate` | Generated birth-certificate PDF (PDFBox) |
| `POST` | `/api/feed-crib` | Submit a feed crib reading |
| `GET` | `/api/feed-crib` | List feed crib readings |
| `GET` | `/api/feed-crib/{penName}` | Readings for one pen |
| `GET` | `/api/farm-traceability` | Placeholder liveness route |

---

## Toolchain & versions

| Component | Version |
|---|---|
| Gradle (wrapper) | 8.13 |
| Gradle daemon JVM | Java 17 (pinned) |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.0.21 |
| KSP | 2.0.21-1.0.28 |
| Compose BOM | 2024.02.00 (most modules) / 2026.02.01 (version catalog) |
| Room | 2.8.4 |
| SQLCipher for Android | 4.17.0 |
| Ktor (client & server) | 3.0.3 |
| Exposed | 0.56.0 |
| `compileSdk` | 35 (`database`, `farm-traceability`, `farmer-registration`, `demoapp`) · 34 (`authentication`, `calf-registration`, `feed-crib`) |
| `minSdk` | 24 (23 for `:android:database`) |
| JVM target | 17 everywhere |

Dependency declarations are currently **mixed**: `:android:farmer-registration` uses the
`gradle/libs.versions.toml` version catalog, while most other modules hard-code
coordinates. New code should prefer the version catalog.

---

## Contributing

- Branch from `main`; branch names follow `feature/<short-description>`.
- Commit messages follow Conventional Commits, e.g.
  `feat(calf-registration): add photo attachment support`.
- Before opening a PR:
  ```bash
  ./gradlew build          # compiles every module + runs unit tests
  ./gradlew lintDebug      # Android Lint
  ```
- Instrumented tests (`connectedAndroidTest`) need an emulator or device; run them
  for any change under `:android:database`.
- Keep new persistence code behind a repository in `:android:database` rather than
  calling DAOs from feature modules directly.

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

**App cannot reach the backend from a device**
`10.0.2.2` only works on the emulator. Pass a LAN address as `baseUrl` to
`CalfRegistrationApiClient`, and make sure both machines are on the same network.

**Gradle behaves strangely after a branch switch**
```bash
./gradlew --stop && ./gradlew clean
```

---

## Known gaps

- `android/app`, `backend/authentication` and `backend/sync` are empty placeholders.
- `docs/` subdirectories contain only `.gitkeep` files.
- No CI workflow (`.github/`) is configured yet.
- Demo credentials, the JWT secret and the demo SQLCipher passphrase are hard-coded for development.
- Stale standalone Gradle files remain under `android/`.
