# Pickle

Pickle is a native Android app that helps people make group decisions through polls. Someone asks a question, adds possible answers, shares the poll, and uses the votes to settle the decision.

**Purpose of this file:** handoff to the Codex agent building the web version. Match the mobile app's product behavior, content, visual identity, and shared data contracts described here. Browser-specific implementation recommendations are explicitly labeled below. This file is self-contained for the main parity requirements; repository paths identify the source of truth when source access is available.

Typical uses include choosing a film, ordering food, finding a date that works for everyone, picking a meeting place, planning game night, or deciding who is driving.

This overview describes the Android code and database definitions in this repository as of October 15, 2026. The companion website and deployed backend have not been verified as part of this document.

## How it works

### 1. Explore or sign in

The welcome screen offers account creation, login, and a **Try it** option.

- **Try it** opens a read-only demo with sample polls and notifications. It does not load real polls from the database.
- **Accounts** use email and password through Supabase Auth. Signup also stores a display name and avatar URL.
- Creating polls, casting votes, and other real data changes require a signed-in account.
- The app observes session changes and clears account-specific cached data when the account changes.

### 2. Find your polls

The home screen shows the signed-in user's own polls, with separate views for open and closed polls. It also provides access to templates, quick starters, notifications, and the account menu.

Open polls are ordered newest first. The main view also includes up to three recently settled polls. In demo mode, home shows sample data instead.

### 3. Create a poll

Start with a blank poll, a built-in starter, or a personal template. Enter a question and between **2 and 10 nonempty, distinct options**. Options can be edited, removed, and reordered; duplicate checks ignore capitalization and surrounding whitespace.

The creator can configure:

| Setting | Behavior |
| --- | --- |
| Single-choice voting | Each voter selects one option. |
| Multiple-choice voting | Each voter can select several options in one ballot. |
| Suggestions | Participants can propose additional options. |
| Suggestion approval | Participant suggestions wait for the creator's approval when enabled. |
| Results before voting | Results can be shown before a participant submits a vote. |
| Vote changes | Participants can replace their submitted ballot while the poll remains open. |
| Closing time | Use no deadline, a preset duration, or a custom closing time. |
| Expected voters | Optionally provide a group size to calculate turnout. |

The default settings use single-choice voting, disable suggestions, hide results before voting, and allow vote changes. Suggestion approval defaults to enabled if suggestions are turned on.

After creation, the app opens the poll's detail screen. Creation uses a request ID so a retry can recover an existing creation instead of allocating another poll or template round.

### 4. Share the poll

The share action opens Android's share chooser with the question and a web link in this format:

```text
https://pickle-web-iota.vercel.app/p/<poll-id>
```

Users can send this text through a messaging app or another share target. The companion web application's source is outside this repository. The Android manifest currently has no incoming poll-link intent filter, so this project does not establish direct opening of those links in the Android app.

### 5. Vote and suggest answers

Signed-in participants select approved options and submit a ballot. Multiple-choice voting stores the complete selected set. If changes are allowed, a new ballot replaces the participant's previous selections.

When suggestions are enabled, participants can propose new answers. With approval enabled, suggestions from other participants remain pending until the creator approves or rejects them. Creator-added suggestions, or suggestions without an approval requirement, become approved immediately.

Pending suggestions are shown to the creator for moderation while the poll is open. Rejected suggestions are excluded from the available voting options.

### 6. Review and settle the result

Results become visible after voting, when the poll allows results before voting, or when the poll is closed. Demo polls also expose results.

The results calculation:

- Ranks approved options by vote count.
- Shows each option's share of participating voters.
- Distinguishes a single winner, a tie, and no votes.
- Tracks the number of voters separately from the number of selections.
- Calculates turnout when an expected voter count is supplied.

In multiple-choice polls, one person can support several options, so the displayed percentages can add up to more than 100%.

Creators can close polls manually. A poll also becomes unavailable for voting once its deadline passes. During a data refresh, the Android repository calls the backend to finalize expired open polls; this code does not establish a background deadline scheduler.

## Templates and repeated decisions

Pickle includes six built-in starters: **Film night**, **What are we eating?**, **Pick a date**, **Where to meet**, **Game night**, and **Who's driving?** Each provides a starting question, options, voting mode, and closing time. The driving starter leaves the names for the user to fill in.

Personal templates save a poll's question, options, and settings with a name and icon. Templates belong to the account that saves them. An optional weekly closing time includes a weekday, local time, and timezone.

Using a saved template opens an editable draft for the next numbered round. Where completed history is available, it carries forward the latest completed round's approved options and settings, and identifies the previous winner, runners-up, and newly introduced options. When there are more than two options, the draft initially excludes the previous winner; the user can include it again.

