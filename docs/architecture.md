# MVVM architecture

The app keeps one Gradle module and three main packages:

- `ui`: Compose screens, immutable UI state, ViewModels, navigation, theme, and platform interactions.
- `domain`: Kotlin models, repository contracts in `repository`, and operations in `usecases/auth`, `usecases/poll`, and `usecases/notification`.
- `data`: Supabase and in-memory implementations, DTO mapping, and sample fixtures.

Screens collect ViewModel state with `collectAsStateWithLifecycle` and send actions through callbacks. ViewModels call use cases and own coroutine lifetimes. Use cases depend on domain repository interfaces; Hilt binds those interfaces to data implementations in `di`. Domain code must not import Android, UI, data implementations, or Supabase.

Non-sensitive form inputs, filters, and poll selections are saved through `SavedStateHandle`. Passwords remain in memory. Poll creation IDs use a buffered channel consumed as a flow, separate from persistent state, to avoid replaying navigation after recollection.

Repository mutation semantics, server RPCs, and notification side effects remain in the existing implementations. Shared input normalization and poll validation live in domain. “My polls” retains the existing all-open-polls filter; menu statistics remain creator-specific.

Core library desugaring supports the existing `java.time` usage on the declared minimum Android API 24. No backend schema changes are required.

Validate with `gradlew.bat testDebugUnitTest assembleDebug lintDebug`. Regression tests cover domain behavior, authentication, forms and restoration, creation events, poll state/actions, home filters, menu counts, and notification read state.
