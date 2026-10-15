package com.shkurta.pickle.domain.model

data class OptionResult(
    val option: PollOption,
    val percentage: Float, // 0f...1f, handy for animated progress bars
    val isLeading: Boolean,
)

data class PollResults(
    val totalVotes: Int,
    val options: List<OptionResult>,
    val outcome: Outcome,
    val votingMode: VotingMode = VotingMode.SINGLE,
    val totalSelections: Int = totalVotes,
)

sealed interface Outcome {
    data object NoVotes : Outcome
    data class Winner(val option: PollOption) : Outcome
    data class Tie(val options: List<PollOption>) : Outcome
}

fun Poll.toResults(): PollResults {
    val total = totalVotes
    val maxVotes = approvedOptions.maxOfOrNull { it.voteCount } ?: 0
    val leaders = approvedOptions.filter { it.voteCount == maxVotes && maxVotes > 0 }

    val outcome = when {
        total == 0 -> Outcome.NoVotes
        leaders.size == 1 -> Outcome.Winner(leaders.first())
        else -> Outcome.Tie(leaders)
    }

    return PollResults(
        totalVotes = total,
        votingMode = settings.votingMode,
        totalSelections = totalSelections,
        options = approvedOptions
            .sortedByDescending { it.voteCount }
            .map { option ->
                OptionResult(
                    option = option,
                    percentage = if (total == 0) 0f else option.voteCount / total.toFloat(),
                    isLeading = option in leaders,
                )
            },
        outcome = outcome,
    )
}