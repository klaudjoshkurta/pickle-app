package com.shkurta.pickle.ui

import androidx.lifecycle.SavedStateHandle
import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.domain.usecases.auth.*
import com.shkurta.pickle.domain.usecases.notification.*
import com.shkurta.pickle.domain.usecases.poll.*
import com.shkurta.pickle.testing.*
import com.shkurta.pickle.ui.auth.*
import com.shkurta.pickle.ui.components.ActionRunner
import com.shkurta.pickle.ui.create.*
import com.shkurta.pickle.ui.home.*
import com.shkurta.pickle.ui.menu.MenuViewModel
import com.shkurta.pickle.ui.notifications.NotificationsViewModel
import com.shkurta.pickle.ui.poll.PollViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class StabilityTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val auth = FakeAuthRepository()
    private val polls = FakePollRepository()
    private val notifications = FakeNotificationRepository()
    private fun create(handle: SavedStateHandle = SavedStateHandle()) = CreatePollViewModel(
        handle, CreatePollUseCase(polls, auth), RefreshPollsUseCase(polls), RefreshNotificationsUseCase(notifications), FakeTemplateRepository(), polls,
    )
    private fun draft(vm: CreatePollViewModel) {
        vm.setQuestion("Dinner?"); vm.setOption(0, "Pizza"); vm.setOption(1, "Pasta")
    }
    private fun poll() = PollViewModel(
        SavedStateHandle(mapOf("pollId" to "poll")), ObservePollUseCase(polls), ObserveMyVotesUseCase(polls),
        ObserveAuthenticationUseCase(auth), GetCurrentUserUseCase(auth), VoteUseCase(polls),
        SuggestOptionUseCase(polls, auth), ModerateOptionUseCase(polls), ClosePollUseCase(polls), FakeTemplateRepository(), RefreshPollsUseCase(polls),
    )

    @Test fun homeRefreshFailurePreservesDataAndStillRefreshesNotifications() = runTest {
        polls.refreshFailure = IOException()
        val vm = HomeViewModel(SavedStateHandle(), ObservePollsUseCase(polls), ObserveNotificationsUseCase(notifications),
            ObserveAuthenticationUseCase(auth), GetCurrentUserUseCase(auth), RefreshPollsUseCase(polls), RefreshNotificationsUseCase(notifications))
        runCurrent()
        assertNotNull(vm.actionState.value.message)
        assertEquals(1, notifications.refreshCalls)
        assertEquals(1, polls.polls.value.size)
        polls.refreshFailure = null
        vm.retry(); runCurrent()
        assertNull(vm.actionState.value.message)
    }

    @Test fun pollActionsCatchFailuresAndCanBeRetried() = runTest {
        for (action in listOf<(PollViewModel) -> Unit>(
            { it.selectOption("a"); it.vote() }, { it.suggestOption("Soup") },
            { it.moderate("suggestion", true) }, { it.close() },
        )) {
            val vm = poll(); runCurrent()
            polls.failure = IOException("sensitive server details")
            action(vm); runCurrent()
            assertFalse(vm.actionState.value.busy)
            assertNotNull(vm.actionState.value.message)
            assertFalse(vm.actionState.value.message!!.contains("sensitive"))
            polls.failure = null
            vm.retry(); runCurrent()
            assertNull(vm.actionState.value.message)
        }
    }

    @Test fun failedNotificationWritesAndRefreshAreRecoverable() = runTest {
        val vm = NotificationsViewModel(ObserveNotificationsUseCase(notifications), MarkNotificationReadUseCase(notifications),
            MarkAllNotificationsReadUseCase(notifications), RefreshNotificationsUseCase(notifications))
        runCurrent()
        for (action in listOf<() -> Unit>({ vm.markRead("one") }, { vm.markAllRead() })) {
            notifications.failure = IOException()
            action(); runCurrent()
            assertNotNull(vm.actionState.value.message)
            notifications.failure = null
            vm.retry(); runCurrent()
            assertNull(vm.actionState.value.message)
        }
        notifications.refreshFailure = IOException()
        vm.refresh(); runCurrent()
        assertNotNull(vm.actionState.value.message)
        notifications.refreshFailure = null
        vm.retry(); runCurrent()
        assertNull(vm.actionState.value.message)
    }

    @Test fun failedSignOutNeverEmitsSuccessAndRetryCompletes() = runTest {
        val vm = MenuViewModel(ObservePollsUseCase(polls), ObserveNotificationsUseCase(notifications),
            ObserveAuthenticationUseCase(auth), GetCurrentUserUseCase(auth), SignOutUseCase(auth))
        auth.failure = IOException()
        vm.signOut(); runCurrent()
        assertFalse(vm.signedOut.value)
        assertTrue(auth.isAuthenticated.value)
        auth.failure = null
        vm.retry(); runCurrent()
        assertTrue(vm.signedOut.value)
        assertFalse(auth.isAuthenticated.value)
    }

    @Test fun creationSuppressesDoubleTapsAndKeepsTheSubmittedDraft() = runTest {
        polls.gate = CompletableDeferred()
        val vm = create(); draft(vm)
        vm.createPoll(); vm.createPoll(); vm.setQuestion("Changed while saving")
        runCurrent()
        assertEquals(1, polls.createCalls)
        assertEquals("Dinner?", vm.uiState.value.question)
        assertTrue(vm.actionState.value.busy)
        polls.gate!!.complete(Unit); runCurrent()
        assertFalse(vm.actionState.value.busy)
    }

    @Test fun creationRetryRetainsRequestIdAcrossProcessRestorationAndEditsReplaceIt() = runTest {
        val handle = SavedStateHandle()
        val vm = create(handle); draft(vm)
        polls.failure = IOException()
        vm.createPoll(); runCurrent()
        val restored = create(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        restored.retry(); runCurrent()
        assertEquals(polls.requestIds[0], polls.requestIds[1])
        restored.setQuestion("Lunch?"); restored.createPoll(); runCurrent()
        assertNotEquals(polls.requestIds[1], polls.requestIds[2])
    }

    @Test fun savedCreationRetriesReadsWithoutReplayingTheWrite() = runTest {
        val vm = create(); draft(vm)
        polls.failure = SavedRefreshException("created")
        vm.createPoll(); runCurrent()
        assertTrue(vm.actionState.value.saved)
        polls.failure = null
        vm.retry(); runCurrent()
        assertEquals(1, polls.createCalls)
        assertEquals("created", vm.createdPollId.firstValue())
    }

    @Test fun savedVoteRetriesRefreshOnly() = runTest {
        val vm = poll(); runCurrent()
        polls.failure = SavedRefreshException()
        vm.selectOption("a"); vm.vote(); runCurrent()
        assertTrue(vm.actionState.value.saved)
        // If Retry replays the vote it will throw again; only refresh can succeed.
        vm.retry(); runCurrent()
        assertNull(vm.actionState.value.message)
        assertTrue(polls.myVotes.value.isEmpty())
    }

    @Test fun cancellationIsRethrownAndDoesNotBecomeAnErrorOrRetry() = runTest {
        val result = runCatching { cancellationSafeResult<Unit> { throw CancellationException() } }
        assertTrue(result.exceptionOrNull() is CancellationException)
        val runner = ActionRunner(backgroundScope)
        runner.run { throw CancellationException() }; runCurrent()
        assertFalse(runner.state.value.busy)
        assertNull(runner.state.value.message)
    }

    @Test fun unexpectedAuthFailuresAreSanitizedAndDoubleSubmitIsSuppressed() = runTest {
        val vm = LoginViewModel(SavedStateHandle(), SignInUseCase(auth))
        vm.setEmail("a@example.com"); vm.setPassword("secret")
        auth.failure = IOException("do not expose")
        vm.submit(); vm.submit(); runCurrent()
        assertEquals(1, auth.signInCalls)
        assertTrue(vm.uiState.value.status is AuthStatus.Error)
        assertEquals(AuthStatus.Error, vm.uiState.value.status)
    }

    @Test fun unknownSavedEnumsUseSafeDefaults() {
        assertEquals(Deadline.NONE, create(SavedStateHandle(mapOf("deadline" to "OLD_DEADLINE"))).uiState.value.deadline)
        val vm = RegisterViewModel(SavedStateHandle(mapOf("step" to "OLD_STEP")), SignUpUseCase(auth))
        assertEquals(RegistrationStep.Details, vm.uiState.value.step)
    }

    @Test fun demoShowsResultsWithoutChangingPollStatusOrAllowingVotes() {
        val state = com.shkurta.pickle.ui.poll.PollUiState.from(testPoll(), "", null).copy(isDemo = true)
        assertTrue(state.isOpen)
        assertTrue(state.showResults)
        assertFalse(state.canVote)
        assertFalse(state.isVoting)
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstValue(): T = this.first()
}
