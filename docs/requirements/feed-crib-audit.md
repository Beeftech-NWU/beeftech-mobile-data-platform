# Feed Crib requirements audit

Working source: requirements pasted by the user on 11 October 2026, attributed to the original Beeftech brief and later functional/technical specifications. The original PDFs have not been independently checked here. Status below is based on source inspection, not a complete device acceptance test.

## Functional requirements

Add Crib is now an explicit user-requested extension. See [Add Crib implementation](feed-crib-add.md) for access, setup, persistence, API and conflict behaviour. RFID scanning remains deferred; optional manual tag entry is part of the new form.

User decision: preserve existing business rules provisionally and ignore RFID for now. FC-11, FC-14 and FC-15 are deferred. No simulated scanner is required. Subsequent user decision: new readings require a selected reading code; ADI-only capture is no longer allowed. Historical ADI-only entries remain readable and synchronisable.

| ID | Current implementation | Remaining work or decision |
| --- | --- | --- |
| FC-01 | Number entry, list selection, case-normalised local lookup and unknown-number error. | Unknown numbers must not create cribs under this requirement. Reference-data provisioning is separate. |
| FC-02 | Description, ration, method, required kg, ADI and four animal counts displayed. | No separate pen-number field exists; confirm whether crib number identifies the pen. |
| FC-03 | Three calendar days by Morning/Mid-Day/Evening; missing slots display a dash. | Confirm this interpretation of nine readings. |
| FC-04 | Downloaded active lookup codes. Backend seeds placeholder codes 0–5. | Obtain approved code values and descriptions. |
| FC-05 | Before 11:00 Morning, before 14:00 Mid-Day, otherwise Evening. | After 20:00 is currently Evening; confirm policy. Preview time can become stale while the screen remains open across a boundary. |
| FC-06 | New entries preserve older records; history selects newest coded entry per slot. Draft now previews the code in the current grid cell and explains replacement. | Confirm correction policy. Backend still permits mutation of an existing GUID by its owner. |
| FC-07 | Starts from current ADI; +/- 0.1 kg; minimum zero. | Confirm meaning, units, step and maximum. |
| FC-08 | Atomic local save, unique GUID, upload attempt, success feedback and return to selection. | First upload attempt delays returning to selection. See SH-07. |
| FC-09 | Discard draft; confirmation for changes; saved records retained. Navigation, discard and draft edits guarded while save is in progress. | Verify on device. |
| FC-10 | Manual Sync now uploads and downloads reference data/history. Background Feed worker also refreshes reference data and retries failed downloads. | Device acceptance checks needed. |
| FC-11 | Missing. | Add crib RFID mapping, local resolution and reader integration. Hardware/SDK and mappings needed. |
| FC-12 | Today's entries for signed-in user, grouped by crib, with three codes and latest ADI. | Confirm meaning of session. |
| FC-13 | Read-only session detail. | Verify on device. |
| FC-14 | Missing with RFID. | Retry/error handling depends on scan integration. |
| FC-15 | Missing; no RFID field in crib/entry contract. | Add backend and local schema support and round-trip tests once mapping contract is agreed. |

## Shared requirements as used by Feed Cribs

| ID | Current implementation | Remaining work or decision |
| --- | --- | --- |
| SH-01 | Uses shared login, token provider and current-user identity. Backend site comes from authenticated user. | Verify offline provisioning and authorisation rules with shared-login owner. |
| SH-02 | Room reference cache; download replaces cribs/codes and preserves pending entries. Manual Sync and Feed background worker refresh references. | Device acceptance checks needed. |
| SH-03 | Validates user, known active crib/code and finite nonnegative ADI locally; backend validates uploads. | New captures require a reading code, including when ADI changes. Save is disabled until a code is selected; local persistence independently rejects missing codes. |
| SH-04 | GUID, device ID, capture timestamp and user ID stored. | GPS capture missing: factory supplies default 0,0. Username is not captured as a separate snapshot. Confirm user ID versus username requirement. |
| SH-05 | Readings persist in Room before networking. | Device restart/offline acceptance checks needed. |
| SH-06 | Pending until explicit server confirmation; rejection state retained. | Already implemented. |
| SH-07 | Immediate send attempt, network-constrained worker and periodic retry; Feed worker uploads and refreshes references. | Save waits for first upload attempt. Decouple upload from capture. |
| SH-08 | GUID-based retry, preserved pending records, capped explicit rejections and manual retry. | Transport failures returned to caller but not persisted on entry rows. |
| SH-09 | Session unsynced counts, per-entry status and shared sync-run tracking in worker. Feed home shows manual Sync progress and latest result for screen lifetime; duplicate manual Sync taps ignored. | Module-wide pending count and persistent last-attempt/result summary missing; direct Save/Sync attempts bypass worker tracking. |
| SH-10 | Capture UI exposes no metadata editing or saved-record deletion. New slot readings retain old GUIDs. | Backend existing-GUID updates are mutable; agree immutable retry/correction contract. |

## Implementation sequence

1. Completed: show unsaved reading/replacement in the grid with clear Save guidance; guard discard/navigation/draft edits during save.
2. Fix capture/save lifecycle, boundary-time preview and reference refresh during sync.
3. Connect shared GPS capture and persistent Feed sync status without altering other modules' flows.
4. RFID deferred by user; no hardware or simulation implementation for now.
5. Existing business rules retained provisionally by user. Backend immutability still needs an agreed correction contract.

Keep implementation in Feed Cribs and necessary shared schema/integration points. FCR reporting and generic feed quantity/type inputs are not established requirements for this implementation.
