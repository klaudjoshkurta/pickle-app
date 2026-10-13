package com.shkurta.pickle.data

import com.shkurta.pickle.data.sample.SamplePolls
import com.shkurta.pickle.domain.model.NotificationType
import com.shkurta.pickle.domain.model.OptionStatus
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollSettings
import com.shkurta.pickle.domain.model.PollStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class PollRepositoryTest {

    private val repository = PollRepository(SamplePolls.all)

    private fun poll(id: String): Poll = repository.polls.value.first { it.id == id }

    private fun votes(pollId: String, optionId: String): Int =
        poll(pollId).options.first { it.id == optionId }.voteCount

    @Test
    fun createPoll_prependsOpenPollWithCleanedInput() {
        val closesAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val settings = PollSettings(allowSuggestions = true, allowVoteChange = false)

        val created = repository.createPoll(
            question = "  Lunch?  ",
            optionTexts = listOf(" Gyros ", "", "gyros", "Salad"),
            settings = settings,
            closesAt = closesAt,
            expectedVoters = 5,
            creatorId = SamplePolls.CREATOR_ID,
        )!!

        assertEquals(created, repository.polls.value.first())
        assertEquals(4, repository.polls.value.size)
        assertEquals("Lunch?", created.question)
        assertEquals(listOf("Gyros", "Salad"), created.approvedOptions.map { it.text })
        assertEquals(settings, created.settings)
        assertEquals(closesAt, created.closedAt)
        assertEquals(5, created.expectedVoters)
        assertTrue(created.isOpen())
        assertTrue(created.isCreator(SamplePolls.CREATOR_ID))
    }

    @Test
    fun createPoll_rejectsBlankQuestionAndTooFewOptions() {
        val blankQuestion = repository.createPoll(
            " ", listOf("A", "B"), PollSettings(), null, null, SamplePolls.CREATOR_ID,
        )
        val oneDistinctOption = repository.createPoll(
            "Lunch?", listOf("A", "a ", ""), PollSettings(), null, null, SamplePolls.CREATOR_ID,
        )

        assertNull(blankQuestion)
        assertNull(oneDistinctOption)
        assertEquals(3, repository.polls.value.size)
    }

    @Test
    fun createPoll_zeroExpectedVotersIsUnknown() {
        val created = repository.createPoll(
            "Lunch?", listOf("A", "B"), PollSettings(), null, 0, SamplePolls.CREATOR_ID,
        )!!

        assertNull(created.expectedVoters)
        assertNull(created.closedAt)
    }

    @Test
    fun notifications_emittedForCreateSuggestModerateAndClose() {
        val notifications = NotificationRepository(emptyList())
        val repository = PollRepository(SamplePolls.all, notifications)

        repository.createPoll(
            "Lunch?", listOf("A", "B"), PollSettings(), null, null, SamplePolls.CREATOR_ID,
        )
        repository.suggestOption("poll_1", "Pasta", "user_guest_2")
        repository.moderate("poll_1", "opt_1_4", approve = true)
        repository.close("poll_1")
        repository.close("poll_3")

        val emitted = notifications.notifications.value.reversed()
        assertEquals(
            listOf(
                NotificationType.POLL_CREATED to null,
                NotificationType.SUGGESTION_RECEIVED to "Pasta",
                NotificationType.SUGGESTION_APPROVED to "Tacos",
                NotificationType.POLL_CLOSED to "Winner: Pizza",
                NotificationType.POLL_CLOSED to "No votes",
            ),
            emitted.map { it.type to it.detail },
        )
        assertEquals("Lunch?", emitted.first().pollQuestion)
        assertEquals("poll_1", emitted[1].pollId)
    }

    @Test
    fun notifications_notEmittedForVotesOrIgnoredActions() {
        val notifications = NotificationRepository(emptyList())
        val repository = PollRepository(SamplePolls.all, notifications)

        repository.vote("poll_1", "opt_1_2")
        repository.createPoll(" ", listOf("A", "B"), PollSettings(), null, null, SamplePolls.CREATOR_ID)
        repository.suggestOption("poll_1", "Pasta", SamplePolls.CREATOR_ID)
        repository.suggestOption("poll_1", "pizza", "user_guest_2")
        repository.moderate("poll_1", "opt_1_1", approve = false)
        repository.close("poll_2")

        assertTrue(notifications.notifications.value.isEmpty())
    }

    @Test
    fun notifications_rejectedSuggestion() {
        val notifications = NotificationRepository(emptyList())
        val repository = PollRepository(SamplePolls.all, notifications)

        repository.moderate("poll_1", "opt_1_4", approve = false)

        val emitted = notifications.notifications.value.single()
        assertEquals(NotificationType.SUGGESTION_REJECTED, emitted.type)
        assertEquals("Tacos", emitted.detail)
    }

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
