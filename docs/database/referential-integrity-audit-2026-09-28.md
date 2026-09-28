# Referential-integrity audit (R5.2)

Status: written alongside the R5.1 fix (branch `fix/R5.1-animal-costs-fk`), 2026-09-28.
Owner: whoever gets device access next should run the queries below and
replace the "not yet run" sections with real results.

**This note documents queries and schema-level foreign-key coverage. It does
not contain real audit results.** No real device database exists in this
development environment (see `AGENT.md`), so nothing here has been run
against actual field data. Treat every "0 rows expected" statement below as
a claim about what the schema *should* produce, not a confirmed finding.

## What this is

`android/database/src/main/java/com/beeftech/database/BeefTechDatabase.kt`
declares 23 foreign keys as of schema version 23 (after R5.1 added
`animal_costs.animalId -> animals.animalId`). The runnable companion to this
note, `referential-integrity-audit-2026-09-28.sql`, has one anti-join query
per declared FK: `SELECT ... FROM <child> WHERE <fk column> NOT IN (SELECT
<parent column> FROM <parent>)`, guarded with `IS NOT NULL` for the three FK
columns that are nullable (`animals.animalGroupId`, `users.role`,
`calf_registrations.dam_id`/`sire_id`).

## Why orphans could still exist despite the FKs being declared

Room only enables `PRAGMA foreign_keys` when it opens the database, and a
migration runs with FK enforcement off (`AGENT.md`'s "Foreign keys are not
enforced while migrations run" trap). So a device that:

- upgraded through an **older** migration chain, before the relevant FK
  existed on that table (e.g. any `animal_costs` row written before this
  branch's `MIGRATION_22_23` on a device that hasn't upgraded past v22 yet),
  or
- has a row that predates the FK and has never since been touched by a
  write that would trigger SQLite's runtime check,

could still be carrying an orphaned row that the schema now forbids for new
writes. That's exactly the gap this audit is for.

## What every anti-join query should find (and won't check by itself)

For a device fully migrated to v23, every query in the `.sql` file is
expected to return 0 rows, because:

- Every FK's own migration (`MIGRATION_14_16` for the earlier batch,
  `MIGRATION_22_23` for `animal_costs`) already ran an orphan sweep
  (re-key-then-quarantine, R0.5/R5.1) before declaring the constraint, and
  ended with its own `PRAGMA foreign_key_check`.
- Once open, Room enforces every FK on every write from then on.

A non-zero result on a real device would mean either an upgrade path this
audit hasn't accounted for, or a write that happened with FK enforcement
somehow bypassed (e.g. raw SQL outside Room). Either is worth its own
incident note, following this file's structure.

## What this audit deliberately does not cover

- **`quarantine_<table>` rows.** `MIGRATION_14_16` and `MIGRATION_22_23`
  copy every row they remove into `quarantine_<table>` before deleting it
  (rule 3). Those tables are not Room entities and carry no FK of their
  own by design -- they're a deliberate un-constrained holding pen, not a
  gap to close. Auditing what's in them is a different, worthwhile task
  (how much got quarantined, and can any of it still be repaired?) but it's
  not a referential-integrity question, so it's out of scope here.
- **Backend referential integrity.** The backend (`backend/api`) has zero FK
  or existence check on any animal-reference column
  (`calf_registrations.animal_uuid`, `treatments.animal_id`,
  `animal_movements.animal_id`, etc. are all plain unconstrained `varchar`).
  Whether it should get one is Q7 in `Instructions.md`, an open decision for
  the maintainer, not something this audit decides. R5.3 (movement source/
  destination FKs) and R5.4 (backend FKs) are separately blocked on R6 and
  Q7 respectively.
- **Relationships that are real but not FK-shaped**, e.g. `animals.parentId`
  (freeform, superseded by `calf_registrations.dam_id`/`sire_id` — see D10/
  R4) and the string-matched `animal_ownerships.owner_name` (R6 turns this
  into a real `owner_farmer_id` FK). These aren't declared FKs today, so
  they have no anti-join query in the `.sql` file; the acceptance line in
  `Instructions.md`'s R5 section ("every relationship is either a declared
  foreign key or has a written reason for not being one") is tracked by R4/
  R6 finishing that work, not by this audit.

## How to actually run this

1. Get a copy of a real device's `beeftech.db` (SQLCipher-encrypted; needs
   the device's passphrase to open — see `android/database/DatabaseFactory.kt`
   for how the app derives/stores it). **Always work from a copy, never the
   live file.**
2. Open it with a SQLCipher-aware client (e.g. `sqlcipher` CLI, or DB Browser
   for SQLite with the SQLCipher extension) using that passphrase.
3. Run `referential-integrity-audit-2026-09-28.sql` in full. Every query
   should return 0 rows; record which ones don't.
4. For any non-empty result, check `quarantine_<table>` first (the row may
   already have a known reason it didn't resolve — see the R0.5/R5.1
   migrations) before treating it as a new finding.

## Follow-up

- [ ] Run the queries against a real device database and replace this
  section with actual counts, once device access exists.
- [ ] If any query returns rows on a real device, write a dedicated incident
  note (follow `incident-2026-09-r0-migration-data-loss.md`'s structure) and
  cross-reference it here.
- [ ] Revisit this file once R4 and R6 land, since both change which
  relationships are FK-shaped at all (`parentId` retirement, `owner_name` ->
  `owner_farmer_id`, movement source/destination FKs) and this audit's query
  list will need new entries.
