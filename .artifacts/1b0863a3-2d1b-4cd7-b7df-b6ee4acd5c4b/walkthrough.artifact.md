# Walkthrough — Phase 2 Referential Integrity Remediation

Completed Phase 2 of the **BeefTech Data Model — Remediation Plan**.

## Summary of Changes

### 1. Declared Foreign Keys & `onDelete` Policies
- **Child Event Tables (`CASCADE`)**:
  - `AnimalCost` -> `animals(animalId)`
  - `AnimalMovement` -> `animals(animalId)`
  - `CalfRegistration` -> `animals(animalId)`
  - `Supplier` -> `animals(animalId)`
  - `LocationFeed` -> `animals(animalId)`
  - `Mortality` -> `animals(animalId)`
  - `Treatment` -> `animals(animalId)`
  - `FarmerAddressEntity` -> `farmers(farmer_id)`
  - `FarmerRoleEntity` -> `farmers(farmer_id)`
  - `FeedCribReadingEntity` -> `feed_cribs(id)`
  - `FeedCribReadingValueEntity` -> `feed_crib_readings(id)`
  - `SyncBackupEntity` -> `sync_batches(id)`
- **Lookups (`RESTRICT` / `SET_NULL`)**:
  - `Animal` -> `animal_groups(animalGroupId)` (`RESTRICT`)
  - `User` -> `roles(role_id)` (`SET_NULL`)
  - `FarmerRoleEntity` -> `roles(role_id)` (`RESTRICT`)

### 2. Handled Broken / Unresolvable FKs
- Removed/unconstrained broken parent references where targets may not exist or cause issues (`Animal.parentId`, `CalfRegistration.damId`, `CalfRegistration.sireId`).
- Retargeted and fixed type mismatch for `FarmerRoleEntity.role_id` (changed from `String` to `Long` to match `Role.roleId` and added valid `ForeignKey` constraint).

### 3. Database Migration & Anti-Join Data Cleanup (Version 8 -> 9)
- Implemented `MIGRATION_8_9` in `DatabaseFactory.kt` containing anti-join queries to remove orphan rows and nullify orphan group references prior to table recreation with constraints.
- Enabled runtime foreign key enforcement (`PRAGMA foreign_keys = ON;`) via connection callback.

### 4. Verification & Testing
- Added comprehensive migration test (`DatabaseMigration8To9Test.kt`) verifying cascade deletion.
- Successfully built and verified all database and demo app modules (`:android:database:assembleDebug`, `:demoapp:assembleDebug`).
