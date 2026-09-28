# R4 Implementation Plan: Make History Capture Real

## Overview & Objectives

Package R4 ("Make history capture real") refactors the BeefTech database schema and Kotlin data access layer to eliminate dual sources of truth for animal attributes and enforce normalized history capture across weights, group memberships, tag identifiers, and parentage.

Specifically, R4 addresses:
- **D5 (No weight or group history)**: Moving weight readings and group memberships out of direct `Animal` entity properties and into dedicated history tables (`animal_weights` and `animal_group_memberships`).
- **D10 (Two sources of truth on `animals`)**: Eliminating legacy columns on `animals` (`tagNumber`, `oldTagNumber`, `temperatureNumber`, `referenceNumber`, `massKg`, `condition`, `age`, `photoPath`, `videoPath`, `animalGroupId`, `parentId`) in favor of normalized tables (`animal_identifiers`, `animal_media`, `animal_weights`, `animal_group_memberships`, and `dam_id`/`sire_id` FKs).

---

## Resolved Architectural Decisions

1. **Q1 (Parentage scope)**: Embryo transfer is **out of scope**. Parentage is represented by direct `dam_id` and `sire_id` foreign keys on `animals` pointing to parent `animals(animalId)` with `ON DELETE SET NULL`.
2. **Q3 (Identifier lifecycle & permanence)**: A tag number is permanently unique across time and cannot be reassigned to a different animal once assigned. Within an animal, at most one active (`valid_to IS NULL`) identifier per type is permitted.
3. **Migration Granularity**: Executed as four discrete sequential migrations (`Room v24 → v28`):
   - **R4.1 (v24 → v25)**: Add `body_condition_score` to `animal_weights`, introduce `identifier_types` lookup table with FK constraint, and enforce identifier constraints via SQL triggers.
   - **R4.2 (v25 → v26)**: Backfill data from legacy `animals` columns into `animal_identifiers`, `animal_media`, `animal_weights`, and `animal_group_memberships`. Enforce single active group membership via trigger.
   - **R4.3 (v26 → v27)**: Add `dam_id` and `sire_id` FKs on `animals` and migrate legacy `parentId` data.
   - **R4.4 (v27 → v28)**: Drop legacy columns on `animals` and finalize the v28 schema.

---

## Migration Breakdown (Room v24 → v28)

### Migration 24 → 25 (R4.1: Lookup & Identifier Constraints)
- **`animal_weights`**:
  - Add column `body_condition_score TEXT`.
- **`identifier_types`**:
  - Create lookup table `identifier_types (code TEXT PRIMARY KEY, name TEXT NOT NULL, validation_regex TEXT)`.
  - Seed codes: `TAG`, `OLD_TAG`, `REFERENCE`, `TEMPERATURE`, `TRANSPONDER`.
- **`animal_identifiers`**:
  - Rebuild table to add foreign key `FOREIGN KEY(identifier_type) REFERENCES identifier_types(code) ON DELETE RESTRICT`.
- **Triggers**:
  - `trg_animal_identifiers_unique_active`: Enforces at most one active (`valid_to IS NULL`) identifier per type per animal.
  - `trg_animal_identifiers_tag_permanence`: Prevents assigning a `TAG` value to an animal if it was ever assigned to a different animal.

### Migration 25 → 26 (R4.2: Normalized Data Backfill)
- Backfill `animal_identifiers` from `animals.tagNumber` (`TAG`), `oldTagNumber` (`OLD_TAG`), `referenceNumber` (`REFERENCE`), and `temperatureNumber` (`TEMPERATURE`).
- Backfill `animal_media` from `animals.photoPath` (`PHOTO`) and `videoPath` (`VIDEO`).
- Backfill `animal_weights` from `animals.massKg` (storing `condition` as `body_condition_score`).
- Backfill `animal_group_memberships` from `animals.animalGroupId`.
- Trigger `trg_animal_group_memberships_single_open`: Enforces at most one open (`left_at IS NULL`) group membership per animal.