A weekly schedule supplies the next draft's closing time. It does **not** automatically publish a new poll every week.

## Notifications and account menu

The notifications screen displays poll activity and lets users open the associated poll, mark a notification as read, or mark all notifications as read. The notification model supports poll creation, poll closure, new votes, and suggestion receipt, approval, or rejection. Actual delivery depends on the implemented client actions and backend behavior.

Notifications are an in-app inbox fetched from the backend. This repository does not configure Android push notifications.

The account menu shows profile information and poll statistics, with actions for creating polls, opening notifications, sharing Pickle, and signing out.

## How the app is built

Pickle is a single-module Android project using Kotlin, Jetpack Compose, and Material 3. The application ID is `com.shkurta.pickle`. The current build declares minimum Android API 24 and compile/target API 37.

The app follows an MVVM structure:

```text
Compose screen
    -> ViewModel
    -> Domain use case / repository interface
    -> Repository implementation
    -> Supabase Auth or PostgREST
    -> PostgreSQL tables and functions
```

Repository state flows back to ViewModels and then to the screens through lifecycle-aware state collection. Kotlin coroutines handle asynchronous work, and Hilt supplies dependencies.

| Location | Purpose |
| --- | --- |
| `app/src/main/java/com/shkurta/pickle/ui/` | Screens, ViewModels, navigation, reusable components, and theme. |
| `app/src/main/java/com/shkurta/pickle/domain/` | Poll models, validation, result calculations, repository contracts, and use cases. |
| `app/src/main/java/com/shkurta/pickle/data/` | Authentication, remote repositories, data mapping, account caches, and sample data. |
| `app/src/main/java/com/shkurta/pickle/di/` | Supabase configuration and repository bindings. |
| `supabase_schema.sql` | Database bootstrap for a new Supabase database. |
| `supabase/migrations/` | Versioned database changes for existing deployments. |
| `app/src/test/` | Unit and regression tests. |
| `app/src/androidTest/` | Android instrumentation tests. |

Production dependency bindings use Supabase repositories. In-memory repositories and sample fixtures also exist for development and tests. The app preserves non-sensitive form values and poll selections through `SavedStateHandle`; passwords remain in memory.

See [the architecture notes](docs/architecture.md) for more implementation detail.

## Backend and data flow

Supabase Auth manages accounts and sessions. PostgREST provides database access. The database definitions include:

| Table | Role |
| --- | --- |
| `polls` | Questions, creators, settings, status, deadlines, and template-round metadata. |
| `poll_options` | Answers, moderation status, ordering, and vote totals. |
| `poll_ballots` | Complete ballots for single-choice and multiple-choice voting. |
| `votes` | Legacy single-choice vote records retained for compatibility. |
| `poll_templates` | Account-owned reusable templates and optional weekly closing schedules. |
| `notifications` | Account-specific activity and read state. |
| `poll_creation_requests` | Request tracking for safe creation retries. |

Database functions handle operations such as `create_poll_v2`, `cast_ballot`, `moderate_option`, `close_poll`, `expire_poll`, and `save_poll_template`. The supplied SQL includes row-level security policies and server-side checks.

The home screen's **My polls** filter is a creator filter in the client. The supplied database policy allows authenticated users to read polls generally; the project does not define a private group-membership system for poll access.

Data is loaded through explicit refreshes and refreshes after writes. The configured Supabase client installs Auth and PostgREST, with no Realtime subscription integration. Account-specific caches hold the last loaded state in memory; the app does not implement persistent offline synchronization.

## Web implementation handoff

### Scope and source of truth

Use the implemented Android behavior described above as the parity baseline. Preserve the existing web project and inspect its backend integration before making changes. If both clients are intended to use the same deployment, they must operate on the same poll IDs, account identities, templates, ballots, and database functions. Confirm the web project's configured deployment rather than assuming this Markdown identifies it.

The Android client uses the share route `/p/<poll-id>`. Preserve that route in the web app. Other browser route names are implementation choices; the mobile app uses typed navigation destinations rather than HTTP routes.

Do not infer implemented capabilities from mockup titles. Features such as reopening polls, sudden-death tie resolution, automatic weekly publishing, anonymous live voting, or downloadable result cards are not established by the inspected Android implementation. A tie remains a tie. Any intentional web-only behavior should be documented as a product difference.

### Screens and interactions to match

