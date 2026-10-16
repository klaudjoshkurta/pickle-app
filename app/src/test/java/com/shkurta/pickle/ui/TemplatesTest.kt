package com.shkurta.pickle.ui

import androidx.lifecycle.SavedStateHandle
import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.domain.usecases.poll.*
import com.shkurta.pickle.domain.usecases.auth.*
import com.shkurta.pickle.domain.usecases.notification.RefreshNotificationsUseCase
import com.shkurta.pickle.testing.*
import com.shkurta.pickle.ui.create.*
import com.shkurta.pickle.ui.templates.TemplatesViewModel
import com.shkurta.pickle.ui.poll.PollViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.collect
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.*

@OptIn(ExperimentalCoroutinesApi::class)
class TemplatesTest {
    @get:Rule val dispatcher=MainDispatcherRule()
    private val auth=FakeAuthRepository()
    private val polls=FakePollRepository()
    private val templates=FakeTemplateRepository()
    private val notifications=FakeNotificationRepository()
    private val now=Instant.parse("2026-10-15T12:00:00Z")
    private fun template()=PollTemplate("template","poll","Film night","film","Film?",listOf("A","B","C"),PollSettings(),1,WeeklySchedule(5,"20:00","Europe/Athens"))
    private fun vm(handle:SavedStateHandle)=CreatePollViewModel(handle,CreatePollUseCase(polls,auth),RefreshPollsUseCase(polls),RefreshNotificationsUseCase(notifications),templates,polls,com.shkurta.pickle.ui.components.StarterLocalization { it.seed() })
    private fun pollVm(handle:SavedStateHandle=SavedStateHandle(mapOf("pollId" to "poll")))=PollViewModel(handle,ObservePollUseCase(polls),ObserveMyVotesUseCase(polls),ObserveAuthenticationUseCase(auth),GetCurrentUserUseCase(auth),VoteUseCase(polls),SuggestOptionUseCase(polls,auth),ModerateOptionUseCase(polls),ClosePollUseCase(polls),templates,RefreshPollsUseCase(polls))