### Migration 26 → 27 (R4.3: Dam & Sire Foreign Keys)
- Rebuild `animals` table to add `dam_id` and `sire_id` foreign keys referencing `animals(animalId)` `ON DELETE SET NULL`.
- Backfill `dam_id` from legacy `animals.parentId` (resolving UUID or tag number).

### Migration 27 → 28 (R4.4: Legacy Column Clean-up)
- Rebuild `animals` table to drop legacy columns: `tagNumber`, `oldTagNumber`, `temperatureNumber`, `referenceNumber`, `massKg`, `condition`, `age`, `photoPath`, `videoPath`, `animalGroupId`, `parentId`.
- Retain core attributes: `animalId`, `birthdate`, `breed`, `gender`, `hideColour`, `brandMark`, `dam_id`, `sire_id`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `record_guid`, `syncStatus`, `syncedat`.

---

## App Code Changes

1. **Entities**:
   - `IdentifierType.kt`: New entity and seed (`IdentifierTypeSeed`).
   - `AnimalWeightEntity.kt`: Added `bodyConditionScore` property.
   - `AnimalIdentifierEntity.kt`: Foreign key updated to reference `IdentifierType`.
   - `Animal.kt`: Dropped legacy fields; added `damId` and `sireId`.
2. **Repositories**:
   - `AnimalHistoryRepository.kt`: New repository providing `recordWeighIn`, `getWeightHistory`, `changeGroup`, `getGroupHistory`, `retagIdentifier`, `getActiveIdentifiers`, and `getIdentifierHistory`.
   - `AnimalManagementRepository.kt`: Removed legacy `massKg` / `condition` parameters from update routines; updated query delegates to `AnimalDao`.
3. **DAOs**:
   - `AnimalDao.kt`: Removed queries on dropped columns; updated tag / reference / temperature queries to join `animal_identifiers`.
   - `AnimalGroupMembershipDao.kt`: Added `closeOpenMemberships` and flow getters.
   - `AnimalIdentifierDao.kt`: Added `closeActiveIdentifier`.
4. **Mappers & UI Integration**:
   - `CalfRegistrationMappers.kt`: Removed dual-writing to `animals.tagNumber`.

---

## Verification & Testing Plan

1. **Migration Tests**:
   - Create `Migration24To28Test.kt` verifying each step from v24 through v28.
   - Assert data preservation, trigger enforcement (duplicate tag rejection, multi-open group membership rejection), and FK integrity via `PRAGMA foreign_key_check`.
2. **Unit & Instrumented Tests**:
   - Update `AnimalDaoTest`, `AnimalCostDatabaseTest`, `CalfRegistrationDaoTest`, and related test suits to match the v28 schema.
   - Execute `./gradlew testDebugUnitTest test` and `./gradlew :android:database:connectedAndroidTest`.

---

## R4.1 Corrections & Hardening

1. **`legacy_animals` Snapshot**: Added `CREATE TABLE legacy_animals` in `MIGRATION_26_27` to preserve all legacy columns (`age`, `condition`, `parentId`, duplicate tags, etc.) before `MIGRATION_27_28` drops them.
2. **Identifier Type Mapping & Quarantine**: Remapped `RFID` → `TRANSPONDER` and `OLDTAG` → `OLD_TAG` in `MIGRATION_24_25`. Unrecognized types are preserved in `quarantine_animal_identifiers`.
3. **Duplicate Tag Handling**: Excluded shared legacy tags (`HAVING COUNT(DISTINCT animalId) > 1`) from `animal_identifiers` insertion to avoid permanence trigger aborts. Both instances remain in `legacy_animals`.
4. **Deterministic Dam Resolution**: In `MIGRATION_26_27`, parentage is resolved only for unique `TAG` matches and UUID matches, excluding self-references.
5. **Fresh Install Triggers**: Added `createHistoryTriggers` to `SEED_CALLBACK` so fresh database installs automatically receive active identifier and single open group membership triggers.