| Screen | Mobile behavior to preserve |
| --- | --- |
| Welcome | Create account, log in, and Try it; demo uses sample data and requires authentication for real writes. |
| Signup | Four steps in order: Name, Avatar, Email, Password. Back moves to the preceding step. Avatar choices can be shuffled, which clears the selection. |
| Login | Email and password form, loading state, error feedback, and a way to switch to signup. |
| Home | Profile identity, unread notification indicator, My polls / Closed / Templates controls, create action, starter shortcuts, active cards, recently settled cards, and a meaningful empty state. Templates opens its own screen. |
| New poll | Single choice / Multi-choice selector; question; option rows; Add an option; deadline choices; voting settings; optional expected voters; bottom action labeled "Create & get the link". |
| Starter or template draft | Editable seeded question and options, option reordering, settings, closing time, expected voters, and relevant previous-round context. Using a seed never publishes immediately. |
| Poll detail | Question, open/closed state, selectable approved options or results, voter/selection totals, sharing, vote-change state, optional suggestions, and creator controls. |
| End voting confirmation | Confirmation sheet with current results and actions to end voting or keep the poll open. Closing occurs after confirmation. |
| Save template | Name, one of the six starter icons, optional weekly closing weekday/time/timezone, Save and Cancel. Creator-only action; also available for a closed poll. Existing templates can be updated through this flow. |
| Templates | Personal templates first, then a two-column starter grid. Personal rows show name, round count, option count, optional weekly schedule, and Use. Empty copy: "Save one of your polls as a template to start a tradition." |
| Notifications | Activity rows, unread/read state, open associated poll, and mark all read. |
| Account | Display name/avatar, counts, create, notifications, Share Pickle, version where available, and sign out. |

Signup validates a nonblank name, an explicitly selected avatar, an email whose trimmed value contains `@`, and a password of at least six characters. Login requires nonblank email and password; backend authentication still determines validity. Preserve backend errors and requirements, including any email confirmation requirement configured on the deployment.

Avatars use DiceBear notionists SVGs with ten randomly generated eight-character seeds per choice set. The URL pattern is:

```text
https://api.dicebear.com/9.x/notionists/svg?seed=<encoded-seed>&backgroundColor=f3c24a,cde7e0,f6c9b5,d9c7f0,bfe0f5,fbe3c0,e6d5c3,fbdde3&backgroundType=solid
```

Store signup metadata under `display_name` and `avatar_url` so both clients read the same profile fields.

### Exact voting state rules

These formulas capture the mobile ViewModel behavior. `hasVoted` means the user's saved ballot contains at least one option ID; `isDemo` means the sample browsing experience.

```text
isOpen = status == OPEN AND (closed_at is null OR now < closed_at)
isCreator = poll.creator_id == authenticated user ID
canVote = NOT isDemo AND isOpen AND (NOT hasVoted OR allow_vote_change)
isVoting = canVote AND (NOT hasVoted OR isChangingVote)
showResults = isDemo OR hasVoted OR show_results_before_voting OR NOT isOpen
showResultsSection = showResults AND (NOT isVoting OR voterCount > 0)
```

- Single-choice selection replaces the selected option; multiple-choice selection toggles membership in a set of option IDs.
- Disable submission when the selection is empty or identical to the saved ballot.
- Before selection, the action reads "Pick an option". Single-choice labels use "Vote for <option>" or "Change to <option>". Multiple-choice labels use "Submit <n> selections" or "Update <n> selections".
- After voting, show the submitted options and a change action if allowed. Entering change mode keeps the saved selections initially. "Keep my vote" restores the saved ballot and exits change mode.
- Only approved options participate in selection and result calculations. Sort voting options by `position`, then `created_at`, then ID. Sort result rows by descending vote count, retaining option order for equal counts.
- Use `voter_count` as the results denominator. For legacy rows where it is absent, mobile falls back to the sum of approved option vote counts. Show selections separately for multiple-choice polls.
- A unique highest positive count is a winner; equal highest positive counts are a tie; zero voters means no votes. Do not pick an arbitrary winner from a tie.
- Turnout is `voterCount / expectedVoters`, clamped between 0 and 1, and is omitted when the expected count is absent or not positive.
- Creator controls include Save as template; open polls also include sharing/link controls, pending suggestion moderation, and End voting.

The current result-visibility setting controls the mobile presentation. The supplied database policies allow authenticated reads of poll options and totals; do not describe this setting as server-enforced concealment of vote totals.

### Draft and template details

Blank drafts begin with two empty options and no deadline. The standard deadline choices are **No deadline**, **In 1 hour**, **1 day**, **3 days**, and **1 week**. Expected voters accepts digits only, up to four characters. Normalize question/option whitespace on submission and reject duplicate or blank option rows in the form.

Built-in starters use these exact seeds:

