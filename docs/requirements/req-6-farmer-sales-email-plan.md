# Req 6: Farmer Registration & Sales Email, closing the gaps

## Context
Req 6 (critical) asks for three things: capture farmer details on site, show a status pill for each registration (Pending Sync / Processing / Registered), and send the assigned sales rep a JSON email when the farmer syncs. Today's gaps:
- The form has no contact name, phone, farm size, head count or breed.
- No screen shows a farmer's sync status.
- Every email goes to the single `BEEFTECH_SALES_REP_EMAIL` inbox.
- The attachment has no `event` or rep address.
- An email is sent again on every re-sync.

Decisions made:
- Each site gets its own sales rep.
- The current camelCase payload stays, with fields added to it rather than reshaped.
- The form captures all 5 new fields.
- A new farmer list screen shows the pills.

Work goes on a new branch `feature/farmer-sales-email` created from `main`, not from `feature/project-code-digits`.

## 1. New farmer fields (contact name, contact number, farm size ha, head count, primary breed)

**Room (`:android:database`), following the database rules in CLAUDE.md:**
- `entity/FarmerEntity.kt`: add the nullable fields `contact_name`, `contact_number`, `farm_size_ha: Double?`, `head_count: Int?` and `primary_breed`. All default to null.
- `BeefTechDatabase.VERSION` 44 → 45. Add `MIGRATION_44_45`, which runs 5 × `ALTER TABLE farmers ADD COLUMN …`. This only adds columns, so no data is touched. If `main` has moved on, use whatever the next version is.
- Register the migration in `DatabaseFactory.kt` `.addMigrations(...)` inside `guarded(...)`.
- Commit `schemas/.../45.json`.
- Add `androidTest/.../Migration44To45Test.kt`, modelled on `Migration43To44Test.kt`. It inserts a farmer at version 44, migrates, and asserts the existing values survive and the new columns are NULL.

**Form (`:android:farmer-registration`):**
- `ClientDetailsScreen.kt`: `ClientRegistrationData` gets `contactName` and `contactNumber`. Both are optional. The phone check is light: `+`, digits and spaces, with 9–15 digits.
- `AddressAndLocationScreen.kt`: `AddressAndLocationData` gets `farmSizeHa`, `headCount` and `primaryBreed`. These sit beside `herdCapacity`, reuse its numeric input and `MAX_HERD_CAPACITY` style limits, and breed is free text.
- `CoordinatesAndSaveScreen.kt` (around line 430, where `FarmerEntity(...)` is built): map the new fields with the same `.trim().ifBlank { null }` / `toIntOrNull()` idiom. Show them on the review summary.
- `data/FarmerApiClient.kt`: add the fields to the DTO and to the entity → DTO mapping (around line 172).

**Backend:**
- `FarmerTable.kt`, `FarmerModels.kt` (`FarmerDto`), and `FarmerRepository.save` (both the insert and update branches).
- `FarmerSchemaMigration.kt`: add the 5 columns to `NEW_COLUMNS["farmers"]`. Existing deployed databases get them through the existing idempotent ALTER path.

## 2. Sales rep per site
- `auth/SitesTable.kt`: add `salesRepEmail = varchar("sales_rep_email", 255).nullable()`, plus an ALTER in `auth/SitesSchemaMigration.kt` using the same pattern as `farm_code`.
- `auth/SiteModels.kt`: add `salesRepEmail` to `SiteDto`, `CreateSiteRequest` and `UpdateSiteRequest` (null means unchanged).
- `auth/SiteService.kt`: validate the email address, save it, and add it to the audit details.
- `auth/SiteRepository.kt`: read and write it. Add `salesRepEmailOfSite(siteId)`, next to `farmCodeOfSite` in `SyncUploadLog.kt`.
- Management app: `android/management/.../ui/SitesScreen.kt` gets a "Sales rep email" field in the create and edit dialogs and shows it on each site row. `data/ManagementModels.kt` gets the matching DTO fields.
- `FarmerSalesNotification.kt`:
  - `notifyRegistration` sends to `payload.assignedSalesmanEmail ?: config.recipientAddress`, so the env var becomes the fallback.
  - `fromEnvironment` must no longer return null just because `BEEFTECH_SALES_REP_EMAIL` is blank. SMTP stays enabled when only per-site emails are set.
  - With no recipient at all, it logs and skips.

