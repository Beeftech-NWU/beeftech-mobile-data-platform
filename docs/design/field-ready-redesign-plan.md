# Field-ready redesign: implementation plan and tracker

Source: "BeefTech Mobile: Design Proposal" (CMPG 323), kept in the team notes repo at
`beeftech-agent-notes/design-proposal/BeefTech Mobile_ Design Proposal.html`.
Code references were checked against `main` @ `78550dc` on 2026-10-06. All paths are
repo-relative.

The proposal keeps every feature and changes how fast and safely a worker gets through
them: bottom navigation, one theme, a sync chip in every header, calf registration in
four short steps, Undo instead of pop-ups, and 56dp targets for gloves and sunlight.

## How to use this file

- **Claim a work package** by putting your name in the tracker's Owner column and setting
  Status to `In progress` in a small PR or a direct commit to your branch. Check that
  nobody else has claimed it first. One owner per package. Ask in the group before you
  take over someone else's.
- **One branch and one PR per package** where possible, named in the Branch column (e.g.
  `feature/ui-design-system`). Put the PR number in the tracker when you open it.
- **Status values:** `Not started` · `Blocked (by WPn)` · `In progress` · `In review` ·
  `Done`. Set `Done` only once the PR is merged and the package's "Done when" list holds.
- **Change the plan here, not in chat.** If you change scope, add a line to the
  Decision log at the bottom with the date and your name.
- Packages that share a "Wave" can be worked on in parallel.

## Tracker

| WP | Work package | Wave | Depends on | Owner | Branch | PR | Status |
|---|---|---|---|---|---|---|---|
| 0 | Settle the open decisions | 0 | — | Darian | — | — | Done |
| 1 | Shared design-system module and theme | 1 | — | | | | Not started |
| 2 | App-wide sync status source | 1 | — | | | | Not started |
| 3 | Bottom navigation, header chip and More | 2 | 1, 2, D1 | | | | Not started |
| 4 | Home screen | 2 | 1, 2, 3 | | | | Not started |
| 5a | Theme adoption: calf registration | 2 | 1 | | | | Not started |
| 5b | Theme adoption: farm traceability | 2 | 1 | | | | Not started |
| 5c | Theme adoption: feed crib | 2 | 1 | | | | Not started |
| 5d | Theme adoption: farmer registration, login, tag scanner | 2 | 1 | | | | Not started |
| 5e | Theme adoption: management screens | 2 | 1 | | | | Not started |
| 6 | Calf registration in four steps | 3 | 1, 5a, D2 | | | | Not started |
| 7 | Save message with Undo | 3 | 6, D3 | | | | Not started |
| 8 | Errors in words and inline validation | 3 | 1 | | | | Not started |
| 9 | Remember last choices | 3 | 6 | | | | Not started |
| 10 | Roll the patterns out to other capture flows | 4 | 6, 7, 8 | | | | Not started |
| 11 | Accessibility and field test pass | 4 | 3–9 | | | | Not started |

## What the code does today (the gap)

