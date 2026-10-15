package com.shkurta.pickle.domain.model

import java.time.Instant

data class Poll(
    val id: String,
    val question: String,
    val creatorId: String,
    val options: List<PollOption>,
    val settings: PollSettings,
    val status: PollStatus,
    val createdAt: Instant,
    val closedAt: Instant? = null,
    val winnerOptionId: String? = null,
    val voterCount: Int? = null,
    val templateId: String? = null,
    val templateRound: Int? = null,
    val expectedVoters: Int? = null, // e.g. 4 people in the group chat; null = unknown
) {
    /** Options voters can actually see and vote on. */
    val approvedOptions: List<PollOption>
        get() = options.filter { it.status == OptionStatus.APPROVED }

    /** Suggestions waiting for the creator to moderate. */
    val pendingSuggestions: List<PollOption>
        get() = options.filter { it.status == OptionStatus.PENDING }

    val totalVotes: Int
        get() = voterCount ?: totalSelections

    val totalSelections: Int get() = approvedOptions.sumOf { it.voteCount }

    /** 0f..1f, or null when the group size is unknown. */
    val turnout: Float?
        get() = expectedVoters
            ?.takeIf { it > 0 }
            ?.let { (totalVotes / it.toFloat()).coerceIn(0f, 1f) }

    fun isOpen(now: Instant = Instant.now()): Boolean = status == PollStatus.OPEN && (closedAt == null || now.isBefore(closedAt))

    fun isCreator(userId: String) = creatorId == userId
}

data class PollOption(
    val id: String,
    val text: String,
    val status: OptionStatus,
    val voteCount: Int = 0,
    val suggestedBy: String? = null, // null = added by creator
    val createdAt: Instant
)

enum class VotingMode { SINGLE, MULTIPLE }

data class PollSettings(
    val votingMode: VotingMode = VotingMode.SINGLE,
    val allowSuggestions: Boolean = false,
    val requireApproval: Boolean = true,   // suggestions go to PENDING first
    val showResultsBeforeVoting: Boolean = false,
    val allowVoteChange: Boolean = true,
)

data class Vote(
    val pollId: String,
    val voterId: String,   // anonymous device/user id
    val optionId: String,
    val createdAt: Instant,
)

enum class PollStatus { OPEN, CLOSED }

enum class OptionStatus { APPROVED, PENDING, REJECTED }
