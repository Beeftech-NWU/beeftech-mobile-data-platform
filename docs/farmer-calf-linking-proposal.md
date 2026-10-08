# Farmer, calf and farm traceability: implementation contract proposal

## Existing implementation observed

- Farmer registration already posts to `/api/farmers/sync`.
- Calf registration already posts to `/api/calf-registrations/sync`.
- Calves are stored as `Animal` plus `CalfRegistrationEntity` locally.
- The `Animal` and `CalfRegistrationEntity` records currently have no farmer/owner foreign key.
- This supplied archive contains Android module source, **not the backend API implementation**. The payload example below is a PROPOSAL, not an implemented endpoint.

## Recommended sequence

1. Register the farmer, preferably first. This creates a durable farmer ID. Registration must work offline.
2. Register the calf, assigning a durable animal ID and record GUID. The calf can also be registered without assigning an owner immediately, e.g. when capture takes place in the field.
3. Open Farm Traceability, choose the farmer from Registered Farmers, and explicitly **Assign animals** from the already-registered local animal list.
4. Store an ownership/association record independently of calf birth registration, as ownership can change through sales or movement. Do not overwrite historical associations.
5. Queue the association offline and synchronize it once both referenced records are known to the server. Use local IDs or UUIDs; never key ownership on a farmer name or animal display tag.

## Proposed association JSON, not implemented

```json
{
  "recordGuid": "association-uuid",
  "farmerId": "farmer-uuid",
  "animalId": "animal-uuid",
  "relationship": "OWNER",
  "effectiveFrom": "2026-10-08T00:00:00Z",
  "effectiveTo": null,
  "deviceId": "device-uuid",
  "syncStatus": "PENDING"
}
```

The backend should return a separate per-association sync result with `recordGuid`, `status`, `serverSyncedAt` and a validation message on error. It should validate farmer and animal existence, require an authorized actor, make `recordGuid` idempotent, and protect against overlapping active owners according to the business rule. Do not embed all calves in the farmer registration confirmation JSON: returning links or animal counts in a farmer detail *read* endpoint is safer and avoids changing the registration contract.

## Required implementation steps not yet included

- Add a dedicated `AnimalFarmerAssociationEntity`, DAO, migration, repository and offline outbox worker.
- Add a farmer-scoped animal selection screen and a clear `Assign selected animals` action.
- Implement and test backend association endpoints and authorization, then align Android DTOs.
- Include unit and migration tests, duplicate submissions, offline capture, reassignment and sync ordering.

## Sync placement

The full sync status should appear once on the dashboard. Farm Traceability keeps a compact pending-record summary and retry action when appropriate, along with the existing important unsynced-data warning. Farmer profile can still show its own farmer-specific registration/sync status, which is not the same as duplicating a global dashboard.
