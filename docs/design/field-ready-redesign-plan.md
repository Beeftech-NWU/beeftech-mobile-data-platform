# Field-ready redesign: implementation plan and tracker

Source: "BeefTech Mobile: Design Proposal" (CMPG 323), kept in the team notes repo at
`beeftech-agent-notes/design-proposal/BeefTech Mobile_ Design Proposal.html`.
Code references were first checked against `main` @ `78550dc` on 2026-10-06, then updated
on 2026-10-07 for the UI redesign branch `beeftech-ui-redesign` (PR #102 @ `6881125`). All
paths are repo-relative.

The proposal keeps every feature and changes how fast and safely a worker gets through
them: bottom navigation, one theme, a sync chip in every header, calf registration in
four short steps, Undo instead of pop-ups, and 56dp targets for gloves and sunlight.

PR #102 delivered a first pass of the shell (bottom navigation, header, Home, More, app-wide
sync state) and restyled most screens. It also set the visual language the rest of the plan
should follow; see [Design language](#design-language-as-built-in-pr-102).

## How to use this file

- **Claim a work package** by putting your name in the tracker's Owner column and setting
  Status to `In progress` in a small PR or a direct commit to your branch. Check that
  nobody else has claimed it first. One owner per package. Ask in the group before you
  take over someone else's.
- **One branch and one PR per package** where possible, named in the Branch column (e.g.
  `feature/ui-design-system`). Put the PR number in the tracker when you open it.
- **Status values:** `Not started` · `Blocked (by WPn)` · `In progress` · `Partly done` ·
  `In review` · `Done`. Set `Done` only once the PR is merged and the package's "Done when"
  list holds. `Partly done` means a merged (or approved) PR covers some of the package; the
  package section lists what is left.
- **Change the plan here, not in chat.** If you change scope, add a line to the
  Decision log at the bottom with the date and your name.
- Packages that share a "Wave" can be worked on in parallel.

## Tracker

| WP | Work package | Wave | Depends on | Owner | Branch | PR | Status |
|---|---|---|---|---|---|---|---|
| 0 | Settle the open decisions | 0 | — | Darian | — | — | Done |
| 1 | Shared design-system module and theme | 1 | — | Kurtleigh (theme) | `beeftech-ui-redesign` | #102 | Partly done |
| 2 | App-wide sync status source | 1 | — | Kurtleigh | `beeftech-ui-redesign` | #102 | Partly done |
| 3 | Bottom navigation, header chip and More | 2 | 1, 2, D1 | Kurtleigh | `beeftech-ui-redesign` | #102 | Partly done |
| 4 | Home screen | 2 | 1, 2, 3 | Kurtleigh | `beeftech-ui-redesign` | #102 | Partly done |
| 5a | Theme adoption: calf registration | 2 | 1 | | | | Not started |
| 5b | Theme adoption: farm traceability | 2 | 1 | Kurtleigh | `beeftech-ui-redesign` | #102 | Partly done |
| 5c | Theme adoption: feed crib | 2 | 1 | Kurtleigh | `beeftech-ui-redesign` | #102 | Partly done |
| 5d | Theme adoption: farmer registration, login, tag scanner | 2 | 1 | Kurtleigh (farmer reg) | `beeftech-ui-redesign` | #102 | Partly done |
| 5e | Theme adoption: management screens | 2 | 1 | Kurtleigh | `beeftech-ui-redesign` | #102 | Partly done |
| 6 | Calf registration in four steps | 3 | 1, 5a, D2 | | | | Not started |
| 7 | Save message with Undo | 3 | 6, D3 | | | | Not started |
| 8 | Errors in words and inline validation | 3 | 1 | | | | Not started |
| 9 | Remember last choices | 3 | 6 | | | | Not started |
| 10 | Roll the patterns out to other capture flows | 4 | 6, 7, 8 | | | | Not started |
| 11 | Accessibility and field test pass | 4 | 3–9 | | | | Not started |
| 12 | Clean-up after PR #102 (mock data, contrast, type sizes) | 2 | — | | | | Not started |

## Design language (as built in PR #102)

PR #102 did not use the proposal's dark green (`#1F4D3A`). It took the **sage and cream
palette that calf registration already used** (`CalfRegistrationStyle.kt`) and made it the
app-wide brand. Calf registration was the screen the team considered finished, so the rest
of the app now looks like it. Treat these as the design rules from now on (D6–D9).

**Palette** (`demoapp/.../ui/theme/Color.kt`; the same values are copied into
`farmer-registration/.../ui/theme/Color.kt`, `TraceabilityStyle.kt` and `FeedCribStyle.kt`):

| Token | Value | Use |
|---|---|---|
| `BeefPrimary` | `#4F6256` | Header band, primary buttons |
| `BeefPrimaryStrong` | `#35473D` | Selected nav item, icon tint on soft tiles |
| `BeefAccent` | `#667A6C` | Secondary, chevrons |
| `BeefBackground` | `#FAF9F2` | Screen background (cream) |
| `BeefSurface` | `#FFFFFF` | Cards, bottom bar |
| `BeefSoftSurface` | `#F4F3E8` | Welcome card, `surfaceVariant` |
| `BeefSoftGreen` | `#E3E8E2` | Icon tiles, nav indicator, "Synced" chip |
| `BeefText` / `BeefMutedText` | `#1F2823` / `#6D756F` | Body / secondary text |
| `BeefBorder` | `#D9DDD8` | 1dp card borders, `outline` |
| `BeefSuccess` / `BeefWarning` / `BeefDanger` / `BeefOffline` | `#3F6A50` / `#7A5A20` / `#8A4F4F` / `#3A433D` | Status foregrounds |

Status chip backgrounds keep the proposal's values: waiting `#FCE8C3`, failed `#F9DAD7`,
offline `#E3E6E4` (synced uses `BeefSoftGreen`).

**Patterns:**

- **Header band.** Sage `BeefPrimary` bar: an overline "BEEFTECH · SCREEN TITLE", then
  "username · site", with an Online/Offline chip and the sync chip stacked on the right
  (`BeefAppHeader` in `demoapp/.../AppChrome.kt`).
- **Cards, not lists.** Every entry point is a white card with a 1dp `BeefBorder` border,
  16dp radius (18dp for hero cards), 16dp padding. On the left is a 42–44dp `BeefSoftGreen`
  tile with a 22dp outlined icon in `BeefPrimaryStrong`, then a bold title and a muted
  one-line subtitle, and a "›" chevron on the right when the card navigates.
- **Section headings with a reason.** Each group starts with a bold title and a muted line
  that says what it is for ("Sync: Know what is safely stored and what still needs the
  server").
- **Plain-language status.** Sync states read "Synced", "Waiting to sync", "Sync paused",
  "Needs attention", always with an icon *and* a word, never colour alone.
- **Status pills.** Fully rounded pills with a pale background and a dark foreground of the
  same hue for record states (At site, Moved, Sold, Deceased, Done/In progress).
- **Snackbars, not toasts.** Every `Toast` in `MainActivity.kt` became a message on the one
  app-wide `SnackbarHost` in the `Scaffold`.
- **Outlined Material icons** everywhere (`material-icons-extended` is now a dependency of
  `:demoapp` and `:android:management`).
- **Shapes:** `small` 12dp, `medium` 16dp, `large` 18dp (`BeefTechShapes`). Pills use
  `CircleShape`/999dp.
- **One light theme + sunlight.** Dynamic colour and dark mode are gone from
  `BeeftechTheme`; sunlight mode still overrides colours and type.

**Where PR #102 departs from the plan** (each is either accepted in the Decision log or
listed as WP12 clean-up):

- The palette above replaces the proposal's tokens in WP1 (D6).
- Dashboard, Reports, Team and Feed crib use extra accent colours for icons (blue
  `#2E6DA4`/`#2F6FAE`, orange `#E58E2A`, red `#E35B62`, teal `#2B6F68`, green `#1E805A`) to
  tell metric categories apart. See D9.
- Body text is below the 16sp floor: `bodyMedium` is 15sp, `bodySmall` 13sp and
  `labelSmall` 11sp, and card subtitles use `bodySmall` (WP12).
- The header overline is white at 78% alpha on sage, about 4.1:1, just under 4.5:1 (WP12).

## What the code does today (the gap)

Updated for PR #102. ✅ = done in #102, ◐ = partly done, ✗ = not started.

| Proposal | Today in code |
|---|---|
| Bottom navigation, four items + More | ✅ `BeefBottomNavigation` (`AppChrome.kt`) shows Home · Calves · Traceability · Feed · More to every role. `tabsFor()` returns those five, and `moreTabsFor(role)` returns the manager/admin screens. My activity and Log out are on the More screen; the logout confirm dialog is kept. `BackHandler` closes My activity or a More destination. |
| One brand colour, no purple | ◐ Purple and dynamic colour are gone, and `BeeftechTheme` is the sage scheme (D6). But the theme still lives in `demoapp`, `farmer-registration` keeps its own copy, and each feature module keeps its own `*Style.kt` with copied hex values. About 24 non-test files still hard-code `Color(0x…)`; the biggest are `TagIdentityScreen.kt` (18), `RecordsReviewScreen.kt` (17), `EarTagScannerScreen.kt` (15) and `AnimalRecordScreen.kt` (13). |
| Sync chip in every header | ◐ `BeefAppHeader` shows the Online/Offline chip and `SyncStatusChip` on every screen. `rememberIsOnline()` (a composable `ConnectivityManager` callback in `AppChrome.kt`) provides connectivity. `PendingSyncRepository.observeFailedCount()` (new DAO query `observeRetryLimitCountForUser`, `retryCount >= 3`) provides "failed". `appSyncUiState()` combines them, tested in `AppSyncUiStateTest`. Missing: tapping the chip does not retry yet, and "last manual retry failed" is not tracked (D4). |
| Calf registration in 4 steps with a review | ✗ Unchanged: two screens, form state in the composable. |
| Undo instead of a pop-up | ✗ Unchanged: the `AlertDialog` in `CalfRegistrationFlow.kt` is still there. Snackbar infrastructure now exists (the app-wide `SnackbarHost`), so WP7 only has to use it. |
| Errors in words | ✗ Unchanged: `TagIdentityScreen.kt` (~lines 287, 309) still shows `Icons.Outlined.Warning`. |
| Remember last tag colour and type | ✗ Unchanged. |
| Sunlight mode | ✅ Kept in the rewritten `Theme.kt`/`Type.kt` (sunlight body text 17–18sp). |

## WP0 — Decisions (settled 2026-10-06, extended 2026-10-07)

All decisions are recorded in the Decision log. The text below explains each choice.

- **D1. What are the bottom items?** The mockup shows Home · Calves · Feed · Reports ·
  More. But workers have no Reports today, and Farm Traceability (movements, treatments,
  mortalities, costs and so on) isn't in the mockup. **Decided: Home · Calves ·
  Traceability · Feed · More** for everyone. Managers and admins get Dashboard, Reports,
  Records, Team and Admin under More, plus Dashboard and Reports cards on Home (amended
  2026-10-07 to match #102). My activity and Log out move to More.
- **D2. What goes in each of the four steps?** The mockup's review screen shows Ear tag,
  Type and gender, Age and condition, and Photo. **Decided:** (1) Ear tag; (2) Calf details:
  type, gender, hide colour, conformity, mark; (3) Age, condition, parentage (dam/sire)
  and photo; (4) Check and save.
- **D3. What does Undo mean once the save has already synced?** Options: (a) hold the
  immediate sync until the snackbar closes (~5–10 s) so Undo is only a local delete of a
  `PENDING` row. (b) Undo always works, and voids the record on the server if it synced.
  (c) Only offer Undo while the row is still `PENDING`. **Decided: (a), with an ~8 s hold.** It's the
  simplest and never touches the server. It needs a short delay in the calf
  save → sync call.
- **D4. When does "Sync failed" show?** **Decided:** when any of the user's `pending_sync`
  rows has reached the retry limit (`retryCount >= 3`, the same `maxRetries` that
  `SyncRepository.getPendingForRetry` uses), or when the last manual retry failed.
  "Tap to retry" runs the immediate batch sync (`ScheduledBatchSyncWorker` path).
  **Amended 2026-10-07:** when the phone is offline, the chip shows "Sync paused" even if
  rows are at the retry limit, because being offline is the reason they can't sync. The
  chip label for this state is "Needs attention", not "Sync failed".
- **D5. Dark mode.** The proposal mockups use fixed light app colours. Do we ship dark
  mode, or light + sunlight only? **Decided:** light + sunlight now, and leave dark mode
  as a follow-up. WP1 still defines colours as tokens so dark mode can be added later.
- **D6. Brand palette.** **Decided 2026-10-07:** the sage/cream palette from PR #102 (see
  Design language) replaces the proposal's `#1F4D3A` tokens. It matches calf registration,
  which users have already seen, and it passes 4.5:1 for all four status chips.
- **D7. Snackbars replace toasts app-wide.** **Decided 2026-10-07:** there is one
  `SnackbarHost` in the `MainActivity` `Scaffold`, not one on Home. Feature modules report
  results through callbacks; they don't show `Toast`s or their own snackbars.
- **D8. Connectivity lives in the UI layer for now.** **Decided 2026-10-07:**
  `rememberIsOnline()` stays a composable in `:demoapp` and moves to
  `:android:design-system` with WP1. A `Flow` in `:android:database` is only needed if a
  worker or ViewModel has to know about connectivity.
- **D9. Accent colours on management screens.** *Open.* #102 gives Dashboard and Reports
  categories their own icon colours (blue, orange, red, teal, green). That helps managers
  scan the screen, but it goes against "one brand colour". Options: (a) keep them as a
  named `CategoryAccent` token set in WP1, management screens only; (b) replace them with
  sage tiles like the rest of the app. Decide before WP5e is finished.

## Work packages

### WP1 — Shared design-system module and theme

**Done in #102:** the purple template colours are deleted, dynamic colour is off,
`BeeftechTheme` maps the sage palette onto a full `lightColorScheme`, `BeefTechShapes` is
set, and `Typography`/`SunlightTypography` are rewritten. Components that exist (in
`demoapp/.../AppChrome.kt`, not yet shared): `BeefAppHeader`, `NetworkStatusChip`,
`SyncStatusChip`, `BeefBottomNavigation`, plus private `HomeActionCard`,
`MoreActionCard` and `HomeSectionTitle`.

**Left to do:**

- Add a new module `:android:design-system` (`include` it in `settings.gradle.kts`).
  `demoapp` and every feature module depend on it.
- Move `BeeftechTheme`, the `Beef*` colour tokens, `BeefTechShapes`, `Typography` and
  `SunlightTypography` into it. Delete `farmer-registration/.../ui/theme/` and use the
  shared one. Delete the copied hex values in `TraceabilityStyle.kt`, `FeedCribStyle.kt`
  and `CalfRegistrationStyle.kt` (do this in the WP5 PRs).
- Tokens: the D6 palette in the Design language table. Add the status chip backgrounds
  (`#FCE8C3`, `#F9DAD7`, `#E3E6E4`, `#DFF3E5`) as named tokens instead of private `val`s
  in `AppChrome.kt`. If D9 goes with (a), add `CategoryAccent` here too.
- Move the existing components in, and make the private card helpers public:
  `BigActionCard` (= `HomeActionCard`), `NavigationCard` (= `MoreActionCard`, with a
  `danger` variant), `SectionHeader` (= `HomeSectionTitle`), `StatusPill`, `SyncStatusChip`,
  `NetworkStatusChip`.
- Still to build: `BeefPrimaryButton` / `BeefSecondaryButton`
  (`Modifier.defaultMinSize(minHeight = 56.dp)`, 14dp radius as on Home), `StepProgress`
  (start from `MovementStepIndicator` in `AnimalMovementScreen.kt`), `ReviewRow` (label,
  value, Change), `BottomActionDock` (uses `imePadding()` so it stays above the keyboard),
  `InlineMessage` (ok and error).
- **Done when:** the app builds with the new module; no `Purple*`/`Pink*` colours are left
  (✅ already true); a Compose preview shows each component; text/background contrast in
  each status chip is at least 4.5:1 (checked 2026-10-07: synced 5.0, waiting 5.3, offline
  8.2, failed 4.8, online 5.3); no feature module defines its own copy of the palette.

### WP2 — App-wide sync status source

**Done in #102:** `rememberIsOnline()` (connectivity, D8);
`PendingSyncDao.observeRetryLimitCountForUser` and
`PendingSyncRepository.observeFailedCount(userId, retryLimit = DEFAULT_MAX_RETRIES)`;
`appSyncUiState(isOnline, pendingCount, failedCount)` with the states `SYNCED`, `WAITING`,
`OFFLINE`, `FAILED` and a label and detail line for each; `AppSyncUiStateTest` covers the
four states. No Room migration (query only). ✅

**Left to do:**

- `retry()`: tapping the header chip (or the Home sync card when it shows "Needs
  attention") triggers the immediate batch sync. Today the Home card opens My activity.
- Track "last manual retry failed" (D4), e.g. a flag held in memory in the sync tracker
  and cleared on the next success.
- Move `appSyncUiState`/`SyncTone` out of `AppChrome.kt` into a `SyncStatusTracker` that
  exposes `Flow<AppSyncUiState>` so it can be tested without Compose.
- Run `./gradlew :android:database:connectedAndroidTest`: #102 added a DAO query in
  `:android:database`, and CLAUDE.md requires the instrumented tests for any change there.
  Add an instrumented test for `observeRetryLimitCountForUser` (rows above and below the
  limit, other users' rows ignored).
- **Done when:** JVM unit tests cover each state and the changes between them (including
  offline → online with rows at the retry limit); the DAO query has an instrumented test.

### WP3 — Bottom navigation, header chip and More

**Done in #102:** the five-item `NavigationBar`; `BeefAppHeader` with title, username,
site and both chips; the `BeefMoreScreen` with My activity, role-gated destinations and Log
out (confirm dialog kept); `tabsFor`/`moreTabsFor`; `AppTabTest` updated; no scrolling
tab bar left; `BackHandler` from More destinations and My activity. ✅

**Left to do:**

- The bottom bar shows the More item as selected while a More destination (e.g.
  Dashboard) is open. Check that this is what we want, and that pressing More again returns
  to the More list (today it does, because `onSelect` clears `selectedMoreTab`).
- Navigation is still `selectedDemoTab`/`selectedMoreTab`/`showMyActivity` state in
  `MainActivity`, and it is lost on rotation. Move it to `rememberSaveable` (or Navigation
  Compose) before WP6 adds more screens.
- Give each `NavigationBarItem` and chip a `contentDescription` that includes the state
  (e.g. "Sync: Waiting to sync, 3 records queued").
- **Done when:** a worker sees 4 items + More; a manager or admin reaches every screen
  they reach today; the role tests pass; navigation survives a rotation.

### WP4 — Home screen

**Done in #102:** a welcome card ("Welcome back, <user>", site) with a full-width 56dp
"Register calf" button; Quick actions cards for Traceability and Feed; Farm management
cards for Dashboard and Reports (managers/admins); a Sync card with the chip, a progress
bar and "Tap to review pending activity". The `SnackbarHost` is app-wide (D7). ✅

**Left to do:**

- Real counts: "N registered today" on the Register calf card, and the pen in progress on
  the Feed card (feed crib is still in-memory data, so this may need to wait).
- `SyncProgressVisual` works out a percentage from how far `pendingCount` has dropped since
  the batch started. It's an estimate, not real upload progress. Either label it as
  "N of M sent" or drop the percentage.
- `HomeActionCard` has a 132dp minimum height, which is fine; check that every other target
  on Home is at least 56dp (the Sync card is).
- **Done when:** the counts come from real data; the cards open the right flows; every
  target is at least 56dp.

### WP5a–e — Theme adoption, one module per PR

Follow the Design language section: sage header and buttons, white bordered cards with
soft-green icon tiles, section headings with a reason line, status pills, outlined icons.

- Replace the module's own palette (`CalfRegistrationStyle.kt`, `TraceabilityStyle.kt`,
  `FeedCribStyle.kt`, colours in `LoginScreen.kt`, `EarTagScannerScreen.kt`,
  `ClientDetailsScreen.kt`, …) with `MaterialTheme` tokens and the WP1 components. The ear
  tag colours (blue, red, green, yellow) are domain colours and stay, but each must show
  its name in text too.
- Make each module's buttons and tappable rows at least 56dp, with 8dp gaps between them.
- No change to behaviour or data; these PRs are visual only.
- **Done when:** the module has no `Color(0x…)` outside the tag colours; screenshots
  before and after are in the PR.

Status per module after #102:

- **5a calf registration:** not touched. It is already the reference look (D6), so this is
  mostly swapping `Beeftech*` constants for theme tokens.
- **5b farm traceability:** `TraceabilityStyle.kt` is re-pointed at the D6 values.
  `AnimalMovementScreen` (movement-type cards with icons, a step indicator) and
  `AnimalRecordScreen` (profile card, info tiles, status pills, record link cards) are
  rebuilt in the new style. Still hard-coded: pill colours in `AnimalRecordScreen` (13) and
  `AnimalMovementScreen` (3), and `RegisteredFarmersScreen` (8). Other traceability screens
  only had small changes.
- **5c feed crib:** `FeedCribStyle.kt` is re-pointed at sage. Note that the names now lie
  (`Rust` = sage `#4F6256`, `Olive` = `#667A6C`), so rename them or delete the file.
  `FeedCribListScreen` is rebuilt with a search field, crib cards and Done/In progress pills.
- **5d farmer registration, login, tag scanner:** only farmer registration's theme copy is
  changed (it now uses the D6 palette, not purple). `ClientDetailsScreen` (10),
  `LoginScreen` (10) and `EarTagScannerScreen` (15) still hard-code colours.
- **5e management:** Dashboard (welcome card, metric cards), Reports (menu cards), Records
  review (search, filters, status pills, "Show voided"), and Team are restyled. Waiting on
  D9 for the accent colours. `RecordsReviewScreen` has its own private palette
  (`RecordsSageStrong` and others) and mock data; see WP12.

### WP6 — Calf registration in four steps

- Move form state from the composable into `CalfRegistrationViewModel`
  (`StateFlow<CalfFormState>`, kept with `SavedStateHandle` so a rotation keeps it).
- Split `TagIdentityScreen` / `AppearanceParentageScreen` into the steps from D2, plus a
  new `CalfReviewScreen` built from `ReviewRow`s; each Change jumps back to its step.
  Show `StepProgress` and "Step n of 4: …" in the header. Use the movement screen's step
  indicator as the starting point so both flows look the same.
- Every step has one `BottomActionDock` "Next: …" button; the last step's button is
  "Save calf". The review screen says "Saves on this phone. Sends automatically when
  there is signal."
- **Done when:** a calf can be registered offline and online; the duplicate tag check
  still blocks a save; JVM tests cover the ViewModel's step moves and validation.

### WP7 — Save message with Undo

- Remove the `AlertDialog` in `CalfRegistrationFlow.kt`. A successful save goes back to
  Home and shows the snackbar "Calf Red0000014 saved", with the action "Undo". Use the
  app-wide `SnackbarHost` from #102 (D7). It will need an action-capable variant of
  `showUiMessage` that returns the `SnackbarResult`.
- Implement Undo according to D3. A failed save keeps the worker on the review step with
  an inline error (no dialog).
- Use the same words everywhere: the button says "Save calf" and the message says
  "Calf … saved".
- **Done when:** Undo removes the record and its `pending_sync` row; there is a test for
  undo-before-sync; no dialog has to be acknowledged after a save.

### WP8 — Errors in words and inline validation

- Replace the `Icons.Outlined.Warning` triangles in `TagIdentityScreen.kt` with
  `InlineMessage` text (e.g. "Enter the tag number", "Red0000014 is already
  registered") and a matching `isError` state on the field.
- Validate as the user types (tag free or taken, required fields), not only on Next.
- Word errors in the same plain tone as the #102 sync labels: say what happened and what
  to do, no codes.
- **Done when:** every error on the calf flow is a sentence that a screen reader reads out.

### WP9 — Remember last choices

- Save the last tag colour and animal type for each user (in DataStore or the existing
  prefs), and start a new calf with those values.
- Put "Scan ear tag" before typing on step 1.
- **Done when:** the second calf in a session starts with the first calf's tag colour
  and type.

### WP10 — Roll the patterns out to other capture flows

- Apply steps / review / snackbar / inline errors to the traceability forms (movement,
  treatment, mortality, cost, supplier purchase, location and feed), the feed crib
  reading and farmer registration. Split this into one PR per flow and add a row for
  each to the tracker when someone picks it up.
- The movement form already has a step indicator and card-style type picker from #102;
  bring it onto the shared `StepProgress`/`ReviewRow` components rather than redoing it.
- Traceability results already go through the app-wide snackbar (#102). Remove any
  remaining result dialogs in these flows.

### WP11 — Accessibility and field test pass

- Run the app with system font size at maximum, with TalkBack on, and with a colour-blind
  simulation (Developer options). Check in particular the five-label bottom bar at max
  font size, and that the stacked header chips don't push the title off screen.
- Test outdoors on a real device at full brightness, wearing gloves if possible. Record
  the results and the device in a new `docs/testing/` note.
- Add Compose UI tests for the calf flow (`ui-test-junit4` is already in
  `libs.versions.toml`).
- **Done when:** the findings are recorded and every blocking issue has been fixed or
  added to the tracker.

### WP12 — Clean-up after PR #102

Small fixes that #102 left behind. These can go in one PR and don't depend on anything.

- **Mock data in a real screen.** `RecordsReviewScreen.kt` has an "Animals" filter backed by
  a hard-coded `mockupAnimals` list (`MockAnimal`, five animals) and a hard-coded
  "156 animals" heading. Managers would see invented animals. Either wire it to real data
  or hide the Animals filter until there is a source. This has priority over everything
  else in WP12.
- **Type sizes.** Raise `bodyMedium` to 16sp, and stop using `bodySmall` (13sp) and
  `labelSmall` (11sp) for anything a worker needs to read (card subtitles, chip labels,
  nav labels), or raise them. The plan's floor is 16sp for body text.
- **Header overline contrast.** "BEEFTECH · TITLE" is white at 78% alpha on `#4F6256`
  (about 4.1:1). Use full white or a larger size.
- **Chevrons.** The "›" text chevrons should be an `Icon` (`Icons.AutoMirrored.Outlined.
  KeyboardArrowRight`) so they mirror in RTL and are hidden from screen readers.
- **Deprecated API.** `LinearProgressIndicator(progress = Float)` in `SyncProgressVisual` is
  deprecated; use the lambda overload.
- **Done when:** no mock data is reachable from the app; the type and contrast fixes are
  in; the build has no new deprecation warnings from `AppChrome.kt`.

## Rules that apply to every package

- Don't change sync semantics (`PENDING → PROCESSING → SYNCED`, `pending_sync.user_id`,
  upsert by GUID) as part of UI work. The only planned exception is D3 (a short delay on
  calf sync), and it needs its own tests.
- No Room migration is expected for this redesign. If one turns out to be needed, flag
  it in the Decision log first. New read-only queries (like `observeRetryLimitCountForUser`)
  don't need a migration, but they still need `connectedAndroidTest`.
- Follow the Design language section. Don't add a new palette file to a feature module;
  if a colour is missing, add a token to the theme (WP1) instead.
- Don't ship mock or placeholder data in a screen that is reachable from the app.
- Don't commit backup files (`*.bak`, `*.before-*`). Git history is the backup.
- Build and test with `./gradlew` from the repo root (JDK 17) before you mark a PR ready.
  Attach screenshots for any visible change.

## Decision log

| Date | Who | Decision |
|---|---|---|
| 2026-10-06 | Darian | Plan created from the design proposal. |
| 2026-10-06 | Darian | D1: the bottom bar is Home · Calves · Traceability · Feed · More for every role. The manager and admin screens go under More, and managers also get a Reports card on Home. |
| 2026-10-06 | Darian | D2: the steps are Ear tag / Calf details (type, gender, hide colour, conformity, mark) / Age, condition, parents and photo / Check and save. |
| 2026-10-06 | Darian | D3: after a save, the calf's immediate sync waits about 8 s, until the Undo snackbar closes. Undo deletes the record and its `pending_sync` row, both still on the phone. No backend change. |
| 2026-10-06 | Darian | D4: "Sync failed" shows when any of the user's `pending_sync` rows has `retryCount >= 3`, or the last manual retry failed. Tapping the chip runs the immediate batch sync. |
| 2026-10-06 | Darian | D5: ship light + sunlight only. Dark mode is a follow-up. |
| 2026-10-07 | Darian | Plan updated for PR #102 (`beeftech-ui-redesign`, Kurtleigh): WP1–4 and 5b–5e marked partly done, gap table and work packages updated, Design language section and WP12 added. Stray `.bak` files removed from the branch in `6881125`. |
| 2026-10-07 | Darian | D1 amended: managers and admins get Dashboard and Reports cards on Home (as built in #102). |
| 2026-10-07 | Darian | D4 amended: offline takes priority over the retry limit ("Sync paused"), and the failed label is "Needs attention" (as built in #102). |
| 2026-10-07 | Darian | D6: the sage/cream palette from #102 (taken from calf registration) is the brand. It replaces the proposal's `#1F4D3A` tokens. |
| 2026-10-07 | Darian | D7: one app-wide `SnackbarHost` in `MainActivity`; no `Toast`s in the app. |
| 2026-10-07 | Darian | D8: connectivity stays a composable (`rememberIsOnline`) and moves to `:android:design-system` with WP1. |
| 2026-10-07 | Darian | D9 opened: whether management screens keep per-category accent colours. |
