# Add Crib

Add Crib is an explicit extension requested by the user. It creates a physical crib reference once; it does not create a reading.

## Try it in the demo

Rebuild/run the demo and restart the backend with the updated source. Sign in as an administrator, open Feed and tap the bottom-right **+** button.

The popup downloads and caches setup references. With development seeding enabled, the existing `dev-site-1` site has pens 1–8 and the ration catalogue uses the same identifiers/names as the application's existing RationSeed. Select a pen/ration, enter a new code and Initial ADI, and Save. The crib appears immediately with PENDING status. Entering `a009` creates display code `A009` and preserves its zeros.

Disconnect after setup has downloaded to exercise offline creation and readings. The Feed worker uploads parent cribs before their readings. Manual Feed Sync does the same. A successful backend acknowledgement changes creation status to SYNCED. Cancel does not call the repository; changed forms ask before discarding.

Only ADMIN can create cribs. There is no existing explicit crib-management permission in this app, so MANAGER/WORKER are not granted creation automatically. Authenticated site users may download and capture readings against existing cribs. Admin downloads and reading uploads explicitly name a site; normal users remain limited to the site in their token.

## Reference data and configuration

Production setup must provision existing pens in `feed_pens` with their site IDs. Add Crib does not create pens. Rations use `feed_rations`; defaults mirror the existing mobile ration seed. No methods are invented: the optional picker uses distinct methods already present on backend cribs.

Pen descriptions, counts and required kg come from pen references. Null counts/feed show **Not available**. New records store pen/ration IDs, not copies of animal counts. The flattened legacy columns remain for old crib compatibility. Reference responses and the local cache provide the display projection.

Add Crib ADI rules are downloaded and cached with setup. Backend environment configuration:

| Variable | Default |
| --- | --- |
| `BEEFTECH_ADI_MIN` | 0 |
| `BEEFTECH_ADI_MAX` | Unset (no approved maximum) |
| `BEEFTECH_ADI_STEP` | 0.1 |
| `BEEFTECH_ADI_UNITS` | Unconfirmed |

The popup labels these as temporary. No ADI formula is introduced. Existing reading adjustment/time-slot/session rules are retained. RFID scanning remains deferred; optional manual tag entry and uniqueness validation are supported by this new form.

## Persistence and conflicts

Room v48 adds crib UUID identity, creation metadata, sync status, and a user-scoped setup cache. Migration preserves existing cribs/readings and assigns deterministic UUIDs to legacy rows. New creations use random UUIDs. Readings carry the parent's UUID; display codes may repeat on different sites.

Location uses a recent permitted device fix where available. Missing fixes are stored as null, never fabricated as 0,0. The creator user ID, device ID, and capture timestamp are filled by the app.

Backend endpoints:

- `GET /api/feed-cribs/setup`: ADMIN-only reference snapshot.
- `POST /api/feed-cribs/sync`: ADMIN-only registration batches, maximum 200 records. Retries with the same UUID/data are acknowledged without duplicate inserts.
- Existing reading sync accepts `cribUuid`; new registered cribs require it. Admin reading sync uses `?siteId=`.
- Existing crib download includes UUIDs and site-scoped pen references.

Code uniqueness is scoped to a site and checked ignoring case/outer whitespace. RFID uniqueness is global across cribs. Database uniqueness protects concurrent creation.

A rejected duplicate is marked CONFLICT locally; its crib and readings remain intact. The creating admin can choose **Resolve conflict**, change the code/tag, and retry with the same UUID. This relabels local reading references atomically, without changing their UUIDs or observation metadata. It never merges the local crib into the conflicting backend crib. Other administrators must sign in as the original creator to resolve it.

## Validation

Tests cover permissions, missing/invalid references, initial ADI rules, duplicate codes/tags, UUID idempotency, offline creation/readings, refresh preservation, explicit conflict resolution, cross-site reading isolation, and schema migration/restart persistence.

Automatic creation sync uses the existing Feed WorkManager job and durable crib status. The shared pending-operation queue still tracks readings; creation failures also cause the Feed worker to retry. Creation-specific status/errors are shown on the Feed list.