| Proposal | Today in code |
|---|---|
| Bottom navigation, four items + More | `demoapp/.../MainActivity.kt` (~line 580–685) builds a top `PrimaryTabRow`, or `PrimaryScrollableTabRow` when there are more than 3 tabs. `AppTab.kt` gives workers 3 tabs and managers/admins up to 8. "My activity" and "Log out" are text buttons in the top bar. |
| One brand colour, no purple | `demoapp/.../ui/theme/Color.kt` is the template purple, and `Theme.kt` uses dynamic colour by default. `farmer-registration` has its own copy of the theme. Each feature module has its own palette: `CalfRegistrationStyle.kt` (sage), `FeedCribStyle.kt` (rust/cream), `TraceabilityStyle.kt`. Ten files hard-code `Color(0x…)`. Feature modules can't see `demoapp`'s theme. |
| Sync chip in every header | Sync status only shows inside Farm Traceability (`FarmTraceabilityScreen.kt`, through `SyncStatusViewModel`). `PendingSyncRepository.observePendingCount(userId)` already exists and `MainActivity` collects it, but only for My activity. **There is no connectivity observer**, so "Offline" can't be shown yet. There is no "failed" state: a failed row goes back to `PENDING` and its `pending_sync.retryCount` goes up. |
| Calf registration in 4 steps with a review | `CalfRegistrationFlow.kt` has two capture screens (`TagIdentityScreen`, `AppearanceParentageScreen`) and saves from the second. Form state is a `remember { mutableStateOf(CalfRegistrationData()) }` in the composable, so a rotation or process death loses it. |
| Undo instead of a pop-up | After a save, `CalfRegistrationFlow.kt` shows an `AlertDialog` that can only be closed with OK ("Calf Registered Successfully"). `repository.saveCalf()` **tries to sync straight away**, so the record may already be on the server when Undo is pressed (see D3). |
| Errors in words | `TagIdentityScreen.kt` (~lines 287, 309) shows `Icons.Outlined.Warning`. |
| Remember last tag colour and type | `TagIdentityScreen.kt:53` resets `selectedColour` to `BLUE` every time. `CalfRegistrationData` defaults to "BRN — Brangus". |
| Sunlight mode | Already exists (`SunlightUiState` in `android/database/.../runtime`, `SunlightColorScheme` and `SunlightTypography`). Keep it and move it into the new theme. |

## WP0 — Decisions (settled 2026-10-06)

All five are decided; see the Decision log. The text below explains each choice.

- **D1. What are the bottom items?** The mockup shows Home · Calves · Feed · Reports ·
  More. But workers have no Reports today, and Farm Traceability (movements, treatments,
  mortalities, costs and so on) isn't in the mockup. **Decided: Home · Calves ·
  Traceability · Feed · More** for everyone. Managers and admins get Dashboard, Reports,
  Records, Team and Admin under More, plus a Reports card on Home. My activity and Log
  out move to More.
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
- **D5. Dark mode.** The proposal mockups use fixed light app colours. Do we ship dark
  mode, or light + sunlight only? **Decided:** light + sunlight now, and leave dark mode
  as a follow-up. WP1 still defines colours as tokens so dark mode can be added later.

## Work packages

### WP1 — Shared design-system module and theme

- Add a new module `:android:design-system` (`include` it in `settings.gradle.kts`).
  `demoapp` and every feature module depend on it.
- Move `BeeftechTheme`, `SunlightColorScheme` and `SunlightTypography` in from
  `demoapp/.../ui/theme/`. Delete the purple template colours and turn off dynamic colour.
  Delete `farmer-registration/.../ui/theme/` and use the shared one.
- Tokens, taken from the proposal: primary `#1F4D3A`, ink `#17211B`, muted `#56625B`,
  background `#F7F8F5`, line `#D9DED6`. Status colours: ok `#DDEFE4`/`#17402D`, waiting
  `#FCE8C3`/`#6B3F00`, offline `#E3E6E4`/`#3A433D`, failed `#F9DAD7`/`#8C1D18`. Body text
  is at least 16sp, spacing is a multiple of 8dp, and shapes are 12/16/18dp radii.
- Components: `BeefPrimaryButton` / `BeefSecondaryButton`
  (`Modifier.defaultMinSize(minHeight = 56.dp)`), `BigActionCard`, `SyncStatusChip`
  (four states, each with text and colour), `StepProgress`, `ReviewRow` (label, value,
  Change), `BottomActionDock` (uses `imePadding()` so it stays above the keyboard),
  `InlineMessage` (ok and error).
- **Done when:** the app builds with the new module; no `Purple*`/`Pink*` colours are
  left; a Compose preview shows each component; text/background contrast in each status
  chip is at least 4.5:1 (note the checked ratios in the PR).

### WP2 — App-wide sync status source

- Add a connectivity observer (a `ConnectivityManager.NetworkCallback` wrapped in a
  `Flow<Boolean>`) in `:android:database` (or `:android:design-system` if it must stay
  UI-free; decide in the PR).
- Add `SyncStatusTracker.observe(userId): Flow<AppSyncState>` with the states
  `AllSynced`, `Waiting(count)`, `Offline(count)` and `Failed(count)`. Build it by
  combining `PendingSyncRepository.observePendingCount(userId)`, a new
  "rows at retry limit" count query (D4), and connectivity.
