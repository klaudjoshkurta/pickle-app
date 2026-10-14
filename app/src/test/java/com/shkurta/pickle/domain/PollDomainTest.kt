package com.shkurta.pickle.domain

import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.domain.usecases.poll.CreatePollUseCase
import com.shkurta.pickle.domain.usecases.poll.ObservePollUseCase
import com.shkurta.pickle.domain.usecases.poll.SuggestOptionUseCase
import com.shkurta.pickle.testing.FakeAuthRepository
import com.shkurta.pickle.testing.FakePollRepository
import com.shkurta.pickle.testing.testPoll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PollDomainTest {
    @Test fun normalizationAndValidationUseDistinctNonblankOptions() {
        assertEquals(listOf("Pizza", "Pasta"), distinctOptionTexts(listOf(" Pizza ", "pizza", "", " Pasta ")))
        assertFalse(isValidPollInput(" ", listOf("A", "B")))
        assertFalse(isValidPollInput("Question", listOf("A", " a ")))
        assertTrue(isValidPollInput("Question", listOf("A", "B")))
    }

    @Test fun creationRequiresIdentityAndValidInput() = runTest {
        val auth = FakeAuthRepository()
        val polls = FakePollRepository()
        val create = CreatePollUseCase(polls, auth)
        auth.currentUserId = null
        assertNull(create("Question", listOf("A", "B"), PollSettings(), null, null))
        auth.currentUserId = "creator"
        assertNull(create("Question", listOf("A", " a "), PollSettings(), null, null))
        assertEquals(0, polls.createCalls)
        assertEquals("created", create("Question", listOf("A", "B"), PollSettings(), null, null)?.id)
        assertEquals("creator", polls.lastCreator)
    }

    @Test fun suggestionsResolveCurrentIdentityOnEveryCall() = runTest {
        val auth = FakeAuthRepository()
        val polls = FakePollRepository()
        val suggest = SuggestOptionUseCase(polls, auth)
        assertEquals(OptionStatus.PENDING, suggest("poll", "Soup"))
        assertEquals("creator", polls.lastSuggestionUser)
        auth.currentUserId = null
        suggest("poll", "Salad")
        assertEquals("", polls.lastSuggestionUser)
    }

    @Test fun pollObservationFindsOnlyRequestedPollAndReflectsRemoval() = runTest {
        val polls = FakePollRepository()
        val observe = ObservePollUseCase(polls)
        assertEquals("poll", observe("poll").first()?.id)
        assertNull(observe("missing").first())
        polls.polls.value = emptyList()
        assertNull(observe("poll").first())
    }

    @Test fun resultsExcludePendingOptionsAndPreserveWinnerTieAndNoVotes() {
        val poll = testPoll()
        assertEquals(Outcome.NoVotes, poll.toResults().outcome)
        val winner = poll.copy(options = poll.options.map { it.copy(voteCount = if (it.id == "a") 3 else 1) })
        assertEquals("a", (winner.toResults().outcome as Outcome.Winner).option.id)
        assertEquals(0.75f, winner.toResults().options.first().percentage, 0.001f)
        val tie = poll.copy(options = poll.options.map { it.copy(voteCount = 2) } +
            poll.options.first().copy(id = "pending", status = OptionStatus.PENDING, voteCount = 100))
        assertEquals(4, tie.toResults().totalVotes)
        assertEquals(2, (tie.toResults().outcome as Outcome.Tie).options.size)
    }
}
