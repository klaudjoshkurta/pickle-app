# Home, templates, and multi-choice validation

Implemented 15 October 2026. Pickle branding and theme tokens are retained.

## Delivered behavior

- Home waits for a successful account load before showing the first-run guide. Returning users see their own open polls, three recent settled polls, full Closed history, and a Templates entry. The account menu retains notifications and their unread count. Demo moderation badges are hidden.
- The standalone template browser includes six starters and private saved templates. Owned polls offer an editable Save as template dialog. Saving a source again updates its existing template. Metadata comes from stored rounds, schedules, and approved options.
- Template drafts use the latest completed round, or the original snapshot while no linked round is complete. They carry approved suggestions, exclude a unique previous winner by default when two choices remain, and show result-based runner-up/New badges. Options can be dragged or moved through accessible actions. Draft edits, order, settings, exclusion, and request UUID survive saved-state restoration.
- Weekly deadlines resolve in the stored IANA timezone. Reuse opens a draft and writes nothing until Create is confirmed. Drafts require 2–10 distinct, nonblank choices.
- Single-choice votes remain in `votes`; multiple-choice ballots use separate private rows. Complete ballot replacement and voter/selection counts are transactional. Existing single-choice clients retain `cast_vote`; that RPC rejects multiple-choice polls. Android and web restore complete selected-option sets.
- Existing incomplete poll records are preserved. The unavailable screen remains available for records with fewer than two options. Existing stability handling, account-isolated caches, cancellation, retry, and offset-aware timestamp parsing are retained.

## Prioritized findings addressed

| Priority | Finding | Resolution |
| --- | --- | --- |
| P1 | Repeated template creation could increment history or create duplicate notifications. | Creator/request advisory lock and one transaction cover creation, notification, and round allocation. Identical retries return the committed poll before allocating a round. Failed creation rolls back history. |
| P1 | Treating multi-choice selections as ordinary vote rows would break uniqueness and voter counts. | Separate complete ballots, a parent-poll lock, validated approved options, and atomic count deltas. One person remains one voter across replacements. |
| P2 | Publicly readable shared polls should not populate another account's My polls. | Home filters by the authenticated creator; empty and failed loading remain distinct. |
| P2 | Background template refresh could overwrite an edited draft. | The draft initializes once and persists its own fields and stable option keys. |
| P2 | Approved suggestions can expand a live poll beyond the creation limit. | Ballots accept any nonempty subset of approved options, including expanded polls. Reused drafts still require editing down to 2–10 choices before creation. |
| P2 | Equal timestamps on legacy options could reorder rows after a refresh. | Both clients use position, creation time, and ID as deterministic ordering keys. |

## Database delivery

Applied through Supabase MCP to `wywzsnegtykcdqfkvtsj`:

1. `20261015101848_templates_and_multi_choice`
2. `20261015104704_allow_complete_ballot_sets`

The previous RLS/privilege fix remains deployed. New storage is RLS-protected and owner-readable, with writes restricted to authenticated RPCs. New aggregate, ordering, and lineage fields cannot be forged through client table grants. Creation request rows remain inaccessible directly. The full schema snapshot and versioned migrations are mirrored in the web repository.

Both SQL suites ran successfully through MCP in rollback-only transactions:

- `supabase/tests/stability_rollback.sql`: atomic creation, retries, creator ownership, suggestion author/approval checks, moderation, vote changes, closing, and expiration.
- `supabase/tests/templates_ballots_rollback.sql`: private template storage, source round one, repeated save/reuse, failed round allocation, creation notification uniqueness, counter privileges, single/multiple ballots, replacement, idempotent repeats, locked votes, empty/duplicate/cross-poll choices, expanded option sets, closing, and expiry.

Advisors were rerun after both migrations; the captured summary is `supabase/advisors-2026-10-15.json`. The owner-checked authenticated SECURITY DEFINER RPCs and authenticated anonymous voter access are intentional. The request table's RLS-without-policies notice reflects intentional client denial. Remaining follow-ups include [leaked password protection](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection), existing [RLS initialization-plan warnings](https://supabase.com/docs/guides/database/database-linter?lint=0003_auth_rls_initplan), and [foreign-key indexes](https://supabase.com/docs/guides/database/database-linter?lint=0001_unindexed_foreign_keys). Authentication settings and dependency versions were not changed by this patch. The upstream web merge had dropped Rolldown’s platform binding entries; their existing exact-version metadata was restored in the lockfile. Windows npm still required a local optional-native-binding install before the final test run. `npm audit` reports seven affected packages through the existing [braces denial-of-service advisory](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm); dependency remediation remains separate.

## Validation evidence

| Check | Result |
| --- | --- |
| Android JVM tests | 74 tests, zero failures/errors, including existing stability tests and new template, retry, cancellation, account-switch, deadline/DST, draft restoration, and ballot tests. |
| Android lint | Zero errors, 61 warnings. |
| Android builds | Debug and release builds; final artifacts under `app/build/outputs/apk`. Release remains unsigned under the existing project configuration. |
| Physical Android device | 11 instrumentation checks passed: nine offline visual/navigation/loading checks plus two database timestamp checks. |
| Web tests | 26 tests passed after merging current web master: existing poll/blog logic plus ballot API contracts, accessible radios/checkboxes, and separate voter/selection totals. |
| Web lint and production build | Passed. Existing Google Sans fallback-metadata warning remains nonfatal. A stale Next.js worker-cache panic was resolved by clearing only the local `.next` cache. |
| Browser checks | Isolated fixture backend: single-choice submission and Keep my vote, multi-choice submission and full replacement, restored selections, voter/selection totals, and English/Greek locale switching. No real ballots were submitted. |
| SQL checks | Both rollback suites passed after the final migration. |

### Device captures

- [First run](docs/screenshots/home-first-run.png)
- [My polls](docs/screenshots/home-my-polls.png)
- [Personal templates](docs/screenshots/templates-personal.png)
- [Template draft](docs/screenshots/template-film-draft.png)
- [Only closed polls](docs/screenshots/home-only-closed.png)
- [Empty templates in dark mode](docs/screenshots/templates-empty-dark.png)
- [320 dp width with 150% text](docs/screenshots/home-narrow-large-text.png)
- [Browser single-choice result](docs/screenshots/web-single.png)

The last device rerun initially failed because the phone had entered Doze. After the user manually unlocked it, all 11 checks passed.

The four primary captures were compared visually against the supplied references. Platform system bars, existing Pickle typography, local date formatting, and screen height account for intentional differences. All user names, history, counts, and options in these captures belong to offline test fixtures; production UI uses account data.

Web review: [pickle-web PR #5](https://github.com/klaudjoshkurta/pickle-web/pull/5).

## Release and validation limits

Release order remains database → compatible web → Android. Database migrations are applied; web changes are delivered for review, with no merge or production release performed. The connected device receives only a debug test build. Distribute Android after the compatible web client is released.

The isolated Postgres concurrency harness was extended for template retries and multi-choice voting/closing, and passed a JavaScript syntax check. It was not executed because this machine has neither `psql` nor Docker/local Postgres configured. The sequential rollback tests verify the transactional invariants; they do not establish concurrent stress-test coverage. Run `supabase/tests/concurrency.mjs` against an explicitly isolated local database before release.

Draft restoration is covered by saved-state snapshot tests. A real OS process-kill/rotation cycle with an authenticated template draft has not been verified. Browser tests use an in-memory fixture API; a deployed shared multi-choice poll should receive a final cross-client smoke test after the web release. These are remaining acceptance checks, not claimed successes.
