# Close four client-feedback gaps (PDF "Beeftech Mobile Data Collection Platform 12 sep")

## Context
The PDF review found gaps in four requirements: Req 6, Req 5, Req 2 and Req 1. Worker access to farmer registration stays as it is, because Req 6 says field workers capture farmers. Decisions already made:
- The [Project] code is fixed per module.
- Condition is a 1–5 score stored as text "1"–"5".
- Interest status is a fixed, hard-coded list.

Each part below is independent and gets its own commit, in the order listed. Mobile Room is at v42, so the new migrations are **42→43 (farmer)** and **43→44 (sync log)**.

Shared patterns:
- **Room migrations:** idempotent `PRAGMA table_info` check, then `ALTER`/`CREATE TABLE IF NOT EXISTS`. Templates are `MIGRATION_31_32` and `MIGRATION_39_40` in `BeefTechDatabase.kt`. Register each with `guarded(...)` in `DatabaseFactory.kt:537-577`, export the schema JSON, and add a `MigrationXToYTest` modelled on `Migration41To42Test.kt`.
- **Backend schema:** hand-written `*SchemaMigration` objects run before `SchemaUtils.create` in `Database.kt:41-75`. Use per-column types like `CalfRegistrationDetailsSchemaMigration.kt`. New tables only need adding to the create list.

---

## 1. Req 6: farmer herd capacity and interest status
**Mobile**
- `AddressAndLocationScreen.kt`:
  - Add `herdCapacity: String` and `interestStatus: String` to `AddressAndLocationData`.
  - In the "Land" card after Land Ownership, add a number-only field copied from `FarmerIdentifierTextField` (:857) with `KeyboardType.Number`, digits only, 0–100000.
  - Add a `FarmerDropdownField` (:955) using a new `AddressAndLocationLookups.interestStatuses = listOf("Interested", "Follow-up needed", "Not interested", "Already a client")`.
  - Both fields are optional so existing flows keep working.
- `FarmerEntity.kt`: add `herd_capacity: Int?` and `interest_status: String?`.
  - `MIGRATION_42_43` adds `herd_capacity INTEGER` and `interest_status TEXT` to `farmers`.
- `CoordinatesAndSaveScreen.kt:428-476`: map both fields into the entity.
- `FarmerApiClient.kt`: add `herdCapacity: Int?` and `interestStatus: String?` to `FarmerPayload` (:48-64) and to `toPayload` (:149-167).
- Display:
  - `FarmerFarmProfileScreen.kt`: two `TraceabilityInfoRow`s next to Land Ownership (~:136), wired in `FarmTraceabilityFlow.kt:909-950`.
  - `RegisteredFarmersScreen.kt`: show interest status as a secondary line.

**Backend**
- `FarmerTable.kt`: add `integer("herd_capacity").nullable()` and `varchar("interest_status", 64).nullable()`.
- `FarmerSchemaMigration.kt`: support per-column types and add `herd_capacity INTEGER NULL` and `interest_status VARCHAR(64) NULL`.
- `FarmerModels.kt` `FarmerDto`: add the two fields. In `FarmerRepository.kt`, update the read (:108), update (:221) and insert (:271) paths.
- `FarmerService.kt:39-96`: add both fields to `FarmerSalesNotificationPayload` (`FarmerSalesNotification.kt:20-41`). The JSON attachment picks them up automatically. Add "Herd capacity" and "Interest status" lines to the text body near :422.

**Tests:** `FarmerApiClientTest`, `Migration42To43Test`, `FarmerRoutesTest`, `FarmerSchemaMigrationTest`, `FarmerSalesNotificationTest`.

## 2. Req 2: 1–5 condition scale and select-only treatment type
**Condition** (`android/calf-registration`)
- `CalfRegistrationModel.kt:52-57`: replace `conditions` with scores 1–5 and labels:
  - 1 Poor, 2 Fair, 3 Good, 4 Very good, 5 Excellent.
  - The default `condition = "3"`.
- Add `CalfConditionScale` (new file in `ui/`), a row of five large equal-width tappable boxes, each showing the number, label and a filled/empty pip bar. It copies the gender toggle pattern in `CalfDetailsStepScreen.kt:162-194`, with 56dp+ height for gloved hands.
  - It replaces the dropdown at `CalfConditionStepScreen.kt:223-227`; remove the "CONDITION" branch of the dialog.
