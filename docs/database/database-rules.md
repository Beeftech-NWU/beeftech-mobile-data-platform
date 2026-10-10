# Database rules (`:android:database`)

Field devices can hold up to 30 days of unsynced data. Losing local data is the worst failure this app can have. A real data-loss incident has already happened; see [incident-2026-09-r0-migration-data-loss.md](incident-2026-09-r0-migration-data-loss.md).

## Rules

- **Never use a destructive migration fallback.** Every version step needs an explicit migration that preserves data.
- Values that can't be mapped go into quarantine/legacy tables. Never drop them.
- Backfill GUIDs before adding a unique index on them.
- Version 15 is deliberately skipped (`MIGRATION_14_16`).
- Feature modules access persistence through repositories in `:android:database`, never by calling DAOs directly (known violations are tracked in [future-checks.md](future-checks.md)).
- Don't delete entries from `future-checks.md`; mark them resolved with the PR that resolves them.

## Changing an entity

1. Edit the entity in `entity/` and its DAO in `dao/`.
2. Bump `BeefTechDatabase.VERSION`.
3. Add `MIGRATION_n_n+1` to the `BeefTechDatabase` companion object (migrations 1-8 live in `DatabaseFactory.kt`, 9 onward in `BeefTechDatabase.kt`).
4. Register it in `DatabaseFactory.kt` `.addMigrations(...)`, wrapped in `guarded(...)`.
5. Commit the exported schema JSON under `android/database/schemas/com.beeftech.database.BeefTechDatabase/`.
6. Add a `MigrationTestHelper` test in `android/database/src/androidTest/java/com/beeftech/database/`, modelled on `Migration30To31Test.kt` / `Migration16To17Test.kt`.
7. Run `./gradlew :android:database:connectedAndroidTest` (needs an emulator).

## Note on older docs

Some older documents in this folder cite `AGENT.md` and `Instructions.md` (including numbered rules such as "rule 6"). Those files were never committed to this repository, so the citations can't be followed. The rules above are the ones currently written down in-repo.
