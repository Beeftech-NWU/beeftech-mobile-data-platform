# Future checks: deferred follow-ups

Status: **deferred** (recorded 2026-09-28). None of these items is scheduled. Each will
be checked when it can be prioritised. They were collected while planning R6
(ownership, place and movements) and are deliberately **out of scope** for R6. R6 is
complete without them.

When you pick one up, turn it into a work package in `Instructions.md` with acceptance
criteria. Then mark it here with the PR that resolves it. Don't delete entries.

## Already tracked elsewhere

1. **Flaky backend route tests (N5).** `:backend:api:test` fails intermittently, with a
   different single test each run (`TreatmentRoutesTest`, `CalfRegistrationRoutesTest`).
   The likely cause is state shared between tests. Until it's fixed, any PR's unit-test
   job can go red at random.
   **Resolved** on branch `feature/backend-flaky-tests` (2026-10-02). Cause: repositories opened
   Exposed transactions without naming a database. Exposed caches the default database per
   thread, so a reused `Dispatchers.IO` thread could still point at an earlier test's temp
   database. Every transaction now passes `DatabaseFactory.getDatabase()` explicitly.
2. **Make both CI jobs required checks (N4 follow-up).** The jobs are "Unit tests" and
   "Database migration tests (emulator)". R4 (PR #54) merged with the emulator job red,
   because neither job is required on `main`.
3. **Backend `animals` table and FKs (R5.4).** Add the table, then FKs from the backend's
   animal-reference columns (`calf_registrations.animal_uuid`, `treatments.animal_id`,
   `animal_movements.animal_id`, …). Once R6 lands, the backend half of R5.3 (movement
   source and destination FKs) belongs here too. It needs backend `farms`/`locations`/
   `pens` tables first.
4. **Finish backend alignment (D14).** Only the calf table is aligned today. The rest
   follows R5.4.
5. **Run the referential-integrity audit on real data (R5.2).** Run
   `referential-integrity-audit-2026-09-28.sql` against a copy of a real device
   database, and record the actual counts in the audit note.
6. **What is PERS? (Q2).** An internal term that nobody has explained yet. It is
   informational only.

## Once R6 lands

7. **Quarantine repair pass.** R6 keeps every value it can't map, but doesn't link it.
   That covers owners that matched no farmer, destinations that matched no place, worker
   names that matched no user, feed notes that couldn't be parsed, and legacy supplier,
   location-feed and movement rows that couldn't be resolved. Once real farms, locations
   and users exist on devices, review the R6 `quarantine_*` and `legacy_*_r6` tables and
   repair what can be repaired.
8. **Master-data sync.** Farms, locations, pens, rations and suppliers will exist only
   where a user created them on that device. Syncing them from the backend is a separate
   package.
9. **Merge spelling variants of suppliers and rations.** R6 deduplicates on the
   normalised name only, so near-duplicates stay separate.
10. **Purchase price field.** After R6 the app stores NULL instead of an invented `0.0`,
    but the supplier screen still has no price input.
11. **Ration cost into `animal_costs`** (the `FEED` cost type). This is deliberately left
    out until the cost's currency and basis are defined (per head, per day, or total).
12. **Backend `vatNumber` → `taxNumber`.** Once R6 lands, the app's column is
    `tax_number` (Q5), but the sync JSON key stays `vatNumber` until the backend renames
    it.

## Found during R6 planning

13. **Retire the auto-lookup triggers.** `trg_animals_auto_lookup`,
    `trg_treatments_auto_lookup`, `trg_farmer_addresses_auto_lookup` and the device
    triggers create lookup rows from free text. That conflicts with the rule against
    automatically created lookup values.
14. **Zero epoch dates.** R3.5's date conversion (`MIGRATION_21_22`) stored `0` when it
    couldn't parse a date. Count and repair any `0` dates in `movement_date`,
    `purchase_date` and the other converted columns.
15. **Movement sync device id.** `AnimalMovementRepository` sends `Build.MODEL` to the
    backend instead of the row's `device_id`.
16. **GPS 0,0.** The movement and weight writers never set GPS, so the columns default
    to 0,0: the invented value the rules forbid. Either capture real GPS or make the
    columns nullable.
17. **DAO access sweep.** Check the feature modules for view models that call DAOs
    directly instead of going through a repository in `:android:database`.

## Ongoing

18. **Re-confirm Q6 / rule 6.** Only v10 and earlier builds have reached field devices,
    which is why migrations can still be edited in place. Re-confirm this with the
    maintainer once field deployment starts. From then on, no merged migration may be
    edited.

## Found during role and site foundation (Manager & Admin, Phase 1)

19. **RESOLVED (Phase 2, PR #73): the stub route was removed.** Original note: **`POST /api/auth/register` is a stub.** `AuthService.register` returns `true` and
    creates nothing. It is not a privilege-escalation risk today, but lock it down
    (admin-only with `requireRole`) or remove it in Phase 2, when user management lands.
20. **RESOLVED (Phase 3, sync gaps, PR #75): Feed Crib is stored in `feed_crib_readings`.** Original note: **Feed Crib is not persisted on the backend.** `FeedCribService` keeps readings in
    memory, so they are lost on restart. The submitter and site are stamped on the
    in-memory record and `GET /api/feed-crib` is scoped by them, but the data needs a
    table before the Dashboard can rely on it.
21. **Records from before the deploy have `site_id = NULL`.** `submitted_by_user_id` and
    `site_id` are not backfilled on `calf_registrations`, `animal_movements`,
    `treatments` and `farmers`. A manager's site filter never matches them, and workers
    don't see them either; only admins do. Decide whether to backfill from `device_id` or
    leave them.
22. **Routes left unscoped on purpose.** `GET /api/treatments/reference-data` is shared
    reference data. `POST /api/calf-registrations/{tagNumber}/media` and
    `GET /api/calf-registrations/{tagNumber}/certificate` are reached by tag and do not
    check the caller's scope. Revisit these when the records review lands (Phase 3). **Update (Phase 4c):** these routes now go through the same database token check as every other route (a deactivated user or revoked phone is rejected), but they are still not scoped by site.
23. **RESOLVED (Phase 3, sync gaps, PR #75): `GET /api/animal-movements` and `/{animalId}` are scoped.** Original note: **Animal movements have no `GET` list.** Movements are stamped with the submitter
    and site on sync, but nothing reads them back yet, so there is nothing to scope.
24. **RESOLVED (Phase 4c, `feature/admin-revocation-devices`): tokens are checked against the database on every request.** Original note: **Tokens issued before the deploy have no `site_id` claim.** They stay valid for up
    to 24 h. A manager on such a token sees no site-scoped records until they log in
    again.
25. **RESOLVED (Phase 4c, `feature/admin-revocation-devices`): deactivating a user, unbinding their phone or resetting their PIN ends their tokens at once.** Original note: **Deactivated users keep working tokens on non-admin routes.** `users.active` is checked at
    login and on every `/api/users` call (the caller is re-read from the DB), but the sync and
    record routes only validate the JWT. A deactivated worker can still sync for up to 24 h
    from a token issued before deactivation. Fix with a short token lifetime or an `active`
    check in `requireAuthPrincipal` (Phase 4's revoke list is the natural home).
26. **RESOLVED (Phase 4c, `feature/admin-revocation-devices`): a PIN reset (and `POST /api/users/{id}/unlock-login`) clears the login lockout, which is now stored in the database.** Original note: **A PIN reset does not clear the login lockout.** `AuthService.loginAttempts` is an
    in-memory map, so a worker locked out for 5 minutes stays locked after a manager resets
    their PIN.
27. **RESOLVED (Phase 4b, `feature/admin-sites`): the Add user dialog has a site picker.** Sites come from `GET /api/sites` (active sites only), and the server still rejects unknown ones. Original note: **Admins type a site ID by hand in the Team tab.** There is no sites endpoint until Phase 4, so
    the "Add user" dialog takes a free-text site ID and the backend rejects unknown ones
    ("Unknown site"). Replace it with a site picker when Sites CRUD lands.
28. **RESOLVED (Phase 3, dashboard summary, PR #74): the Dashboard tab now loads `GET /api/dashboard/summary`.** Original note: **Dashboard tab is a placeholder.** It shows static text until Phase 3 adds
    `GET /api/dashboard/summary`.
29. **The dashboard covers only what the backend stores.** It counts calves, treatments, farmers,
    workers and stale syncs. Mortalities, costs, movements and feed are missing because the backend
    has no sync path or table for them (movements have no `GET`, Feed Crib is in memory). Add each
    to the summary once its sync path exists.
30. **Farmer "recent" uses `synced_at`.** Farmers carry no capture time, so "last 7 days" means
    when the record reached the server, not when it was captured.
31. **Dashboard stale-sync alerts use `users.device_last_sync`, which login sets.** It moves on login,
    not on every sync, so a worker who stays logged in and syncs can still look stale. Stamp it from
    the sync routes if the alert proves noisy. **Update (Phase 4c):** any authenticated request now refreshes it (at most every 15 minutes), so it means "last contact", not "last sync" or "last login".
32. **RESOLVED (Phase 4b, `feature/admin-sites`): admins get an "All sites" / per-site switch on the Dashboard.** An unknown `siteId` is now a 400. Original note: **The dashboard has no site switch for admins in the app.** The endpoint takes `siteId`, but the
    app always asks for all sites. Add the switch with Sites CRUD in Phase 4.
33. **Feed Crib `POST` is not idempotent.** `FeedCribRequest` has no record GUID, so a retried request
    inserts a duplicate reading. The app does not post Feed Crib at all yet (`:android:feed-crib` is
    UI only, in memory), so nothing is duplicated today. Add a `recordguid` and an upsert when the
    device gets a Feed Crib sync worker.
34. **RESOLVED (cost sync, PR #79): costs now sync.** Backend
    `animal_costs` table and `POST /api/costs/sync`, scoped `GET /api/costs` and `/{animalId}`; Android
    `CostApiClient`, `CostRepository`, `CostSyncWorker`, Room v35 (`animal_costs.sync_status`, `synced_at`,
    `MIGRATION_34_35`), and `ANIMAL_COST` in the Day-7 wipe. Original note: **Costs still have no sync path;
    mortalities now do (PR #77).**
35. **Legacy mortalities belong to whoever syncs first.** Mortalities recorded before v34 were never
    queued (the queue is user-scoped and they have no owner). `MortalityRepository.syncPending` queues
    them for the signed-in user, so on a shared device the first user to sync owns them on the server.
36. **Mortality queue items start their Day-7 clock when queued.** Legacy mortalities get a fresh
    `pending_sync.createdAt` when they are first queued, not when they were recorded, so the 7-day wipe
    window starts then. This is deliberate (it avoids wiping data that never had a chance to sync),
    but it means an old unsynced mortality can outlive 7 days.
37. **Costs derived from treatments sync as separate rows.** `TreatmentDao.insertWithCost` writes an
    `animal_costs` row (`source_entity = TREATMENT`) next to the treatment, and both now reach the server.
    A future dashboard or report that adds treatment cost to cost totals must use one source, or it counts
    the treatment twice. Derived rows are queued by `CostRepository.syncPending`, not when the treatment
    is saved, so they upload on the next cost sync (scheduled, or Retry Sync).
38. **Costs recorded before v35 belong to whoever syncs first, and their Day-7 clock starts when queued.**
    Same behaviour as #35 and #36, for `animal_costs`.
39. **PR #78 left problems on `main`.** (a) `MainActivity` runs hard-coded dev repair and diagnostic blocks
    on every launch (`BEEFTECH_TEST22_LEGACY_QUEUE_REPAIR`, `BEEFTECH_QUEUE_DIAGNOSTIC`,
    `BEEFTECH_TEST21_OWNER_REPAIR`) that reassign queued rows to the signed-in user, breaking the
    `pending_sync.user_id` ownership boundary. (b) `AuthRepository`'s identity reconciliation takes an
    optional `pendingSyncDao` that `MainActivity` never passes, so it never runs, and it is untested.
    (c) There are two `FarmerSyncScheduler` objects and `FarmerMappers.kt` is unused. (d) The new
    `PendingSyncDao` methods broke the `PendingSyncDao` fakes in `:android:farm-traceability` and
    `:android:calf-registration` unit tests, so those modules did not compile their tests on `main`; fixed in
    the cost sync PR.
40. **A voided record stays on the device and the device is not told.** Void is server-side only
    (corrections are void-only, so there is no server-to-device pull path yet). The worker's phone keeps
    showing the record as synced, and a retried sync still reports `SYNCED` without bringing the record back.
    Add a read-only "voided" flag on the device when the pull path is built.
41. **The calf certificate and media routes still serve voided calves.** They are reached by tag, are not
    scoped (see #22) and do not check `voided_at`. Scope them and hide voided calves together.
42. **Voiding a treatment leaves its derived cost row.** The treatment's `animal_costs` row
    (`source_entity = TREATMENT`) is not voided with it, and costs have no void yet. Void the derived cost
    with its treatment, or add cost void, before cost totals feed any report.
43. **Void has no un-void and no edit.** A wrong void can only be fixed by the worker re-capturing the
    record. Add an audited un-void if managers ask for one.
44. **The records review list is capped and has no paging or filters.** `GET /api/records/{type}` returns
    the newest 100 records (at most 500 with `limit`) for one type, with no search, date range or per-worker
    filter. The screen asks for the default 100. Add paging and a worker filter when a site has more.
45. **The records review screen has not been run on a device.** It is covered by view model tests and the
    backend tests only. Check the Records tab, the Void dialog and the voided state on an emulator against a
    local backend (patch both `DEFAULT_BASE_URL`s, see the handoff notes).

## Found during the Admin tab, Phase 4a (audit log)

46. **Admin actions before 4a are not in the audit log.** User create, update, PIN reset and unbind now
    write `audit_log` rows (actions `USER_CREATE`, `USER_UPDATE`, `USER_RESET_PIN`, `USER_UNBIND_DEVICE`),
    but anything done before the deploy has no row. Only voids were logged before.
47. **Audit log details are plain strings.** `audit_log.details` holds a flat JSON object of strings such as
    `{"role":"3->2"}`. The app shows it as text and does not parse it. A PIN or hash is never written to it;
    `AuditLogRoutesTest` checks this.
48. **The audit log grows forever and has no search.** `GET /api/audit-log` filters by action, entity,
    actor, site and time, and pages by `before=<id>`, but there is no free-text search and nothing is pruned.
    The Android screen filters only by action and time range (24 hours, 7 days, 30 days, all).
49. **The audit log screens have not been run on a device.** The Admin tab, the manager's Team "Activity"
    section and the audit log screen are covered by view model, backend and tab tests only. Check them on an
    emulator against a local backend (patch both `DEFAULT_BASE_URL`s, see the handoff notes).

## Found during the Admin tab, Phase 4b (sites)

50. **Deactivated sites still accept syncs.** A site's `active` flag only stops new users being assigned
    to it (and a site with active users can't be deactivated). Workers already on it keep signing in and
    syncing, and their records are stamped with its id as before. There is no delete: users and records
    point at a site by id.
51. **Site ids are server-generated (`site-<8 hex>`), and a site's name is not a key.** Rename is safe; an
    id can't be changed. The dev seed site `dev-site-1` keeps its hand-written id.
52. **Managers see only their own site in `GET /api/sites`.** The Team tab's picker is for admins, so
    managers don't load sites and still see a raw site id on member cards.
53. **The Dashboard now reads the caller's role and site from the database, not the token.** A manager who
    is moved, demoted or deactivated sees it on the next request (4c extends this to every route). Records
    and the site picker have not been run on a device.

## Found during the Admin tab, Phase 4c (revocation, devices, login security)

54. **The device id is client-supplied and can be spoofed.** `devices` and the revoke check use the `device_id`
    the app sends at login (and puts in the token). A malicious client can claim another id. Revoking a phone
    stops the honest app on it; it is not hardware attestation.
55. **`devices` and `login_events` grow forever.** Nothing is pruned. A busy deployment will want a retention
    window. `login_events.username_attempted` holds whatever was typed, including names that don't exist,
    so treat it as personal data (admin-only).
56. **Old apps send no device model or app version.** The device list shows them blank until the worker updates.
    A login request with `device_model` / `app_version` reaches an old server as unknown keys: deploy the server first.
57. **A revoked phone that isn't reinstated hits the Day-7 wipe.** Revoking blocks sign-in and sync; the unsynced
    data on it can only upload after an admin reinstates the phone. Revoke only when the phone is lost, and
    reinstate promptly if the data matters.
58. **The login lockout counter now starts again after a lock runs out.** Before, one wrong PIN after the
    lock expired locked the user again at once (the count was never reset). Users now get a fresh five attempts.
59. **Every authenticated request now reads the user and phone from the database.** One small SQLite
    transaction per request; the `last_seen` / `device_last_sync` writes are throttled to every 15 minutes.
    Tokens issued before `iat_ms` existed stay valid until they expire, unless that user has a cut-off set.
60. **Role and site now come from the database on every route, not only the services.** A role or site change
    takes effect on the next request without a new login. The Android half of 4c (401 handling, device and
    login-security screens) is not built yet.
