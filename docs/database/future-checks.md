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
