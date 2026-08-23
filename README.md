# BeefTech Mobile Application — Local Database & Security Core (`core:database`)

This repository module contains the hardware-backed encrypted local database foundation for the offline-first **BeefTech Mobile Data Collection Platform**. Built with **Kotlin**, **Room ORM**, **SQLCipher**, and the **Android KeyStore (TEE/StrongBox)**, this core module handles offline data persistence, hardware security, and automated traceability metadata injection.

---

## 🛠 Tech Stack
* **Language:** Kotlin
* **Database / ORM:** Room with SQLCipher for Android
* **Hardware Security:** Android KeyStore System (TEE / StrongBox)
* **Architecture:** Offline-First Modular Android

---

## 🏗 Architecture & Git Workflow

All code for this team resides in the `core:database` package. 

### **Branching Strategy**
* Do **not** commit directly to `main` or `develop`.
* Create feature branches off `develop` using the format: `feature/sec-<task-name>`
  * `feature/sec-keystore-manager`
  * `feature/sec-sqlcipher-room`
  * `feature/sec-metadata-interceptor`
* Submit a Pull Request (PR) to `develop` requiring at least 1 peer code review before merging.

### **Parallel Development Strategy**
To allow Group 2 to build independently without blocking on other auth/UI teams, all hardware and key logic uses **mock input keys** and **mock location providers** during initial development. Clean Kotlin interface contracts are exposed so external teams can connect their triggers when ready.

---

## 📋 Task Breakdown & Technical Requirements

### **Task 1: Android KeyStore & Cryptographic Key Management**
* **Developer:** Developer 1
* **Objective:** Implement the key generation and management lifecycle using Android's hardware-backed security modules.
* **Technical Requirements:**
  * Use the `KeyGenerator` API to create an AES-256 master encryption key inside the **Android KeyStore** (backed by TEE or StrongBox hardware).
  * Ensure the KeyStore key never leaves the hardware boundary.
  * Implement logic where the user's local credential/passcode unlocks this KeyStore key, which in turn encrypts/decrypts the local database passphrase in memory.
  * Provide clean helper functions (`getDatabasePassphrase()`, `clearKeyFromMemory()`) to safely handle key material in RAM.
  * *Parallel Strategy:* Use a mock hardcoded passcode string during local testing to simulate Group 1's login output.

---

### **Task 2: SQLCipher Integration & Room Database Configuration**
* **Developer:** Developer 2
* **Objective:** Secure the local offline SQLite database using SQLCipher to prevent unauthorized physical reading.
* **Technical Requirements:**
  * Integrate `SQLCipher for Android` (`SupportOpenHelperFactory`) with Android's **Room ORM**.
  * Wire the database setup to receive the decrypted passphrase from Task 1 dynamically at startup.
  * Ensure all database reads and writes (pending sync queues, animal movements, treatments, and mortalities) pass transparently through SQLCipher.
  * Implement automated error handling for database access errors (e.g., incorrect passphrase attempt or database corruption).
  * *Parallel Strategy:* Test SQLCipher using a temporary developer passphrase until Task 1's KeyStore provider is connected.

---

### **Task 3: Automated Traceability Metadata Injection**
* **Developer:** Developer 3
* **Objective:** Ensure every database record is automatically stamped with non-repudiable audit metadata prior to saving.
* **Technical Requirements:**
  * Build a Room Interceptor or Base DAO helper to automatically inject immutable metadata into every record saved to SQLite:
    * **UUID / GUID v4:** 128-bit unique identifier for synchronization.
    * **GPS Coordinates:** Latitude and longitude captured at point of record creation.
    * **UTC Timestamp:** Standardized network/system time.
    * **Device ID:** Hardware/App-specific identifier.
  * Mark initial record status as `PENDING` for the background `WorkManager` service to read and sync.
  * *Parallel Strategy:* Implement a mock GPS provider location (e.g., `-25.7461, 28.1881`) to test auto-injection before live location services are hooked up.

---

### **Task 4: Integration Testing & Security Validation**
* **Developer:** Team Lead / QA Developer
* **Objective:** Verify data-at-rest security and test field edge cases.
* **Technical Requirements:**
  * **Database Inspection:** Extract the `.db` file from a test device and verify it cannot be opened using standard SQLite browsers without the SQLCipher key.
  * **Key Lifecycle Tests:** Test app restarts, process kills, and device reboots to confirm the database cannot be decrypted without local credential verification.
  * **Data Integrity Checks:** Perform stress testing on offline capture to ensure up to 30 days of data can be written securely without key leaks or RAM memory retention vulnerabilities.

---

## 🧪 Testing & Verification Checklist
- [ ] `./gradlew test` passes without key leak warnings.
- [ ] Raw database export verified encrypted via SQLite Browser.
- [ ] Database automatically locks and clears memory references upon app termination.
- [ ] Every saved record contains valid non-null GUID, GPS, UTC timestamp, and `PENDING` sync status.