- `retry()` triggers the immediate batch sync.
- **Done when:** JVM unit tests cover each state and every change between them; there is
  no Room migration (the count is a query, not a column).

### WP3 — Bottom navigation, header chip and More

- Replace the tab rows in `MainActivity.kt` with `Scaffold(bottomBar = NavigationBar …)`,
  with the items chosen in D1. The header shows the screen title, the site name and the
  `SyncStatusChip` from WP2.
- Change `AppTab.kt` / `tabsFor(role)` to return the bottom items and the role-gated More
  entries separately. Update `demoapp/src/test/.../AppTabTest.kt`.
- The More screen lists My activity, the manager/admin screens (by role), and Log out
  (keep the existing confirm dialog, because logging out is destructive).
- **Done when:** a worker sees 4 items + More; a manager or admin reaches every screen
  they reach today; the role tests pass; there is no scrolling tab bar left.

### WP4 — Home screen

- Greeting, the user's site and the sync chip. A primary card "Register calf" ("N
  registered today"), a secondary card "Feed crib reading" (shows the pen in progress if
  there is one), and two small cards (Records / Reports by role, from D1).
- Home is the global `SnackbarHost`, so WP7 can show "Calf … saved · Undo" after
  returning here.
- **Done when:** the counts come from real data; the cards open the right flows; every
  target is at least 56dp.

### WP5a–e — Theme adoption, one module per PR

- Replace the module's own palette (`CalfRegistrationStyle.kt`, `TraceabilityStyle.kt`,
  `FeedCribStyle.kt`, colours in `LoginScreen.kt`, `EarTagScannerScreen.kt`,
  `ClientDetailsScreen.kt`, …) with `MaterialTheme` tokens and the WP1 components. The ear
  tag colours (blue, red, green, yellow) are domain colours and stay, but each must show
  its name in text too.
- Make each module's buttons and tappable rows at least 56dp, with 8dp gaps between them.
- No change to behaviour or data; these PRs are visual only.
- **Done when:** the module has no `Color(0x…)` outside the tag colours; screenshots
  before and after are in the PR.

### WP6 — Calf registration in four steps

- Move form state from the composable into `CalfRegistrationViewModel`
  (`StateFlow<CalfFormState>`, kept with `SavedStateHandle` so a rotation keeps it).
- Split `TagIdentityScreen` / `AppearanceParentageScreen` into the steps from D2, plus a
  new `CalfReviewScreen` built from `ReviewRow`s; each Change jumps back to its step.
  Show `StepProgress` and "Step n of 4: …" in the header.
- Every step has one `BottomActionDock` "Next: …" button; the last step's button is
  "Save calf". The review screen says "Saves on this phone. Sends automatically when
  there is signal."
- **Done when:** a calf can be registered offline and online; the duplicate tag check
  still blocks a save; JVM tests cover the ViewModel's step moves and validation.

### WP7 — Save message with Undo

- Remove the `AlertDialog` in `CalfRegistrationFlow.kt`. A successful save goes back to
  Home and shows the snackbar "Calf Red0000014 saved", with the action "Undo".
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

### WP11 — Accessibility and field test pass

- Run the app with system font size at maximum, with TalkBack on, and with a colour-blind
  simulation (Developer options).
- Test outdoors on a real device at full brightness, wearing gloves if possible. Record
  the results and the device in a new `docs/testing/` note.
- Add Compose UI tests for the calf flow (`ui-test-junit4` is already in
  `libs.versions.toml`).
- **Done when:** the findings are recorded and every blocking issue has been fixed or
  added to the tracker.

## Rules that apply to every package

- Don't change sync semantics (`PENDING → PROCESSING → SYNCED`, `pending_sync.user_id`,
  upsert by GUID) as part of UI work. The only planned exception is D3 (a short delay on
  calf sync), and it needs its own tests.
- No Room migration is expected for this redesign. If one turns out to be needed, flag
  it in the Decision log first.
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
