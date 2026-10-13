package com.shkurta.pickle.data

import com.shkurta.pickle.data.sample.SamplePolls
import com.shkurta.pickle.domain.model.OptionStatus
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PollRepositoryTest {

    private val repository = PollRepository(SamplePolls.all)

    private fun poll(id: String): Poll = repository.polls.value.first { it.id == id }

    private fun votes(pollId: String, optionId: String): Int =
        poll(pollId).options.first { it.id == optionId }.voteCount

    @Test
    fun vote_incrementsOptionAndRecordsMyVote() {
        repository.vote("poll_1", "opt_1_2")

        assertEquals(4, votes("poll_1", "opt_1_2"))
        assertEquals("opt_1_2", repository.myVotes.value["poll_1"])
    }

    @Test
    fun vote_changeMovesTheVote() {
        repository.vote("poll_1", "opt_1_2")
        repository.vote("poll_1", "opt_1_3")

        assertEquals(3, votes("poll_1", "opt_1_2"))
        assertEquals(2, votes("poll_1", "opt_1_3"))
        assertEquals(10, poll("poll_1").totalVotes)
        assertEquals("opt_1_3", repository.myVotes.value["poll_1"])
    }

    @Test
    fun vote_changeIgnoredWhenNotAllowed() {
        val locked = SamplePolls.pizzaNight.let {
            it.copy(settings = it.settings.copy(allowVoteChange = false))
        }
        val repository = PollRepository(listOf(locked))

        repository.vote("poll_1", "opt_1_2")
        repository.vote("poll_1", "opt_1_3")

        val options = repository.polls.value.single().options.associate { it.id to it.voteCount }
        assertEquals(4, options["opt_1_2"])
        assertEquals(1, options["opt_1_3"])
        assertEquals("opt_1_2", repository.myVotes.value["poll_1"])
    }

    @Test
    fun vote_ignoredForPendingOptionAndClosedPoll() {
        repository.vote("poll_1", "opt_1_4")
        repository.vote("poll_2", "opt_2_3")

        assertEquals(0, votes("poll_1", "opt_1_4"))
        assertEquals(2, votes("poll_2", "opt_2_3"))
        assertTrue(repository.myVotes.value.isEmpty())
    }

    @Test
    fun suggest_fromGuestIsPendingWhenApprovalRequired() {
        val status = repository.suggestOption("poll_1", "  Pasta ", "user_guest_2")

        assertEquals(OptionStatus.PENDING, status)
        val added = poll("poll_1").options.last()
        assertEquals("Pasta", added.text)
        assertEquals("user_guest_2", added.suggestedBy)
    }

    @Test
    fun suggest_fromCreatorOrWithoutApprovalIsApproved() {
        val fromCreator = repository.suggestOption("poll_1", "Pasta", SamplePolls.CREATOR_ID)
        val noApproval = repository.suggestOption("poll_3", "Meteora", "user_guest_2")

        assertEquals(OptionStatus.APPROVED, fromCreator)
        assertNull(poll("poll_1").options.last().suggestedBy)
        assertEquals(OptionStatus.APPROVED, noApproval)
    }

    @Test
    fun suggest_ignoresBlankDuplicateAndDisallowed() {
        assertNull(repository.suggestOption("poll_1", "   ", "user_guest_2"))
        assertNull(repository.suggestOption("poll_1", "pizza", "user_guest_2"))
        assertNull(repository.suggestOption("poll_2", "Barbie", "user_guest_2"))

        assertEquals(4, poll("poll_1").options.size)
        assertEquals(3, poll("poll_2").options.size)
    }

    @Test
    fun moderate_approveAndReject() {
        repository.moderate("poll_1", "opt_1_4", approve = true)
        assertTrue(poll("poll_1").approvedOptions.any { it.id == "opt_1_4" })
        assertTrue(poll("poll_1").pendingSuggestions.isEmpty())

        val other = PollRepository(SamplePolls.all)
        other.moderate("poll_1", "opt_1_4", approve = false)
        val rejected = other.polls.value.first { it.id == "poll_1" }.options.first { it.id == "opt_1_4" }
        assertEquals(OptionStatus.REJECTED, rejected.status)
    }

    @Test
    fun close_setsWinnerAndStopsVoting() {
        repository.close("poll_1")

        val closed = poll("poll_1")
        assertEquals(PollStatus.CLOSED, closed.status)
        assertFalse(closed.isOpen())
        assertEquals("opt_1_1", closed.winnerOptionId)

        repository.vote("poll_1", "opt_1_2")
        assertEquals(3, votes("poll_1", "opt_1_2"))
    }

    @Test
    fun close_withoutVotesHasNoWinner() {
        repository.close("poll_3")

        assertEquals(PollStatus.CLOSED, poll("poll_3").status)
        assertNull(poll("poll_3").winnerOptionId)
    }
}
