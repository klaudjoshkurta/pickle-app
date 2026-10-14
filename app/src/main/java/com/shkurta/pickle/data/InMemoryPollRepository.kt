package com.shkurta.pickle.data

import com.shkurta.pickle.domain.repository.PollRepository
import com.shkurta.pickle.domain.repository.NotificationRepository
import com.shkurta.pickle.domain.model.MIN_POLL_OPTIONS
import com.shkurta.pickle.domain.model.distinctOptionTexts

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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryPollRepository @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : PollRepository {

    private val _polls = MutableStateFlow(SamplePolls.all)
    override val polls: StateFlow<List<Poll>> = _polls.asStateFlow()

    private val _myVotes = MutableStateFlow<Map<String, String>>(emptyMap())
    override val myVotes: StateFlow<Map<String, String>> = _myVotes.asStateFlow()

    override suspend fun refresh() { /* no-op for in-memory */ }

    override suspend fun createPoll(
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
        notificationRepository.add(NotificationType.POLL_CREATED, poll)
        return poll
    }

    override suspend fun vote(pollId: String, optionId: String) {
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

    override suspend fun suggestOption(pollId: String, text: String, userId: String): OptionStatus? {
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
            notificationRepository.add(NotificationType.SUGGESTION_RECEIVED, poll, trimmed)
        }
        return status
    }

    override suspend fun moderate(pollId: String, optionId: String, approve: Boolean) {
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
        notificationRepository.add(
            type = if (approve) NotificationType.SUGGESTION_APPROVED else NotificationType.SUGGESTION_REJECTED,
            poll = poll,
            detail = suggestion.text,
        )
    }

    override suspend fun close(pollId: String) {
        val poll = openPoll(pollId) ?: return
        val outcome = poll.toResults().outcome
        updatePoll(pollId) {
            it.copy(
                status = PollStatus.CLOSED,
                closedAt = Instant.now(),
                winnerOptionId = (outcome as? Outcome.Winner)?.option?.id,
            )
        }
        notificationRepository.add(
            type = NotificationType.POLL_CLOSED,
            poll = poll,
            detail = when (outcome) {
                Outcome.NoVotes -> "No votes"
                is Outcome.Winner -> "Winner: ${outcome.option.text}"
                is Outcome.Tie -> "Tie: ${outcome.options.joinToString(" · ") { it.text }}"
            },
        )
    }

    private fun openPoll(pollId: String): Poll? =
        _polls.value.firstOrNull { it.id == pollId }?.takeIf { it.isOpen() }

    private fun updatePoll(pollId: String, transform: (Poll) -> Poll) {
        _polls.update { polls -> polls.map { if (it.id == pollId) transform(it) else it } }
    }
}