| ID / title | Question | Options in order | Voting mode | Initial deadline |
| --- | --- | --- | --- | --- |
| `film` / Film night | Film night: what are we watching? | Arrival; Lady Bird; Spirited Away; The Grand Budapest Hotel | `SINGLE` | Now + 3 hours |
| `pizza` / What are we eating? | Pizza night: what are we ordering? | Margherita; Pepperoni; Vegetarian; Four cheese | `SINGLE` | Now + 1 hour |
| `date` / Pick a date | Which date works for everyone? | Next seven local calendar dates, formatted like `Fri 16 Oct` | `MULTIPLE` | Now + 48 hours |
| `place` / Where to meet | Where should we meet? | At home; Local café; The park | `SINGLE` | Now + 2 hours |
| `game` / Game night | Game night: what are we playing? | Board games; Card games; Video games; Trivia | `MULTIPLE` | 23:59 on the current local day, or the following day if already passed |
| `driver` / Who's driving? | Who's driving? | Two empty rows for names | `SINGLE` | Now + 1 hour |

All starters enable suggestions and retain the other default settings. Starter titles mark the draft as seeded, but built-in starters do not themselves reference a personal template ID.

For personal templates, weekdays use ISO numbering: Monday = 1 through Sunday = 7. Use an IANA timezone such as `Europe/Athens` and local `HH:mm` time. Calculate the next strictly future occurrence in that timezone, including daylight-saving transitions. Store absolute poll deadlines as timestamps with timezone.

The draft title is `<template name> #<rounds_count + 1>`. Choose the latest completed round by descending round number, then closing time; include the source poll when looking up history. Carry over that round's question, settings, and approved options, falling back to the saved template when no completed round is available. Exclude a unique previous winner by default only when more than two options remain available before exclusion. Identify runners-up by the next highest positive count and new option texts by case-insensitive comparison with the preceding completed round. Allocate the actual round number on the server at creation time.

Template names are nonblank and limited to 80 characters. Icon values are `film`, `pizza`, `date`, `place`, `game`, or `driver`. Saving from a poll already attached to a personal template updates that template's metadata; it does not create an unrelated duplicate.

### Backend contract to preserve

Use the existing server operations rather than independently modifying vote totals or template round counters. The relevant RPC argument names are:

```text
create_poll_v2(
  p_question, p_options, p_allow_suggestions, p_require_approval,
  p_show_results_before_voting, p_allow_vote_change, p_closes_at,
  p_expected_voters, p_request_id, p_voting_mode, p_template_id
) -> poll UUID

cast_ballot(p_poll_id, p_option_ids) -> void
moderate_option(p_poll_id, p_option_id, p_approve)
close_poll(p_poll_id)
expire_poll(p_poll_id)

save_poll_template(
  p_poll_id, p_name, p_icon, p_weekday, p_local_time, p_timezone
) -> template UUID
```

Use uppercase database enum strings: `SINGLE` / `MULTIPLE`, `OPEN` / `CLOSED`, and `APPROVED` / `PENDING` / `REJECTED`. Ballots are sets of option IDs, not option text. Read the current user's `poll_ballots` along with legacy `votes`, giving complete ballot records precedence when both exist.

`closed_at` has two uses in the existing model: a future closing deadline on an open poll, or the closing timestamp of a closed poll. A nonnull value alone does not mean the poll is closed.

Keep `p_request_id` stable when retrying the same creation after an uncertain response. Generate a new one when the draft changes. If a write succeeds and its following refresh fails, show "Saved. Couldn't refresh the latest data." with a **Refresh** action. Refresh the data rather than replaying the committed mutation.

The mobile client fetches `polls` with nested `poll_options`, orders polls by descending creation time, reads the current user's ballots, and finalizes expired open polls on refresh. Personal templates are filtered by `owner_id` and sorted by descending `updated_at`. Do not assume that client filtering replaces server authorization.