- `CalfRegistrationMappers.kt:140` (reading): convert old text with a `legacyConditionToScore` map (Poor→1, Fair→2, Good→3, Excellent→5). Unknown values become "3".
  - Saving (:121) writes the digit.
  - `CalfReviewScreen.kt:146` and `CalfDetailScreen.kt:65` show "3 – Good".
- Backend `body_condition` stays as `varchar(64)`. No schema change.
- Tests: update `CalfRegistrationMappersTest.kt:223-283` and add cases for converting the old values.

**Treatment type** (`android/farm-traceability`)
- `TreatmentScreen.kt:178-193`: switch to the read-only `TraceabilityDropdown` (`TraceabilityComponents.kt:413`). This avoids the text-box mismatch that `allowCustomEntry=false` would cause. Disease is left unchanged.
- Validation at :262-270: the value must be in `treatmentOptions`, ignoring case. If the options are empty (never synced), fall back to the seed list from `ReferenceDataSeeder.kt:36-49`, which already includes "Other". Saving is never blocked offline.
- Update the placeholder and helper text, and the `traceabilityDefaultFieldHelp` entry (`TraceabilityComponents.kt:196`).
- Backend `TreatmentService`: reject a `treatmentName` that is not an active `TreatmentTypeTable` name, ignoring case. Return a per-record error, not a 500.
  - Records saved earlier with free text: accept them as-is when their `recordGuid` already exists, so old devices still sync.
- Tests: treatment ViewModel/screen validation test and backend `TreatmentRoutesTest`.

## 3. Req 5: file naming `[FarmCode]-[Project]-[YYYYMMDD]-[HHMMSS]-[DeviceID]`
**Farm code**
- Backend `SitesTable`: add `farm_code varchar(4)` with a unique index. `SitesSchemaMigration` adds the column and fills existing rows with `S` + a 3-digit sequence (dev-site-1 becomes `S001`).
- `SiteService.validate`: `^[A-Z0-9]{4}$`, required on create and editable on update. Add it to `SiteDto` and the create/update requests.
- Include `farm_code` in the login/profile `UserProfileDto` and the JWT (`JwtService.kt:54-63`).
- Mobile:
  - Store it in `SessionStore` next to `site_id` (:162).
  - Expose it as `LoggedInUser.farmCode`.
  - Add a field to the management `SitesScreen` create/edit form.

**Shared generator.** Add `android/database/.../util/FileNamingUtils.kt`, modelled on `TagNamingUtils`:
- `enum class ProjectCode { CALF_REG, FARMER_REG, TREATMENT, MOVEMENT, MORTALITY, COST, TRACE_EVENT, REPORT }`.
- `build(farmCode, project, instant, deviceId, ext?)`: uses the device's local time and `yyyyMMdd-HHmmss`, cleans `deviceId` to `[A-Z0-9_]`, and calls `require` on the farm code.
- `REGEX = ^[A-Z0-9]{4}-[A-Z_]+-\d{8}-\d{6}-[A-Za-z0-9_]+(\.[a-z]+)?$`, plus `validate()` and `parse()`.
- Add a JVM test in `android/database/src/test`.
- Mirror it in the backend as `common/FileNaming.kt`, with the same regex and a test.

**Where the generator is used**
- **Sync uploads.** Every `POST /api/*/sync` request gains an optional `batchName` field:
  - Calf, farmer, treatment, movement, mortality, cost and traceability-events request DTOs, plus the matching mobile API clients.
  - Each worker builds the name once per run from the session farm code and the device ID (`DeviceIdProvider`, `MOB_DEV_<hex>`).
- **Backend upload log.** Add a new `sync_upload_log` table:
  - Columns: `batch_name` (unique index), `project`, `device_id`, `user_id`, `site_id`, `record_count`, `accepted`, `rejected`, `received_at`.
  - A shared `SyncUploadLogger` in each sync route checks `batchName` against the regex (400 if it is malformed). It checks that the farm-code part equals the caller's site code and that the device part equals `principal.deviceId` (both 400 if not), then inserts the row.
  - A repeated `batchName` (retry) is accepted and the row updated, so the endpoints stay idempotent.
  - A missing `batchName` is still accepted for now and logged as legacy, so devices running older app versions are not broken.
