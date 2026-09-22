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

# 2. Start the backend (terminal 1)
./gradlew :backend:api:run                            # serves on http://0.0.0.0:8081

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
| `:demoapp` | app | `com.beeftech.demoapp` | `farm-traceability`, `calf-registration`, `database` | Runnable |
| `:android:database` | library | `com.beeftech.database` | — | Core; Room + SQLCipher |
| `:android:calf-registration` | library | `com.beeftech.calfregistration` | `database` | Wired into demoapp; has backend sync |
| `:android:farm-traceability` | library | `com.beeftech.farmtraceability` | `database` | Wired into demoapp |
| `:android:farmer-registration` | library | `com.beeftech.farmerregistration` | `database` | Not yet wired into any app |
| `:android:feed-crib` | library | `com.beeftech.feedcrib` | — (UI only, in-memory data) | Not yet wired into any app |
| `:android:authentication` | library | `com.beeftech.authentication` | — | Not yet wired into any app |
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
