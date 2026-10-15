package com.shkurta.pickle.ui

import androidx.lifecycle.SavedStateHandle
import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.domain.usecases.auth.*
import com.shkurta.pickle.domain.usecases.notification.*
import com.shkurta.pickle.domain.usecases.poll.*
import com.shkurta.pickle.testing.*
import com.shkurta.pickle.ui.auth.*
import com.shkurta.pickle.ui.create.*
import com.shkurta.pickle.ui.home.*
import com.shkurta.pickle.ui.menu.MenuViewModel
import com.shkurta.pickle.ui.notifications.NotificationsViewModel
import com.shkurta.pickle.ui.poll.PollUiState
import com.shkurta.pickle.ui.poll.PollViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MvvmViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val auth = FakeAuthRepository()
    private val polls = FakePollRepository()
    private val notifications = FakeNotificationRepository()

    private fun authViewModel(handle: SavedStateHandle = SavedStateHandle()) =
        AuthViewModel(handle, SignInUseCase(auth), SignUpUseCase(auth), ObserveAuthenticationUseCase(auth))

    private fun createViewModel(handle: SavedStateHandle = SavedStateHandle()) =
        CreatePollViewModel(handle, CreatePollUseCase(polls, auth), RefreshPollsUseCase(polls), RefreshNotificationsUseCase(notifications), FakeTemplateRepository(), polls)

    private fun pollViewModel(handle: SavedStateHandle = SavedStateHandle(mapOf("pollId" to "poll"))) =
        PollViewModel(handle, ObservePollUseCase(polls), ObserveMyVotesUseCase(polls),
            ObserveAuthenticationUseCase(auth), GetCurrentUserUseCase(auth), VoteUseCase(polls),
            SuggestOptionUseCase(polls, auth), ModerateOptionUseCase(polls), ClosePollUseCase(polls), FakeTemplateRepository(), RefreshPollsUseCase(polls))

    private fun <T> TestScope.subscribe(state: StateFlow<T>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect() }
    }

    private fun restored(handle: SavedStateHandle) =
        SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })

    @Test fun homeFiltersOwnedPollsAndRefreshes() = runTest {
        polls.polls.value = listOf(testPoll(), testPoll("guest", "other"), testPoll("closed", status = PollStatus.CLOSED))
        val handle = SavedStateHandle()
        val vm = HomeViewModel(handle, ObservePollsUseCase(polls), ObserveNotificationsUseCase(notifications),
            ObserveAuthenticationUseCase(auth), GetCurrentUserUseCase(auth),
            RefreshPollsUseCase(polls), RefreshNotificationsUseCase(notifications))
        subscribe(vm.uiState)
        runCurrent()
        assertEquals(listOf("poll"), vm.uiState.value.polls.map { it.id })
        assertTrue(vm.uiState.value.hasUnreadNotifications)
        assertEquals(1, polls.refreshCalls)
        assertEquals(1, notifications.refreshCalls)
        vm.selectFilter(HomeFilter.CLOSED)
        runCurrent()
        assertEquals(listOf("closed"), vm.uiState.value.polls.map { it.id })
        assertEquals(HomeFilter.CLOSED.name, handle.get<String>("filter"))
        notifications.markAllRead()
        runCurrent()
        assertFalse(vm.uiState.value.hasUnreadNotifications)
    }

    @Test fun menuTotalsOnlyIncludeCreatedPollsAndSignOutUsesUseCase() = runTest {
        val votedPoll = testPoll().let { it.copy(options = it.options.map { option -> option.copy(voteCount = 2) }) }
        polls.polls.value = listOf(votedPoll, testPoll("closed", status = PollStatus.CLOSED), testPoll("other", "someone"))
        val vm = MenuViewModel(ObservePollsUseCase(polls), ObserveNotificationsUseCase(notifications),
            ObserveAuthenticationUseCase(auth), GetCurrentUserUseCase(auth), SignOutUseCase(auth))
        subscribe(vm.uiState)
        runCurrent()
        assertEquals(2, vm.uiState.value.createdCount)
        assertEquals(1, vm.uiState.value.openCount)
        assertEquals(4, vm.uiState.value.voteCount)
        assertEquals(2, vm.uiState.value.unreadCount)
        vm.signOut()
        runCurrent()
        assertEquals(1, auth.signOutCalls)
        assertEquals(0, vm.uiState.value.createdCount)
    }

    @Test fun signInPreservesValidationTrimmingLoadingAndErrorMessages() = runTest {
        val vm = authViewModel()
        vm.submit()
        assertEquals(0, auth.signInCalls)
        vm.setEmail(" alex@example.com ")
        vm.setPassword("secret")
        auth.result = Result.failure(IllegalStateException("Invalid credentials"))
        vm.submit()
        assertEquals(AuthStatus.Loading, vm.uiState.value.status)
        runCurrent()
        assertEquals("alex@example.com", auth.lastEmail)
        assertEquals(AuthStatus.Error("Sign in failed. Check your connection and credentials."), vm.uiState.value.status)
        vm.clearError()
        assertEquals(AuthStatus.Idle, vm.uiState.value.status)
        auth.result = Result.success(Unit)
        vm.submit()
        runCurrent()
        assertEquals(AuthStatus.Success, vm.uiState.value.status)
    }

    @Test fun signupStepsRestoreButPasswordIsNeverSaved() = runTest {
        val handle = SavedStateHandle()
        val vm = authViewModel(handle)
        vm.initialize(true)
        vm.nextStep()
        assertEquals(SignUpStep.Name, vm.uiState.value.step)
        vm.setName("Alex")
        vm.nextStep()
        assertEquals(SignUpStep.Avatar, vm.uiState.value.step)
        vm.selectAvatar(vm.uiState.value.avatarSeeds.first())
        vm.nextStep()
        vm.setEmail("alex@example.com")
        vm.nextStep()
        vm.setPassword("secret")
        assertTrue(vm.uiState.value.isStepValid)
        val recreated = authViewModel(restored(handle))
        recreated.initialize(false)
        assertTrue(recreated.uiState.value.isSignUp)
        assertEquals(SignUpStep.Password, recreated.uiState.value.step)
        assertEquals(vm.uiState.value.avatarSeeds, recreated.uiState.value.avatarSeeds)
        assertEquals(vm.uiState.value.selectedSeed, recreated.uiState.value.selectedSeed)
        assertEquals("", recreated.uiState.value.password)
        assertFalse(handle.keys().contains("password"))
        recreated.previousStep()
        assertEquals(SignUpStep.Email, recreated.uiState.value.step)
        recreated.toggleMode()
        assertFalse(recreated.uiState.value.isSignUp)
        assertEquals(SignUpStep.Name, recreated.uiState.value.step)
    }

    @Test fun signupSubmitsTrimmedMetadataAndPreservesSuccessAndErrorState() = runTest {
        val vm = authViewModel()
        vm.initialize(true)
        vm.setName(" Alex ")
        vm.setEmail(" alex@example.com ")
        vm.selectAvatar("face-seed")
        vm.setPassword("short")
        vm.submit()
        assertEquals(0, auth.signUpCalls)
        vm.setPassword("secret")
        vm.submit()
        assertEquals(AuthStatus.Loading, vm.uiState.value.status)
        runCurrent()
        assertEquals(1, auth.signUpCalls)
        assertEquals("Alex", auth.lastName)
        assertEquals("alex@example.com", auth.lastEmail)
        assertEquals(DiceBear.avatarUrl("face-seed"), auth.lastAvatar)
        assertEquals(AuthStatus.Success, vm.uiState.value.status)
        auth.result = Result.failure(IllegalStateException("Email already registered"))
        vm.submit()
        runCurrent()
        assertEquals(AuthStatus.Error("Sign up failed. Check your connection and account details."), vm.uiState.value.status)
    }

    @Test fun avatarUrlPreservesQueryEscaping() {
        val url = DiceBear.avatarUrl("a b+c/?~!()'")
        assertTrue(url.contains("seed=a%20b%2Bc%2F%3F~!()'&"))
    }

    @Test fun creationFormRestoresSettingsAndEnforcesOptionLimitsAndExpectedVoterParsing() = runTest {
        val handle = SavedStateHandle()
        val vm = createViewModel(handle)
        vm.setQuestion("Dinner?")
        vm.setOption(0, "Pizza")
        vm.setOption(1, " pizza ")
        assertFalse(vm.uiState.value.isValid)
        vm.setOption(1, "Pasta")
        assertTrue(vm.uiState.value.isValid)
        repeat(12) { vm.addOption() }
        assertEquals(10, vm.uiState.value.options.size)
        repeat(12) { vm.removeOption(2) }
        assertEquals(2, vm.uiState.value.options.size)
        vm.setSettings(PollSettings(allowSuggestions = true, requireApproval = false))
        vm.setDeadline(Deadline.ONE_DAY)
        vm.setExpectedVoters("12x345")
        assertEquals("1234", vm.uiState.value.expectedVoters)
        val recreated = createViewModel(restored(handle))
        assertEquals(vm.uiState.value, recreated.uiState.value)
    }

    @Test fun creationEventSurvivesLateCollectorAndIsDeliveredOnlyOnce() = runTest {
        val vm = createViewModel()
        vm.setQuestion("Dinner?")
        vm.setOption(0, "Pizza")
        vm.setOption(1, "Pasta")
        vm.setDeadline(Deadline.ONE_HOUR)
        vm.setExpectedVoters("4")
        val before = Instant.now()
        vm.createPoll()
        runCurrent()
        assertEquals(1, polls.createCalls)
        assertEquals(4, polls.lastExpectedVoters)
        assertTrue(polls.lastDeadline!!.isAfter(before.plusSeconds(3599)))
        val first = mutableListOf<String>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.createdPollId.collect { first += it } }
        runCurrent()
        assertEquals(listOf("created"), first)
        collector.cancel()
        val second = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.createdPollId.collect { second += it } }
        runCurrent()
        assertTrue(second.isEmpty())
    }

    @Test fun creationWithoutUserDoesNotWriteOrEmit() = runTest {
        auth.currentUserId = null
        val vm = createViewModel()
        vm.setQuestion("Dinner?")
        vm.setOption(0, "Pizza")
        vm.setOption(1, "Pasta")
        val events = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.createdPollId.collect { events += it } }
        vm.createPoll()
        runCurrent()
        assertEquals(0, polls.createCalls)
        assertTrue(events.isEmpty())
    }

    @Test fun pollVotingSelectionRestoresAndTracksVoteChanges() = runTest {
        val handle = SavedStateHandle(mapOf("pollId" to "poll"))
        val vm = pollViewModel(handle)
        subscribe(vm.uiState)
        runCurrent()
        assertTrue(vm.uiState.value.isVoting)
        assertFalse(vm.uiState.value.showResults)
        vm.selectOption("a")
        runCurrent()
        val recreated = pollViewModel(restored(handle))
        subscribe(recreated.uiState)
        runCurrent()
        assertEquals("a", recreated.uiState.value.selectedOptionId)
        vm.vote()
        runCurrent()
        assertEquals("a", vm.uiState.value.myVoteOptionId)
        assertFalse(vm.uiState.value.isVoting)
        assertTrue(vm.uiState.value.showResults)
        vm.changeVote()
        vm.selectOption("b")
        runCurrent()
        assertTrue(vm.uiState.value.isVoting)
        vm.keepVote()
        runCurrent()
        assertEquals("a", vm.uiState.value.selectedOptionId)
        assertFalse(vm.uiState.value.isChangingVote)
        vm.changeVote()
        vm.selectOption("b")
        vm.vote()
        runCurrent()
        assertEquals("b", vm.uiState.value.myVoteOptionId)
        assertEquals("b", vm.uiState.value.selectedOptionId)
        assertFalse(vm.uiState.value.isChangingVote)
    }

    @Test fun pollSuggestionsModerationAndClosingUseCorrectPollId() = runTest {
        val vm = pollViewModel()
        subscribe(vm.uiState)
        runCurrent()
        vm.suggestOption("Soup")
        runCurrent()
        vm.moderate("suggestion", true)
        runCurrent()
        vm.close()
        runCurrent()
        assertEquals(OptionStatus.PENDING, vm.uiState.value.lastSuggestionStatus)
        assertEquals(Triple("poll", "suggestion", true), polls.lastModeration)
        assertEquals("poll", polls.lastClosedPoll)
        vm.clearSuggestionStatus()
        runCurrent()
        assertNull(vm.uiState.value.lastSuggestionStatus)
        polls.polls.value = emptyList()
        runCurrent()
        assertNull(vm.uiState.value.poll)
    }

    @Test fun pollVisibilityPreservesClosedNoVoteChangeAndCreatorModerationRules() {
        val poll = testPoll()
        val hidden = PollUiState.from(poll, "guest", null)
        assertFalse(hidden.showResults)
        val locked = PollUiState.from(poll.copy(settings = poll.settings.copy(allowVoteChange = false)), "guest", "a")
        assertFalse(locked.canVote)
        assertTrue(locked.showResults)
        val closed = PollUiState.from(poll.copy(status = PollStatus.CLOSED), "creator", null)
        assertTrue(closed.showResults)
        assertFalse(closed.canVote)
        val pendingPoll = poll.copy(options = poll.options + poll.options.first().copy(id = "pending", status = OptionStatus.PENDING))
        assertEquals(1, PollUiState.from(pendingPoll, "creator", null).pendingSuggestions.size)
        assertTrue(PollUiState.from(pendingPoll, "guest", null).pendingSuggestions.isEmpty())
        assertTrue(PollUiState.from(pendingPoll.copy(status = PollStatus.CLOSED), "creator", null).pendingSuggestions.isEmpty())
    }

    @Test fun notificationActionsUpdateStateWithoutMarkingUnrelatedItemsRead() = runTest {
        val vm = NotificationsViewModel(ObserveNotificationsUseCase(notifications),
            MarkNotificationReadUseCase(notifications), MarkAllNotificationsReadUseCase(notifications), RefreshNotificationsUseCase(notifications))
        subscribe(vm.uiState)
        runCurrent()
        vm.markRead("one")
        runCurrent()
        assertTrue(vm.uiState.value.notifications.first().isRead)
        assertFalse(vm.uiState.value.notifications.last().isRead)
        vm.markAllRead()
        runCurrent()
        assertFalse(vm.uiState.value.hasUnread)
    }
}