- **Calf photos.** `CalfPhotoCapture.kt:32` uses `FileNamingUtils.build(..., CALF_REG, ..., "jpg")`. The folder stays `calf-photos`, so `deleteSavedPhoto` keeps working. The server's `CalfPhotoStore` keeps storing photos by `recordGuid`.
- **Farmer email attachment.** `FarmerSalesNotification.kt:473-478`: `<farmCode>-FARMER_REG-<ts>-<deviceId>.json`, using the submitter's site code and record device.
- **Reports:** `ReportRoutes.kt:75` and the client name at `ManagementApiClient.kt:85` use `REPORT` with device ID `SERVER` on the server.

**Tests:** `FileNamingUtilsTest` on mobile and backend, `SitesSchemaMigrationTest`, a sync upload log route test (valid, malformed, wrong site, wrong device, retry, legacy), and updated API client tests.

## 4. Req 1: sync run log and app-wide widget
**Log table.** Add `sync_runs` (Room `MIGRATION_43_44`, entity `SyncRunEntity`, DAO `SyncRunDao`).
- Columns: `id`, `user_id`, `module`, `trigger` (MORNING/EVENING/MANUAL/AUTO), `started_at`, `finished_at`, `synced_count`, `failed_count`, `result` (SUCCESS/PARTIAL/FAILED/OFFLINE), `message`, `batch_name`.
- DAO: `insert`, `observeRecent(userId, limit=50)`, and `pruneOlderThan(30 days)`.
- `SyncRepository.recordRun(...)`.

**Writing it.**
- Each sync worker writes one row after its `syncPending()`, using the existing outcome types: `syncedCount` and `errorMessagesBy*.size` from the mortality, cost, treatment, movement and calf repositories.
  - `FarmerSyncWorker` gets real counters in place of its two flags.
- The trigger is passed as WorkManager input data by `ScheduledBatchSyncWorker` (MORNING/EVENING from its unique name) or by the manual action. It defaults to AUTO.

**Fix gaps found along the way**
- Add `TraceabilityOutboxWorker` to `ScheduledBatchSyncWorker` dispatch (:47-93).
- Move the dispatch into a `SyncAllDispatcher.dispatch(context, trigger, policy)` in demoapp. The scheduled worker uses `KEEP`, manual uses `REPLACE`.

**Widget**
- `PendingSyncDao`: add `observePendingCountsByType(userId)` (`SELECT entityType, COUNT(*) … GROUP BY entityType`).
- In the Home "Sync" section (`AppChrome.kt:597-639`):
  - Show a breakdown by module (Calves, Farmers, Treatments, Movements, Mortality, Costs, Traceability), counting only non-zero rows.
  - Show the time of the last run and its result.
  - Add a **Sync now** button, disabled when offline, that calls `SyncAllDispatcher.dispatch(MANUAL)`.
- Wire the counts in `MainActivity.kt:511-530`, next to the current pending and failed flows.
- `MyActivityScreen.kt`: add a "Sync history" list from `observeRecent`, showing module, trigger, time, counts and result.
- The Traceability widget keeps its Retry button, but it now also calls the dispatcher. The per-type handling in `MainActivity.kt:1119-1232` stays for its snackbars, and the missing TREATMENT case is added.

**Tests:** `Migration43To44Test`, `SyncRunDao` instrumented test, a test of the dispatcher's enqueued work (WorkManager test helper), and an `appSyncUiState` unit test with the breakdown.

---

## Verification
- `./gradlew test` for the JVM tests across the Android modules and backend (`:backend:api:test`).
- `./gradlew :android:database:connectedAndroidTest` for the migration tests (needs an emulator).
- Manual check on the emulator against a local backend (`README` dev users admin, fmanager, jvdm):
  1. Register a farmer with herd capacity and interest status. Check it appears in the profile and that the email or log payload contains both.
  2. Register a calf using the 1–5 scale. An old calf with "Excellent" shows "5 – Excellent".
  3. Free text can't be entered for treatment type. Check that offline still works through the seed fallback.
  4. Set farm code BF01 on the site and sync. `sync_upload_log` has `BF01-CALF_REG-YYYYMMDD-HHMMSS-MOB_DEV_xxxxxxxx`, and the photo file uses the same pattern.
  5. Home shows pending counts by module. Sync now writes rows that appear in My activity → Sync history.
