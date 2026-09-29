# Incident note: data loss in MIGRATION_10_11 and MIGRATION_14_16

Status: written alongside the R0 fix (branch `fix/r0-stop-data-loss`), 2026-09-28.
Q6 answered and this note closed out 2026-09-28 — see below.

## What was wrong

Two migrations that had already shipped to `main` could destroy data on a real
device, silently:

1. **`MIGRATION_10_11` (D2, D3)** — introduced in commit `858548c`
   ("Fix Room database integration with version 11 migration"), 2026-09-22.
   On any device upgrading from schema version 10 or earlier:
   - `animal_movements` was dropped and recreated empty. Every movement record
     was lost.
   - `calf_registrations` was dropped and recreated empty. Every calf
     registration (birthdate, breed, photo/video paths, GPS, dam/sire tags)
     was lost.
   - `suppliers` was dropped entirely. GLN numbers and purchase batch numbers
     were lost.
   - `treatments` was rebuilt without copying a `recordguid`, so every
     treatment got `recordguid = ''`. With two or more treatments, the
     `CREATE UNIQUE INDEX` that followed threw and the whole upgrade failed
     (the app showed "Unable to open the local database"). With zero or one
     treatment, the upgrade "succeeded" but left a blank `recordguid` behind.
2. **`MIGRATION_14_16` (N1, N2)** — merged in #47's predecessor, before
   `RoleSeed` existed. On any device upgrading through it:
   - Every `farmer_roles` row was deleted, because the migration's own orphan
     cleanup checked `role_id` against `roles`, and nothing had ever seeded
     `roles` at that point (`RoleSeed` only ran in `SEED_CALLBACK.onOpen`,
     which runs after every migration finishes — see #47).
   - Orphaned/duplicate `treatments`, `mortalities`, `animal_group_memberships`,
     `farmer_addresses`, `feed_crib_readings` and `sync_backups` rows were
     `DELETE`d outright, with no copy kept, including rows that had not yet
     synced to the backend.
3. **The destructive fallback (D4)** — `DatabaseFactory.create` called
   `.fallbackToDestructiveMigration(dropAllTables = true)` and
   `.fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)`. Any
   unhandled migration exception (including the D3 crash above) or a version
   downgrade would silently wipe the entire encrypted database rather than
   fail visibly.

## What the R0 branch fixes, and what it doesn't

- `MIGRATION_10_11` and `MIGRATION_14_16` were rewritten in place (not
  renumbered — see the note on rule 6 below) to keep every row, quarantine
  instead of delete, and seed `roles` before reading it.
- A new `MIGRATION_17_18` repairs any blank/NULL GUID left behind by the old
  behaviour, for a device that is already past v11.
- The destructive fallback was removed; a migration failure now surfaces as
  `DatabaseResult.Error(DatabaseErrorType.MIGRATION_FAILED, ...)` instead of
  wiping the database.

**This does not recover data that a device already lost.** If a device ran
the old `MIGRATION_10_11` and had 2+ unsynced treatments, or ran the old
`MIGRATION_14_16` with any farmer roles, movements, calf registrations,
suppliers or orphaned/duplicate rows present at the time, that data is gone
from the device — the rewritten migrations only change what happens to
devices that have not upgraded past v10 (for D2/D3) or v14 (for N1/N2) yet.

## Q6, answered: which builds have run on field devices?

**Answer (confirmed with the maintainer, 2026-09-28): none. No build of this app has
ever been installed on a real farmer/feedlot device.** `android/app`, the production
app module, is an empty stub (no manifest, no source — just a placeholder); the only
buildable app in this repo is `demoapp`, which exists for manual testing, not field
distribution. There is no release/deploy pipeline, CHANGELOG, or version-tracking
artifact anywhere in the repo, and the one candidate data source this note originally
pointed at — `users.device_last_sync` on the backend — turns out to record only a
sync timestamp, not an app/build version, so it couldn't have answered this anyway.
Every device that has ever run `MIGRATION_10_11` or the pre-`RoleSeed`
`MIGRATION_14_16` was a dev, test, or demo device.

This confirms the assumption this branch proceeded on was correct:
- Editing `MIGRATION_10_11` / `MIGRATION_14_16` in place (as this branch does) was
  safe under rule 6 in `AGENT.md` ("don't edit a migration that has only run on test
  devices... may be corrected, and the PR must say so") — no field device has run
  either migration, so no in-place edit needed to become a new migration instead.
- No real device ever hit the data loss this note describes. The "recovery"
  procedure below was never needed and remains purely hypothetical unless field
  distribution begins in the future.

## If a field device did run the old migrations: recovery

Because the device is offline-first, the backend is the only other copy of
anything that had already synced before the loss:

1. **Movements, calf registrations, treatments, supplier records**: for any
   row whose `recordguid`/sync identity was already sent to the backend
   before the device upgraded, the backend copy is unaffected — the device
   lost its **local** copy, not the synced one. Re-pull from the backend for
   that device/farmer once a read-back sync path exists (none did as of this
   note; that's part of the wider remediation plan, not R0).
2. **Anything that had not synced yet** (by definition, up to 30 days of
   capture on this app) has no other copy anywhere. It cannot be recovered.
   The device operator needs to re-capture it if it's still knowable
   (e.g. re-register a calf that's still in the pen) — most of it (an
   in-the-moment GPS capture, a past movement date) cannot be reconstructed
   after the fact.
3. **Farmer roles** deleted by the old `MIGRATION_14_16`: these are login/
   permission records, not field captures, so they should still exist as
   whatever the backend/login API considers the source of truth. Re-running
   login (or whatever provisions `farmer_roles` from the backend) should
   restore them without any device-side recovery.

## Follow-up

- [x] Answer Q6 (see `Instructions.md`) and confirm the in-place edit here was
  the right call, not a rules violation. **Answered above: no field device exists,
  so the in-place edit was correct.**
- [x] If any field device is confirmed affected, notify the affected
  farmer(s)/feedlot(s) and attempt the backend-side recovery above. **N/A — no field
  device has ever run this app.**
- [x] Once R0.8's CI emulator job has run on this branch a few times without
  flaking, remove this note's "assumption" framing and record the actual
  answer to Q6 here. **Done — see the "Q6, answered" section above.**
