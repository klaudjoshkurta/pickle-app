# MVVM architecture

The app keeps one Gradle module and three main packages:

- `ui`: Compose screens, immutable UI state, ViewModels, navigation, theme, and platform interactions.
- `domain`: Kotlin models, repository contracts in `repository`, and operations in `usecases/auth`, `usecases/poll`, and `usecases/notification`.
- `data`: Supabase and in-memory implementations, DTO mapping, and sample fixtures.

Screens collect ViewModel state with `collectAsStateWithLifecycle` and send actions through callbacks. ViewModels call use cases for poll/authentication actions, coordinate template loading through domain repositories, and own coroutine lifetimes. Use cases depend on domain repository interfaces; Hilt binds those interfaces to data implementations in `di`. Domain code must not import Android, UI, data implementations, or Supabase.

Non-sensitive form inputs, filters, and poll selections are saved through `SavedStateHandle`. Passwords remain in memory. Poll creation IDs use a buffered channel consumed as a flow, separate from persistent state, to avoid replaying navigation after recollection.

Repository mutation semantics, server RPCs, and notification side effects remain in the existing implementations. Shared input normalization and poll validation live in domain. “My polls” filters by the signed-in creator, with public sample data only in demo mode. Template storage has its own account-isolated repository cache. Poll selections and saved ballots are sets of option IDs; voter counts are distinct from selection counts.

Core library desugaring supports the existing `java.time` usage on the declared minimum Android API 24. The full `supabase_schema.sql` snapshot and versioned migrations define templates, ordered options, idempotent round allocation, and complete ballots. New clients use `create_poll_v2` and `cast_ballot`; legacy single-choice RPCs remain available.

Validate with `gradlew.bat testDebugUnitTest assembleDebug assembleRelease lintDebug`. Regression tests cover domain behavior, authentication, forms and restoration, creation events, poll state/actions, home filters, menu counts, and notification read state, template reuse/deadlines, ballot replacement, and account changes. See `HOME_TEMPLATES_REVIEW.md` for device captures, SQL validation, and release acceptance checks.
