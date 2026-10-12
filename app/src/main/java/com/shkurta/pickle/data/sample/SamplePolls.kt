package com.shkurta.pickle.data.sample

import android.os.Build
import androidx.annotation.RequiresApi
import com.shkurta.pickle.domain.model.OptionStatus
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollOption
import com.shkurta.pickle.domain.model.PollSettings
import com.shkurta.pickle.domain.model.PollStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

object SamplePolls {

    @RequiresApi(Build.VERSION_CODES.O)
    private val now: Instant = Instant.now()
    private const val CREATOR_ID = "user_creator"

    // 1. Has votes – clear winner (Pizza), plus a pending suggestion to moderate
    @RequiresApi(Build.VERSION_CODES.O)
    val pizzaNight = Poll(
        id = "poll_1",
        question = "What should we order for pizza night?",
        creatorId = CREATOR_ID,
        options = listOf(
            PollOption(
                id = "opt_1_1",
                text = "Pizza",
                status = OptionStatus.APPROVED,
                voteCount = 5,
                createdAt = now.minus(2, ChronoUnit.HOURS),
            ),
            PollOption(
                id = "opt_1_2",
                text = "Sushi",
                status = OptionStatus.APPROVED,
                voteCount = 3,
                createdAt = now.minus(2, ChronoUnit.HOURS),
            ),
            PollOption(
                id = "opt_1_3",
                text = "Burgers",
                status = OptionStatus.APPROVED,
                voteCount = 1,
                createdAt = now.minus(2, ChronoUnit.HOURS),
            ),
            PollOption(
                id = "opt_1_4",
                text = "Tacos",
                status = OptionStatus.PENDING,
                suggestedBy = "user_guest_1",
                createdAt = now.minus(30, ChronoUnit.MINUTES),
            ),
        ),
        settings = PollSettings(allowSuggestions = true, requireApproval = true),
        status = PollStatus.OPEN,
        createdAt = now.minus(2, ChronoUnit.HOURS),
        closedAt = now.plus(1, ChronoUnit.DAYS),
        expectedVoters = 10
    )

    // 2. Has votes – a tie between two options, and the poll is closed
    @RequiresApi(Build.VERSION_CODES.O)
    val movieNight = Poll(
        id = "poll_2",
        question = "Which movie are we watching on Saturday?",
        creatorId = CREATOR_ID,
        options = listOf(
            PollOption(
                id = "opt_2_1",
                text = "Dune: Part Two",
                status = OptionStatus.APPROVED,
                voteCount = 4,
                createdAt = now.minus(3, ChronoUnit.DAYS),
            ),
            PollOption(
                id = "opt_2_2",
                text = "Oppenheimer",
                status = OptionStatus.APPROVED,
                voteCount = 4,
                createdAt = now.minus(3, ChronoUnit.DAYS),
            ),
            PollOption(
                id = "opt_2_3",
                text = "Inside Out 2",
                status = OptionStatus.APPROVED,
                voteCount = 2,
                createdAt = now.minus(3, ChronoUnit.DAYS),
            ),
        ),
        settings = PollSettings(allowSuggestions = false),
        status = PollStatus.CLOSED,
        createdAt = now.minus(3, ChronoUnit.DAYS),
        closedAt = now.minus(1, ChronoUnit.DAYS),
        expectedVoters = 12
    )

    // 3. No votes yet – freshly created
    @RequiresApi(Build.VERSION_CODES.O)
    val weekendTrip = Poll(
        id = "poll_3",
        question = "Where should we go for the weekend trip?",
        creatorId = CREATOR_ID,
        options = listOf(
            PollOption(
                id = "opt_3_1",
                text = "Nafplio",
                status = OptionStatus.APPROVED,
                createdAt = now.minus(5, ChronoUnit.MINUTES),
            ),
            PollOption(
                id = "opt_3_2",
                text = "Hydra",
                status = OptionStatus.APPROVED,
                createdAt = now.minus(5, ChronoUnit.MINUTES),
            ),
            PollOption(
                id = "opt_3_3",
                text = "Delphi",
                status = OptionStatus.APPROVED,
                createdAt = now.minus(5, ChronoUnit.MINUTES),
            ),
        ),
        settings = PollSettings(allowSuggestions = true, requireApproval = false),
        status = PollStatus.OPEN,
        createdAt = now.minus(5, ChronoUnit.MINUTES),
        expectedVoters = 4
    )

    @RequiresApi(Build.VERSION_CODES.O)
    val all: List<Poll> = listOf(pizzaNight, movieNight, weekendTrip)
}