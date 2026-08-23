# 🐂 BeefTech Mobile Data Collection Platform

An offline-first Android mobile solution engineered for **BeefTech (Pty) Ltd.** to modernize feedlot management, livestock tracking, and farm data collection across geographically dispersed field operations in South Africa and internationally.

---

## 📋 Executive Overview

The **BeefTech Mobile Data Collection Platform** replaces paper-based field records with a suite of native Android applications tailored for agricultural environments. Designed to operate in areas with limited or zero cellular connectivity, the system enables field workers to capture animal registrations, treatments, feed bunk readings, and livestock movements directly at the point of activity.

Every record saved on a mobile device automatically embeds immutable, non-repudiable audit metadata (GPS coordinates, UTC timestamps, device hardware IDs, and unique 128-bit GUIDs) to establish end-to-end chain-of-custody tracking. When network connectivity becomes available, pending records sync securely to the central local backend without interrupting field operations.

---

## 📱 Core System Modules

* **Feed Crib Management:** Enables daily bunk readings, feed type recording, quantity tracking, and feed allocation reporting.
* **Calf Registration & Health:** Manages local calf registrations, treatment logs, health interventions, and mortality tracking directly linked to verified livestock records.
* **Farm Traceability & Security:** Provides hardware-backed encryption for local storage (SQLCipher + Android KeyStore), verified farmer-to-farm profile linking, and tamper-evident audit metadata generation.
* **System & User Administration:** Supports role-based access control (RBAC), user authentication, system settings configuration, and administrative oversight.

---

## 💻 Enterprise Technology Stack

* **Operating System / Target Platform:** Android (Native)
* **User Interface Framework:** Jetpack Compose
* **Programming Language:** Kotlin
* **Local Persistence Engine:** Room ORM with SQLite
* **Local Data Encryption at Rest:** SQLCipher for Android
* **Hardware Cryptography:** Android KeyStore System (backed by TEE / StrongBox hardware)
* **Background Data Synchronization:** Android WorkManager
* **Backend Architecture:** REST APIs over HTTPS with local Linux backend server synchronization

---

## ⚙️ System Architecture & Synchronization Workflow

```text
[ Field Device Capture ] 
          │
          ▼
[ Room ORM + SQLCipher ] ────► Encrypted Local Data Storage (PENDING Status)
          │
          ▼
[ WorkManager Service ] ────► Automatic Background Connectivity Monitor (15-min / Event)
          │
          ▼
[ HTTPS POST Batched ]  ────► Local Linux Backend Server API & Central Database Upsert
          │
          ▼
[ HTTPS Acknowledgment ] ────► Update Local Status to SYNCED (No Duplicate Records)

1. **Field Capture:** Data is captured via digital forms with point-of-entry validation.
2. **Encrypted Local Storage:** Records write immediately to local SQLite databases encrypted via SQLCipher using hardware-secured keys.
3. **Background Sync:** The system periodically checks for network availability and batches `PENDING` records for transmission.
4. **Backend Acknowledgment:** Central API servers perform idempotent updates by GUID and send an acknowledgment to update local records to `SYNCED`.

---

## 📈 Business Impact & Value

* **Zero Connectivity Dependency:** Provides up to 30 days of offline field operation capabilities without data loss or feature degradation.
* **Elimination of Data Entry Errors:** Direct point-of-capture validation prevents human transcription mistakes, protecting operational revenues.
* **Full Regulatory Traceability:** Meets South African biosecurity standards (RMIS / LITS alignment) and international export chain-of-custody compliance.
* **Hardware-Backed Data Security:** Protects sensitive operational data on field devices through hardware key derivation and database encryption at rest.