    @Test fun startersHaveRealDefaultsAndNoInventedHistory() {
        PollStarter.entries.forEach {
            val seed=it.seed(now,ZoneId.of("Europe/Athens"))
            assertNull(seed.templateId);assertNull(seed.history);assertNull(seed.winnerText)
            assertTrue(seed.closesAt!!>now)
        }
        assertEquals(7,PollStarter.DATE.seed(now).options.size)
        assertEquals(VotingMode.MULTIPLE,PollStarter.GAME.seed(now).settings.votingMode)
        assertEquals(listOf("",""),PollStarter.DRIVER.seed(now).options)
        assertEquals(now.plusSeconds(10800),PollStarter.FILM.seed(now).closesAt)
    }
    @Test fun weeklyDeadlineUsesStoredZoneAndAdvancesWhenPassed() {
        val schedule=WeeklySchedule(5,"20:00","Europe/Athens")
        assertEquals(Instant.parse("2026-10-16T17:00:00Z"),schedule.nextOccurrence(now))
        assertEquals(Instant.parse("2026-10-23T17:00:00Z"),schedule.nextOccurrence(Instant.parse("2026-10-16T17:00:00Z")))
        assertEquals(Instant.parse("2026-10-30T18:00:00Z"),schedule.nextOccurrence(Instant.parse("2026-10-26T12:00:00Z")))
    }
    @Test fun seedUsesOnlyLatestCompletedRoundAndApprovedOptions() {
        val source=testPoll(status=PollStatus.CLOSED).copy(templateId="template",templateRound=1)
        val completed=source.copy(id="round2",templateRound=2,question="Latest?",options=listOf(
            PollOption("a","Winner",OptionStatus.APPROVED,5,createdAt=now),
            PollOption("b","Runner",OptionStatus.APPROVED,3,createdAt=now),
            PollOption("c","New suggestion",OptionStatus.APPROVED,1,createdAt=now),
            PollOption("p","Pending",OptionStatus.PENDING,createdAt=now),
            PollOption("r","Rejected",OptionStatus.REJECTED,createdAt=now)))
        val open=completed.copy(id="round3",templateRound=3,status=PollStatus.OPEN,question="Still live")
        val seed=template().seed(listOf(source,completed,open),now)
        assertEquals("Latest?",seed.question)
        assertEquals(listOf("Winner","Runner","New suggestion"),seed.options)
        assertEquals("Winner",seed.winnerText);assertEquals(setOf("Runner"),seed.runnerUpTexts)
        assertTrue("New suggestion" in seed.newTexts)
        assertEquals(template().optionTexts,template().seed(listOf(open),now).options)
    }
    @Test fun tiesAndNoVotesNeverOfferWinnerExclusion() {
        val poll=testPoll(status=PollStatus.CLOSED)
        assertNull(template().seed(listOf(poll),now).winnerText)
        assertNull(template().seed(listOf(poll.copy(options=poll.options.map {it.copy(voteCount=2)})),now).winnerText)
    }
    @Test fun templateDraftInitializesOnceRestoresOrderAndRetryUuid()=runTest {
        templates.templates.value=listOf(template())
        val source=testPoll(status=PollStatus.CLOSED).copy(options=listOf(
            PollOption("a","A",OptionStatus.APPROVED,4,createdAt=now),
            PollOption("b","B",OptionStatus.APPROVED,2,createdAt=now),
            PollOption("c","C",OptionStatus.APPROVED,1,createdAt=now)))
        polls.polls.value=listOf(source)
        val handle=SavedStateHandle(mapOf("templateId" to "template"))
        val first=vm(handle);runCurrent()
        assertTrue(first.uiState.value.excludeWinner)
        assertEquals(listOf("B","C"),first.uiState.value.options)
        first.moveOption(1,0);first.setQuestion("Edited?");first.setExcludeWinner(false)
        polls.failure=IOException();first.createPoll();first.createPoll();runCurrent()
        assertEquals(1,polls.createCalls)
        val uuid=polls.requestIds.single()
        val restored=vm(SavedStateHandle(handle.keys().associateWith {handle.get<Any?>(it)}));runCurrent()
        assertEquals(listOf("C","B","A"),restored.uiState.value.options)
        assertEquals("Edited?",restored.uiState.value.question)
        assertFalse(restored.uiState.value.excludeWinner)
        assertEquals(first.uiState.value.optionKeys,restored.uiState.value.optionKeys)
        polls.failure=null;restored.createPoll();runCurrent()
        assertEquals(uuid,polls.requestIds.last())
        assertEquals(1,templates.refreshCalls)
    }
    @Test fun twoOptionWinnerStartsIncludedAndInvalidDraftCannotCreate()=runTest {
        templates.templates.value=listOf(template())
        polls.polls.value=listOf(testPoll(status=PollStatus.CLOSED).let {it.copy(options=it.options.mapIndexed {i,o->o.copy(voteCount=if(i==0) 2 else 0)})})
        val vm=vm(SavedStateHandle(mapOf("templateId" to "template")));runCurrent()
        assertFalse(vm.uiState.value.excludeWinner)
        vm.setExcludeWinner(true);assertFalse(vm.uiState.value.isValid)
        vm.createPoll();runCurrent();assertEquals(0,polls.createCalls)
    }
    @Test fun templateRefreshFailureRetryAndSessionCancellation()=runTest {
        templates.failure=IOException()
        val vm=TemplatesViewModel(templates,polls,auth);runCurrent()
        assertNotNull(vm.actionState.value.message)
        templates.failure=null;vm.retry();runCurrent();assertNull(vm.actionState.value.message)
        templates.gate=CompletableDeferred();vm.refresh();runCurrent();assertTrue(vm.actionState.value.busy)
        auth.session.value=com.shkurta.pickle.domain.repository.AuthSession(false,"other");runCurrent()
        assertFalse(vm.actionState.value.busy)
        templates.gate!!.complete(Unit);runCurrent();assertNull(vm.actionState.value.message)
    }
    @Test fun failedDraftInitializationCanRetryAndExpandedSnapshotsRequireEditing()=runTest {
        templates.templates.value=listOf(template())
        templates.failure=IOException()
        val draft=vm(SavedStateHandle(mapOf("templateId" to "template")));runCurrent()
        assertNotNull(draft.actionState.value.message)
        templates.failure=null;draft.retry();runCurrent();assertNotNull(draft.uiState.value.title)
        assertFalse(CreatePollUiState(question="Question",options=(1..11).map {"Option $it"}).isValid)
        assertFalse(CreatePollUiState(question="Question",options=listOf("A","B","")).isValid)
    }
    @Test fun multiChoicePercentagesUseVotersAndBarsCanUseSelectionShare() {
        val poll=testPoll().copy(settings=PollSettings(votingMode=VotingMode.MULTIPLE),voterCount=2,options=testPoll().options.map {it.copy(voteCount=2)})
        assertEquals(4,poll.totalSelections);assertEquals(2,poll.totalVotes)
        assertEquals(1f,poll.toResults().options[0].percentage)
        assertTrue(poll.toResults().outcome is Outcome.Tie)
    }
    @Test fun ballotSelectionsRestoreAndKeepVoteRestoresTheEntireSavedSet()=runTest {
        polls.polls.value=listOf(testPoll().copy(settings=PollSettings(votingMode=VotingMode.MULTIPLE)))
        val handle=SavedStateHandle(mapOf("pollId" to "poll"))
        val first=pollVm(handle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){first.uiState.collect()};runCurrent()
        first.selectOption("a");first.selectOption("b");runCurrent()
        assertEquals(setOf("a","b"),first.uiState.value.selectedOptionIds)
        val restored=pollVm(SavedStateHandle(handle.keys().associateWith {handle.get<Any?>(it)}))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){restored.uiState.collect()};runCurrent()
        assertEquals(setOf("a","b"),restored.uiState.value.selectedOptionIds)
        restored.vote();runCurrent();assertEquals(setOf("a","b"),polls.myVotes.value["poll"])
        restored.changeVote();restored.selectOption("a");runCurrent()
        assertEquals(setOf("b"),restored.uiState.value.selectedOptionIds)
        restored.keepVote();runCurrent();assertEquals(setOf("a","b"),restored.uiState.value.selectedOptionIds)
    }
    @Test fun templateSaveFailureRetriesAndCommittedSaveNeverReplays()=runTest {
        val vm=pollVm();runCurrent()
        templates.failure=IOException();vm.saveTemplate("Film","film",null);vm.saveTemplate("Film","film",null);runCurrent()
        assertEquals(1,templates.saveCalls);assertNotNull(vm.actionState.value.message)
        templates.failure=null;vm.retry();runCurrent();assertEquals("template",vm.savedTemplateId.value)
        templates.failure=SavedRefreshException("template");vm.saveTemplate("Film","film",null);runCurrent()
        assertTrue(vm.actionState.value.saved);val savedCalls=templates.saveCalls
        templates.failure=null;vm.retry();runCurrent();assertEquals(savedCalls,templates.saveCalls)
    }
}
