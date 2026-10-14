package com.shkurta.pickle.data.remote

import com.shkurta.pickle.domain.repository.NotificationRepository
import com.shkurta.pickle.domain.repository.PollRepository
import com.shkurta.pickle.domain.repository.AuthRepository
import com.shkurta.pickle.domain.model.MIN_POLL_OPTIONS
import com.shkurta.pickle.domain.model.distinctOptionTexts
import com.shkurta.pickle.data.remote.dto.CreateOptionDto
import com.shkurta.pickle.data.remote.dto.CreatePollDto
import com.shkurta.pickle.data.remote.dto.PollDto
import com.shkurta.pickle.data.remote.dto.PollOptionDto
import com.shkurta.pickle.data.remote.dto.VoteDto
import com.shkurta.pickle.domain.model.NotificationType
import com.shkurta.pickle.domain.model.OptionStatus
import com.shkurta.pickle.domain.model.Outcome
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollSettings
import com.shkurta.pickle.domain.model.toResults
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabasePollRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
) : PollRepository {

    private val _polls = MutableStateFlow<List<Poll>>(emptyList())
    override val polls: StateFlow<List<Poll>> = _polls.asStateFlow()

    private val _myVotes = MutableStateFlow<Map<String, String>>(emptyMap())
    override val myVotes: StateFlow<Map<String, String>> = _myVotes.asStateFlow()

    override suspend fun refresh() = refreshPolls()

    private suspend fun refreshPolls() {
        val result = postgrest.from("polls")
            .select(columns = Columns.raw("*, poll_options(*)")) {
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<PollDto>()

        _polls.value = result.map { it.toDomain() }

        // Also refresh my votes
        val userId = authRepository.currentUserId ?: return
        val votes = postgrest.from("votes")
            .select {
                filter { eq("voter_id", userId) }
            }
            .decodeList<VoteDto>()

        _myVotes.value = votes.associate { it.pollId to it.optionId }
    }

    override suspend fun createPoll(
        question: String,
        optionTexts: List<String>,
        settings: PollSettings,
        closesAt: java.time.Instant?,
        expectedVoters: Int?,
        creatorId: String,
    ): Poll? {
        val trimmedQuestion = question.trim()
        val texts = distinctOptionTexts(optionTexts)
        if (trimmedQuestion.isEmpty() || texts.size < MIN_POLL_OPTIONS) return null

        val pollDto = CreatePollDto(
            question = trimmedQuestion,
            creatorId = creatorId,
            allowSuggestions = settings.allowSuggestions,
            requireApproval = settings.requireApproval,
            showResultsBeforeVoting = settings.showResultsBeforeVoting,
            allowVoteChange = settings.allowVoteChange,
            closedAt = closesAt?.toString(),
            expectedVoters = expectedVoters?.takeIf { it > 0 },
        )

        val insertedPoll = postgrest.from("polls")
            .insert(pollDto) { select() }
            .decodeSingle<PollDto>()

        val options = texts.map { text ->
            CreateOptionDto(pollId = insertedPoll.id, text = text)
        }
        postgrest.from("poll_options").insert(options)

        refreshPolls()
        val poll = _polls.value.firstOrNull { it.id == insertedPoll.id }

        if (poll != null) {
            notificationRepository.add(NotificationType.POLL_CREATED, poll)
        }

        return poll
    }

    override suspend fun vote(pollId: String, optionId: String) {
        postgrest.rpc("cast_vote", buildJsonObject {
            put("p_poll_id", pollId)
            put("p_option_id", optionId)
        })
        refreshPolls()
    }

    override suspend fun suggestOption(pollId: String, text: String, userId: String): OptionStatus? {
        val poll = _polls.value.firstOrNull { it.id == pollId } ?: return null
        if (!poll.settings.allowSuggestions) return null

        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        val isDuplicate = poll.options.any {
            it.status != OptionStatus.REJECTED && it.text.equals(trimmed, ignoreCase = true)
        }
        if (isDuplicate) return null

        val isCreator = poll.isCreator(userId)
        val status = if (poll.settings.requireApproval && !isCreator) "PENDING" else "APPROVED"

        val option = CreateOptionDto(
            pollId = pollId,
            text = trimmed,
            status = status,
            suggestedBy = userId.takeUnless { isCreator },
        )
        postgrest.from("poll_options").insert(option)

        refreshPolls()

        val optionStatus = OptionStatus.valueOf(status)
        if (optionStatus == OptionStatus.PENDING) {
            notificationRepository.add(NotificationType.SUGGESTION_RECEIVED, poll, trimmed)
        }
        return optionStatus
    }

    override suspend fun moderate(pollId: String, optionId: String, approve: Boolean) {
        val poll = _polls.value.firstOrNull { it.id == pollId } ?: return
        val suggestion = poll.pendingSuggestions.firstOrNull { it.id == optionId } ?: return
        val newStatus = if (approve) "APPROVED" else "REJECTED"

        postgrest.from("poll_options").update({
            set("status", newStatus)
        }) {
            filter { eq("id", optionId) }
        }

        refreshPolls()

        notificationRepository.add(
            type = if (approve) NotificationType.SUGGESTION_APPROVED else NotificationType.SUGGESTION_REJECTED,
            poll = poll,
            detail = suggestion.text,
        )
    }

    override suspend fun close(pollId: String) {
        val poll = _polls.value.firstOrNull { it.id == pollId } ?: return
        val outcome = poll.toResults().outcome

        postgrest.rpc("close_poll", buildJsonObject {
            put("p_poll_id", pollId)
        })

        refreshPolls()

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
}