## 3. Email payload (keep the current format, add fields)
- `FarmerSalesNotificationPayload` gets these new fields:
  - `event: String = "NEW_FARMER_REGISTRATION"`
  - `assignedSalesmanEmail: String?`
  - `contactName`, `contactNumber`, `farmSizeHa`, `headCount`, `primaryBreed`
- `FarmerService.syncRecords` fills them in. The rep is `principal.siteId?.let { salesRepEmailOfSite(it) }`.
- **Stop duplicate emails:**
  - Add a `sales_notified_at` column to the backend farmers table (in `FarmerTable` and `FarmerSchemaMigration`).
  - `FarmerService` emails only when that column is null, and sets it after a successful send.
  - A re-sync or retry then sends nothing, and a send that failed is retried on the next sync.
- `FarmerSalesNotificationTest.kt`: assert `event`, the rep address and the new fields appear in the JSON.
- New tests check that the rep address falls back to the env var and that no second email goes out on a re-sync. Use a fake `FarmerSalesNotificationService` that records its calls.

## 4. Farmer list with status pills
- `dao/FarmerDao.kt`: add `observeAllFarmers(): Flow<List<FarmerEntity>>`, ordered newest first. If farmers don't have a created timestamp, order by `rowid DESC`.
- `repository/FarmerRepository.kt`: expose that Flow. Feature code stays behind the repository.
- New `android/farmer-registration/.../FarmerListScreen.kt`:
  - It's an Activity, like the other farmer screens.
  - Each row shows the organisation name, contact name, client code and a pill.
  - Pill mapping: `PENDING` → "Pending Sync" (amber/warning), `PROCESSING` → "Processing" (blue), `SYNCED` → "Registered" (green). Anything else shows as Pending Sync.
  - Take the pill style from `SyncStatusChip` in `demoapp/.../AppChrome.kt:308` (a CircleShape `Surface`, bold `labelMedium`). It can't be imported because it lives in `:demoapp`, so the chip is copied locally.
  - The list updates live as `FarmerSyncWorker` changes statuses.
  - A "Register farmer" button opens `ClientDetailsScreen`.
- `demoapp/.../MainActivity.kt` (around line 855): `onFarmerRegistrationClick` opens `FarmerListScreen` instead of `ClientDetailsScreen`. After saving, the flow returns to the list so the new Pending Sync pill shows.
- Register the Activity in the module manifest.

## Verification
- `./gradlew :android:database:connectedAndroidTest` (emulator), which covers `Migration44To45Test` and all earlier migrations.
- `./gradlew testDebugUnitTest test --continue`. `:backend:api:test` is flaky (N5), so re-run if an unrelated single test fails.
- Manual check:
  1. Run `./gradlew :backend:api:run -Dbeeftech.seed.dev=true` with SMTP env vars pointing at a local catcher (for example MailHog on port 1025 with `BEEFTECH_SMTP_SECURITY=none`).
  2. As admin, set a sales rep email on site S001.
  3. Install the demo app with `:demoapp:installDebug`, pointed at `10.0.2.2:8081`.
  4. Register a farmer while offline and check the list shows **Pending Sync**.
  5. Go online and press Sync now. The pill should go to **Processing** and then **Registered**.
  6. Check exactly one email reached the site rep, with the attachment `S001-FARMER_REG-…json` containing `event`, `assignedSalesmanEmail` and the 5 new fields.
  7. Sync again and check no second email arrives.

## Suggested commits
1. `feat(database): farmer contact, farm size, head count and breed fields` (entity, migration, schema, test)
2. `feat(farmer-registration): capture the new farmer fields`
3. `feat(backend): sales rep email per site`, with `feat(management): edit a site's sales rep email`
4. `feat(backend): sales email event, rep address and one email per farmer`
5. `feat(farmer-registration): farmer list with sync status pills`
