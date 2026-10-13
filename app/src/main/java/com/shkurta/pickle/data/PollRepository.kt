package com.shkurta.pickle.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.shkurta.pickle.data.sample.SamplePolls
import com.shkurta.pickle.domain.model.NotificationType
import com.shkurta.pickle.domain.model.OptionStatus
import com.shkurta.pickle.domain.model.Outcome
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollOption
import com.shkurta.pickle.domain.model.PollSettings
import com.shkurta.pickle.domain.model.PollStatus
import com.shkurta.pickle.domain.model.toResults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.UUID

internal const val MIN_POLL_OPTIONS = 2

/** Trimmed, non-blank option texts with case-insensitive duplicates removed. */
internal fun distinctOptionTexts(texts: List<String>): List<String> =
    texts.map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }

/** In-memory poll store. State is lost when the process dies. */
class PollRepository(
    initialPolls: List<Poll>,
    private val notifications: NotificationRepository? = null,
) {

    private val _polls = MutableStateFlow(initialPolls)
    val polls: StateFlow<List<Poll>> = _polls.asStateFlow()

    /** pollId -> optionId the current user voted for. */
    private val _myVotes = MutableStateFlow<Map<String, String>>(emptyMap())
    val myVotes: StateFlow<Map<String, String>> = _myVotes.asStateFlow()

    /** Returns the created poll, or null when the question is blank or too few options remain. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createPoll(
        question: String,
        optionTexts: List<String>,
        settings: PollSettings,
        closesAt: Instant?,
        expectedVoters: Int?,
        creatorId: String,
    ): Poll? {
        val trimmedQuestion = question.trim()
        val texts = distinctOptionTexts(optionTexts)
        if (trimmedQuestion.isEmpty() || texts.size < MIN_POLL_OPTIONS) return null

        val now = Instant.now()
        val poll = Poll(
            id = "poll_${UUID.randomUUID()}",
            question = trimmedQuestion,
            creatorId = creatorId,
            options = texts.map { text ->
                PollOption(
                    id = "opt_${UUID.randomUUID()}",
                    text = text,
                    status = OptionStatus.APPROVED,
                    createdAt = now,
                )
            },
            settings = settings,
            status = PollStatus.OPEN,
            createdAt = now,
            closedAt = closesAt,
            expectedVoters = expectedVoters?.takeIf { it > 0 },
        )
        _polls.update { listOf(poll) + it }
        notifications?.add(NotificationType.POLL_CREATED, poll)
        return poll
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun vote(pollId: String, optionId: String) {
        val poll = openPoll(pollId) ?: return
        if (poll.approvedOptions.none { it.id == optionId }) return

        val previous = _myVotes.value[pollId]
        if (previous == optionId) return
        if (previous != null && !poll.settings.allowVoteChange) return

        updatePoll(pollId) {
            it.copy(
                options = it.options.map { option ->
                    when (option.id) {
                        optionId -> option.copy(voteCount = option.voteCount + 1)
                        previous -> option.copy(voteCount = (option.voteCount - 1).coerceAtLeast(0))
                        else -> option
                    }
                },
            )
        }
        _myVotes.update { it + (pollId to optionId) }
    }

    /** Returns the status the new option got, or null when the suggestion was ignored. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun suggestOption(pollId: String, text: String, userId: String): OptionStatus? {
        val poll = openPoll(pollId) ?: return null
        if (!poll.settings.allowSuggestions) return null

        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val isDuplicate = poll.options.any {
            it.status != OptionStatus.REJECTED && it.text.equals(trimmed, ignoreCase = true)
        }
        if (isDuplicate) return null

        val isCreator = poll.isCreator(userId)
        val status = if (poll.settings.requireApproval && !isCreator) {
            OptionStatus.PENDING
        } else {
            OptionStatus.APPROVED
        }
        val option = PollOption(
            id = "opt_${UUID.randomUUID()}",
            text = trimmed,
            status = status,
            suggestedBy = userId.takeUnless { isCreator },
            createdAt = Instant.now(),
        )
        updatePoll(pollId) { it.copy(options = it.options + option) }
        if (status == OptionStatus.PENDING) {
            notifications?.add(NotificationType.SUGGESTION_RECEIVED, poll, trimmed)
        }
        return status
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun moderate(pollId: String, optionId: String, approve: Boolean) {
        val poll = openPoll(pollId) ?: return
        val suggestion = poll.pendingSuggestions.firstOrNull { it.id == optionId } ?: return
        val newStatus = if (approve) OptionStatus.APPROVED else OptionStatus.REJECTED
        updatePoll(pollId) {
            it.copy(
                options = it.options.map { option ->
                    if (option.id == optionId) option.copy(status = newStatus) else option
                },
            )
        }
        notifications?.add(
            type = if (approve) {
                NotificationType.SUGGESTION_APPROVED
            } else {
                NotificationType.SUGGESTION_REJECTED
            },
            poll = poll,
            detail = suggestion.text,
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun close(pollId: String) {
        val poll = openPoll(pollId) ?: return
        val outcome = poll.toResults().outcome
        updatePoll(pollId) {
            it.copy(
                status = PollStatus.CLOSED,
                closedAt = Instant.now(),
                winnerOptionId = (outcome as? Outcome.Winner)?.option?.id,
            )
        }
        notifications?.add(
            type = NotificationType.POLL_CLOSED,
            poll = poll,
            detail = when (outcome) {
                Outcome.NoVotes -> "No votes"
                is Outcome.Winner -> "Winner: ${outcome.option.text}"
                is Outcome.Tie -> "Tie: ${outcome.options.joinToString(" · ") { it.text }}"
            },
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openPoll(pollId: String): Poll? =
        _polls.value.firstOrNull { it.id == pollId }?.takeIf { it.isOpen() }

    private fun updatePoll(pollId: String, transform: (Poll) -> Poll) {
        _polls.update { polls -> polls.map { if (it.id == pollId) transform(it) else it } }
    }

    companion object {
        @get:RequiresApi(Build.VERSION_CODES.O)
        val Default: PollRepository by lazy {
            PollRepository(SamplePolls.all, NotificationRepository.Default)
        }
    }
}