The mobile suggestion path inserts into `poll_options`, with trimmed text, `poll_id`, moderation status, and `suggested_by` (null for the creator's additions). Reject duplicate non-rejected option text case-insensitively. Check the shared backend's current policies before porting this path. Notification creation is partly client-driven in this implementation; inspect the existing repository and SQL before adding web notifications to avoid duplicates or assuming every event is generated automatically.

### Visual identity

Use **Rubik** throughout: normal body text, medium/semibold labels, bold titles, and extra-bold/black headings. The mobile headline-medium style is 28sp with 32sp line height. Preserve the warm canvas, dark outlines, rounded cards, pill buttons, and playful heavy headings.

| Token | Light | Dark |
| --- | --- | --- |
| canvas | `#F6EFE3` | `#1C1412` |
| surface | `#FFFAF3` | `#261D19` |
| sunken | `#EFE2CF` | `#332824` |
| ink | `#33201B` | `#F5EBDD` |
| inkMuted | `#6E5A4E` | `#B8A696` |
| inkInverse | `#FFFAF3` | `#1C1412` |
| lineStrong | `#33201B` | `#6E5A4E` |
| lineSoft | `#E0D3BE` | `#3D302A` |
| tomato | `#E0552D` | `#E9693F` |
| tomatoDeep | `#B8401F` | `#A8401F` |
| mustard | `#F3C24A` | `#F3C24A` |
| teal | `#1F7A6B` | `#258C78` |
| tealSoft | `#CDE7E0` | `#1F3833` |
| textAccent | `#C9442A` | `#F08A66` |
| onBrand | `#FFFFFF` | `#FFFFFF` |
| onMustard | `#33201B` | `#33201B` |

Teal is the primary action/live-state color, tomato highlights leaders, and mustard highlights celebration and templates. Standard cards have a 16dp radius and 1.5dp outlines. Shape scale: 8, 12, 16, 20, and 28dp. Standard buttons are pill-shaped with a minimum 52dp height; circular navigation controls are 40dp with 20dp icons. Common screen gutters are 16–20dp; option rows use 8dp gaps. Translate these dimensions to CSS logical pixels as a starting point, then verify the browser layout visually.

The app follows the system light/dark preference. Browser recommendation: use `prefers-color-scheme`, keep visible focus states and keyboard navigation, and support larger text without clipping. On narrow screens, preserve the mobile content order and accessible bottom actions. On wide screens, adapt the layout while keeping the same actions and information hierarchy. Browser sharing can use Web Share with a copy-link fallback; this fallback is a web adaptation.

If the receiving agent has repository access, current screen captures are under `docs/screenshots/`, with home states, a film draft, and light/dark templates. Reference boards are under `design/`, and reusable vector resources are under `app/src/main/res/drawable/`. Prefer actual captures and implemented screens over unimplemented concepts in the reference boards. A Markdown-only handoff conveys the tokens and behavior, but does not embed those image or icon files.

### Acceptance checks for the web agent

- [ ] Welcome, signup, login, demo, home, create, poll, templates, notifications, and account flows exist and use the mobile identity.
- [ ] Demo browsing cannot write real poll data; authenticated account changes do not expose the previous account's cached state.
- [ ] Blank, duplicate, fewer-than-two, and more-than-ten option drafts cannot be submitted. Valid seeded drafts remain editable before creation.
- [ ] All six starters match their questions, ordered options, voting modes, and deadlines.
- [ ] Single-choice and multiple-choice ballots persist correctly across reloads and, where a shared deployment is available, across mobile and web.
- [ ] Vote replacement honors `allow_vote_change`; unchanged and empty selections cannot be submitted.
- [ ] Result visibility, no-vote, unique-winner, tie, percentages, and turnout match the rules above.
- [ ] Suggestions follow approval settings; only the creator can moderate, close, or save that poll as a personal template.
- [ ] Expired and manually closed polls reject voting; manual closing requires confirmation.
- [ ] Templates carry forward history, preserve option order, handle previous winners, and use timezone-aware weekly closing times without automatic publishing.
- [ ] Retrying creation does not duplicate a poll or consume another round; a committed-write refresh failure does not replay the write.
- [ ] Shared `/p/<poll-id>` URLs resolve directly and survive browser reloads. Verify the web authentication flow for those links explicitly.
- [ ] Loading, empty, invalid-data, request-failure, and saved-but-refresh-failed states are visible and actionable.
- [ ] Light/dark colors, Rubik typography, outlines, cards, buttons, narrow layouts, and larger text are visually checked.

These are implementation acceptance criteria, not claims that the web app currently passes them. Record any existing web-only features or intentional differences in the web project's handoff notes.

## Running the Android project

1. Open the repository in Android Studio with an Android SDK and JDK compatible with the Gradle configuration.
2. Configure the local Android SDK path and these backend properties in the root `local.properties` file:

   ```properties
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-client-anon-key
   ```

3. Ensure the configured Supabase project has the matching schema. Use `supabase_schema.sql` for a new database; existing deployments use the versioned migrations.
4. Build and run the `app` configuration on an emulator or device supporting API 24 or newer.

The Gradle configuration requires both Supabase properties even when building for demo use.

The project's documented verification command on Windows is:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleRelease lintDebug
```

Tests cover domain rules, ballots, authentication transitions, form restoration, poll creation, home filters, template reuse, notification read state, and remote repository behavior. This overview was prepared from source inspection; it does not report a fresh build or test run.
