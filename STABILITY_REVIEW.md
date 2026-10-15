# Pickle stability review — 15 October 2026

## Findings and implemented fixes

| Priority | Finding | Change |
| --- | --- | --- |
| P1 | Home refresh and most poll, notification, and sign-out coroutines allowed network/database errors to escape. This can terminate the Android process. | Screen action runner catches expected exceptions, preserves cancellation, exposes progress and sanitized retry messages, and suppresses overlapping actions. |
| P1 | Follow-up device logs showed `DateTimeParseException` during Home refresh after sign-in. Actual PostgREST timestamps contain numeric offsets and microseconds; desktop fixtures previously used only `Z`. | All database timestamp parsing now uses `OffsetDateTime` with `ISO_OFFSET_DATE_TIME`, then converts to `Instant`. Added desktop and physical-device regression tests using real response formats. Updated the installed debug build without clearing app data and confirmed Home loads with no error. |
| P1 | Supabase's option-insert policy resolved an unqualified `status` to the parent poll. Database logs recorded repeated insert-policy violations. | Applied migration `20261015090518_fix_option_insert_policy_and_client_privileges`; all option fields are explicitly qualified. |
| P1 | Android created the poll and its options in separate requests. Failure could leave incomplete polls; retries could duplicate them. | Android now calls the deployed transactional `create_poll` RPC, retains a request UUID for an unchanged draft across retries/restoration, and uses the notification inserted by that transaction. |
| P1 | Follow-up reads and notification requests could turn a committed write into an apparent failure. | Committed writes produce a distinct saved-but-refresh-failed state. The retry control refreshes instead of replaying that write. |
| P1 | Singleton vote and notification caches survived account changes; delayed requests could repopulate an old user's data. | Account-scoped caches clear on session changes, active requests are canceled, and cache publication checks the originating session. |
| P2 | Initial unauthenticated state was indistinguishable from session restoration. Auth navigation could replace a restored detail screen; sign-out navigated before completion. | Explicit initialized session state, tested redirect decisions, and sign-out success signaling. |
| P2 | Guest navigation used live repositories without authentication and displayed an empty or failing experience. | Guest mode reads bundled sample polls/notifications without database calls. Live poll creation requires sign-in; demo poll details show a sign-in action. |
| P2 | Unknown saved enum names and malformed remote enums/dates could throw during restoration or refresh. | Safe saved-state defaults; invalid remote records fail recoverably while retaining the last valid snapshot. |
| P2 | App font certificate resources collided with private Compose resource names. | Renamed app-owned certificate resources and updated the provider reference. |
| P2 | Client database roles held unnecessary `TRUNCATE`, `TRIGGER`, and `REFERENCES` privileges. | Revoked those privileges on app tables; existing RPC signatures and other clients' access patterns remain intact. |

These findings establish plausible crash paths and a confirmed database failure. No Android crash stack trace was available to attribute every reported crash to a specific path.

## Database verification

- Applied and recorded the migration through Supabase MCP on project `wywzsnegtykcdqfkvtsj`.
- Executed `supabase/tests/stability_rollback.sql` using the authenticated role with temporary test identities. Atomic creation/retry, ownership/author/approval checks, vote totals, moderation, closing, and expiration passed. All fixtures were rolled back.
- Rebuilt the bootstrap SQL from deployed table, function, trigger, policy, index, and permission definitions. Executed the final bootstrap in a temporary schema and rolled it back successfully.
- Verified zero test identities remain, zero dangerous client grants remain, and the creation-request table is inaccessible directly to authenticated clients.
- Preserved all four existing polls, including the one incomplete poll. The app displays an unavailable state for an incomplete poll rather than inventing missing options.

## Android verification

- Unit tests: 60 passed, 0 failed (36 existing tests plus 24 regression tests).
- Physical-device tests: both timestamp regression tests passed. Confirmed the signed-in Home screen is visible after restarting the updated app, the generic error is absent, and the new process has no `PickleAction` or `AndroidRuntime` error logs.
- Android lint: 0 errors, 61 warnings, down from 66. The remaining warnings concern dependency updates (16), certificate-string typo suggestions (35), unused resources (9), and a redundant manifest label (1).
- Debug APK: built successfully at `app/build/outputs/apk/debug/app-debug.apk`.
- Release APK: built successfully at `app/build/outputs/apk/release/app-release-unsigned.apk`; signing is required for distribution.
- Final combined Gradle validation completed successfully, including release R8 optimization and release lint checks. `git diff --check` passed.

At the original rollout no device was connected. During the follow-up, a physical phone became available and the timestamp tests and signed-in Home startup were verified on it. Offline operation, rotation, full process restoration, and expired sessions still need a broader Android device pass. JVM tests cover the underlying failure handling, session guard, restoration inputs, and redirect decisions.

The offset-aware parser follows the [Android OffsetDateTime API](https://developer.android.com/reference/java/time/OffsetDateTime). Malformed timestamps remain recoverable data errors; valid database dates are preserved rather than rewritten.

## Remaining maintenance findings

- Dependency upgrades are intentionally separate from the crash patch. Runtime dependency versions are unchanged; the matching Ktor mock engine was added only for tests.
- Supabase performance advisors identify five unindexed foreign keys and eleven policies that can cache `auth.uid()` with scalar subqueries. These are follow-up optimizations for the shared backend. [RLS performance guidance](https://supabase.com/docs/guides/database/postgres/row-level-security#rls-performance-recommendations).
- Advisor warnings about authenticated `SECURITY DEFINER` RPC execution reflect intentional, authenticated entry points with explicit authorization checks and fixed search paths. Internal helper functions remain unavailable to client roles. [Advisor explanation](https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable).
- The creation-request table intentionally has RLS without client policies because access is exclusively through the creation RPC. [Advisor explanation](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy).
- Anonymous Supabase identities remain supported by existing backend policies for other clients. Android's offline demo does not create anonymous accounts. [Advisor explanation](https://supabase.com/docs/guides/database/database-advisors?queryGroups=lint&lint=0012_auth_allow_anonymous_sign_ins).
- Leaked-password protection is disabled in the existing Auth configuration; it was not changed as part of this stability patch. [Configuration guidance](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection).

## Reproduce

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --offline --max-workers=2
.\gradlew.bat :app:assembleDebug :app:assembleRelease --offline --max-workers=2
```

For a new Supabase project, use `supabase_schema.sql`. For this existing project, the recorded migration is already applied; do not run the bootstrap over the live database.
