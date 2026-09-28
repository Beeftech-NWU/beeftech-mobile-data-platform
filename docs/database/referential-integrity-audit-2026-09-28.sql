-- Referential-integrity orphan audit (R5.2).
--
-- One anti-join per declared foreign key in schema version 23
-- (android/database/schemas/com.beeftech.database.BeefTechDatabase/23.json,
-- current as of R5.1). Each query returns the child rows whose FK column
-- doesn't resolve to a parent row.
--
-- Every one of these SHOULD return zero rows on a healthy database: Room
-- enables `PRAGMA foreign_keys` on open, so the app can't write a genuine
-- orphan once these constraints exist. This audit exists for two other
-- cases:
--   1. A device that upgraded through an OLDER migration chain (before the
--      relevant FK was added) and has never since had a row inserted that
--      would trigger a foreign_key_check.
--   2. `quarantine_<table>` rows (R0.5, R5.1) -- deliberately NOT FK
--      constrained, kept here as raw copies of what was removed, so an
--      operator can see what a migration quarantined.
--
-- Run each SELECT against a copy of a real device's `beeftech.db` (decrypt
-- with SQLCipher and the device passphrase first; never run this against a
-- production file without a backup). No real device database exists in
-- this development environment, so these queries are UNVERIFIED -- see the
-- companion write-up, referential-integrity-audit-2026-09-28.md.

-- ============================================================
-- animals.animalGroupId -> animal_groups.animalGroupId (SET NULL, nullable)
-- ============================================================
SELECT animalId, animalGroupId
FROM animals
WHERE animalGroupId IS NOT NULL
  AND animalGroupId NOT IN (SELECT animalGroupId FROM animal_groups);

-- ============================================================
-- animal_costs.costType -> cost_types.code (RESTRICT, not null)
-- ============================================================
SELECT id, costType
FROM animal_costs
WHERE costType NOT IN (SELECT code FROM cost_types);

-- ============================================================
-- animal_costs.animalId -> animals.animalId (CASCADE, not null) -- R5.1
-- ============================================================
SELECT id, animalId
FROM animal_costs
WHERE animalId NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- animal_group_memberships.animal_id -> animals.animalId (CASCADE, not null)
-- ============================================================
SELECT membership_id, animal_id
FROM animal_group_memberships
WHERE animal_id NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- animal_group_memberships.group_id -> animal_groups.animalGroupId (RESTRICT, not null)
-- ============================================================
SELECT membership_id, group_id
FROM animal_group_memberships
WHERE group_id NOT IN (SELECT animalGroupId FROM animal_groups);

-- ============================================================
-- animal_movements.animal_id -> animals.animalId (CASCADE, not null)
-- ============================================================
SELECT movement_id, animal_id
FROM animal_movements
WHERE animal_id NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- farmer_addresses.farmer_id -> farmers.farmer_id (CASCADE, not null)
-- ============================================================
SELECT address_id, farmer_id
FROM farmer_addresses
WHERE farmer_id NOT IN (SELECT farmer_id FROM farmers);

-- ============================================================
-- farmer_roles.farmer_id -> farmers.farmer_id (CASCADE, not null)
-- ============================================================
SELECT farmer_role_id, farmer_id
FROM farmer_roles
WHERE farmer_id NOT IN (SELECT farmer_id FROM farmers);

-- ============================================================
-- farmer_roles.role_id -> roles.role_id (RESTRICT, not null)
-- ============================================================
SELECT farmer_role_id, role_id
FROM farmer_roles
WHERE role_id NOT IN (SELECT role_id FROM roles);

-- ============================================================
-- feed_crib_readings.cribId -> feed_cribs.id (CASCADE, not null)
-- ============================================================
SELECT id, cribId
FROM feed_crib_readings
WHERE cribId NOT IN (SELECT id FROM feed_cribs);

-- ============================================================
-- feed_crib_reading_values.readingId -> feed_crib_readings.id (CASCADE, not null)
-- ============================================================
SELECT id, readingId
FROM feed_crib_reading_values
WHERE readingId NOT IN (SELECT id FROM feed_crib_readings);

-- ============================================================
-- users.role -> roles.role_id (SET NULL, nullable)
-- ============================================================
SELECT user_id, role
FROM users
WHERE role IS NOT NULL
  AND role NOT IN (SELECT role_id FROM roles);

-- ============================================================
-- sync_backups.batchId -> sync_batches.id (CASCADE, not null)
-- ============================================================
SELECT id, batchId
FROM sync_backups
WHERE batchId NOT IN (SELECT id FROM sync_batches);

