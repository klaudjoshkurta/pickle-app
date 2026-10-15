package com.shkurta.pickle.data.remote.dto

import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollSettings
import com.shkurta.pickle.domain.model.PollStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class PollDto(
    val id: String,
    val question: String,
    @SerialName("creator_id") val creatorId: String,
    @SerialName("allow_suggestions") val allowSuggestions: Boolean,
    @SerialName("require_approval") val requireApproval: Boolean,
    @SerialName("show_results_before_voting") val showResultsBeforeVoting: Boolean,
    @SerialName("allow_vote_change") val allowVoteChange: Boolean,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("closed_at") val closedAt: String? = null,
    @SerialName("winner_option_id") val winnerOptionId: String? = null,
    @SerialName("expected_voters") val expectedVoters: Int? = null,
    @SerialName("poll_options") val pollOptions: List<PollOptionDto> = emptyList(),
) {
    fun toDomain(): Poll = Poll(
        id = id,
        question = question,
        creatorId = creatorId,
        options = pollOptions.map { it.toDomain() },
        settings = PollSettings(
            allowSuggestions = allowSuggestions,
            requireApproval = requireApproval,
            showResultsBeforeVoting = showResultsBeforeVoting,
            allowVoteChange = allowVoteChange,
        ),
        status = PollStatus.valueOf(status),
        createdAt = parseDatabaseInstant(createdAt),
        closedAt = closedAt?.let { parseDatabaseInstant(it) },
        winnerOptionId = winnerOptionId,
        expectedVoters = expectedVoters,
    )
}

@Serializable
data class CreatePollDto(
    val question: String,
    @SerialName("creator_id") val creatorId: String,
    @SerialName("allow_suggestions") val allowSuggestions: Boolean,
    @SerialName("require_approval") val requireApproval: Boolean,
    @SerialName("show_results_before_voting") val showResultsBeforeVoting: Boolean,
    @SerialName("allow_vote_change") val allowVoteChange: Boolean,
    @SerialName("closed_at") val closedAt: String? = null,
    @SerialName("expected_voters") val expectedVoters: Int? = null,
)