-- ============================================================
-- treatments.animalId -> animals.animalId (CASCADE, not null)
-- ============================================================
SELECT id, animalId
FROM treatments
WHERE animalId NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- mortalities.animalId -> animals.animalId (CASCADE, not null)
-- ============================================================
SELECT id, animalId
FROM mortalities
WHERE animalId NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- animal_identifiers.animal_id -> animals.animalId (CASCADE, not null)
-- ============================================================
SELECT identifier_id, animal_id
FROM animal_identifiers
WHERE animal_id NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- animal_media.animal_id -> animals.animalId (CASCADE, not null)
-- ============================================================
SELECT media_id, animal_id
FROM animal_media
WHERE animal_id NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- animal_weights.animal_id -> animals.animalId (CASCADE, not null)
-- ============================================================
SELECT weight_id, animal_id
FROM animal_weights
WHERE animal_id NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- animal_ownerships.animal_id -> animals.animalId (CASCADE, not null)
-- ============================================================
SELECT ownership_id, animal_id
FROM animal_ownerships
WHERE animal_id NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- animal_purchases.animal_id -> animals.animalId (CASCADE, not null)
-- ============================================================
SELECT purchase_id, animal_id
FROM animal_purchases
WHERE animal_id NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- calf_registrations.registered_animal_id -> animals.animalId (CASCADE, not null)
-- ============================================================
SELECT registration_id, registered_animal_id
FROM calf_registrations
WHERE registered_animal_id NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- calf_registrations.dam_id -> animals.animalId (SET NULL, nullable)
-- ============================================================
SELECT registration_id, dam_id
FROM calf_registrations
WHERE dam_id IS NOT NULL
  AND dam_id NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- calf_registrations.sire_id -> animals.animalId (SET NULL, nullable)
-- ============================================================
SELECT registration_id, sire_id
FROM calf_registrations
WHERE sire_id IS NOT NULL
  AND sire_id NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- animals.breed -> breeds.breedId (RESTRICT, not null) -- R7
-- ============================================================
SELECT animalId, breed
FROM animals
WHERE breed NOT IN (SELECT breedId FROM breeds);

-- ============================================================
-- animals.hideColour -> hide_colours.colourId (SET NULL, nullable) -- R7
-- ============================================================
SELECT animalId, hideColour
FROM animals
WHERE hideColour IS NOT NULL
  AND hideColour NOT IN (SELECT colourId FROM hide_colours);

-- ============================================================
-- animals.deviceId -> devices.deviceId (RESTRICT, not null) -- R7
-- ============================================================
SELECT animalId, deviceId
FROM animals
WHERE deviceId NOT IN (SELECT deviceId FROM devices);

-- ============================================================
-- treatments.disease -> diseases.diseaseId (RESTRICT, not null) -- R7
-- ============================================================
SELECT id, disease
FROM treatments
WHERE disease NOT IN (SELECT diseaseId FROM diseases);

-- ============================================================
-- treatments.deviceId -> devices.deviceId (RESTRICT, not null) -- R7
-- ============================================================
SELECT id, deviceId
FROM treatments
WHERE deviceId NOT IN (SELECT deviceId FROM devices);

-- ============================================================
-- mortalities.necropsy_code_id -> necropsy_codes.necropsyCodeId (SET NULL, nullable) -- R7
-- ============================================================
SELECT id, necropsy_code_id
FROM mortalities
WHERE necropsy_code_id IS NOT NULL
  AND necropsy_code_id NOT IN (SELECT necropsyCodeId FROM necropsy_codes);

-- ============================================================
-- farmer_addresses.province -> provinces.provinceId (SET NULL, nullable) -- R7
-- ============================================================
SELECT address_id, province
FROM farmer_addresses
WHERE province IS NOT NULL
  AND province NOT IN (SELECT provinceId FROM provinces);

-- ============================================================
-- animal_movements.device_id -> devices.deviceId (RESTRICT, not null) -- R7
-- ============================================================
SELECT movement_id, device_id
FROM animal_movements
WHERE device_id NOT IN (SELECT deviceId FROM devices);

-- ============================================================
-- animal_weights.device_id -> devices.deviceId (RESTRICT, not null) -- R7
-- ============================================================
SELECT weight_id, device_id
FROM animal_weights
WHERE device_id NOT IN (SELECT deviceId FROM devices);

-- ============================================================
-- medication_batches.medicationId -> medications.medicationId (CASCADE, not null) -- R7
-- ============================================================
SELECT batchId, medicationId
FROM medication_batches
WHERE medicationId NOT IN (SELECT medicationId FROM medications);

-- ============================================================
-- provinces.countryId -> countries.countryId (CASCADE, not null) -- R7
-- ============================================================
SELECT provinceId, countryId
FROM provinces
WHERE countryId NOT IN (SELECT countryId FROM countries);

-- ============================================================
-- animal_identifiers.identifier_type -> identifier_types.code (RESTRICT, not null) -- R4
-- ============================================================
SELECT identifier_id, identifier_type
FROM animal_identifiers
WHERE identifier_type NOT IN (SELECT code FROM identifier_types);

-- ============================================================
-- animals.dam_id -> animals.animalId (SET NULL, nullable) -- R4
-- ============================================================
SELECT animalId, dam_id
FROM animals
WHERE dam_id IS NOT NULL
  AND dam_id NOT IN (SELECT animalId FROM animals);

-- ============================================================
-- animals.sire_id -> animals.animalId (SET NULL, nullable) -- R4
-- ============================================================
SELECT animalId, sire_id
FROM animals
WHERE sire_id IS NOT NULL
  AND sire_id NOT IN (SELECT animalId FROM animals);

