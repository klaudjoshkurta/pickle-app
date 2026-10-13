package com.shkurta.pickle.data.sample

import android.os.Build
import androidx.annotation.RequiresApi
import com.shkurta.pickle.domain.model.NotificationType
import com.shkurta.pickle.domain.model.PollNotification
import java.time.Instant
import java.time.temporal.ChronoUnit

object SampleNotifications {

    @RequiresApi(Build.VERSION_CODES.O)
    private val now: Instant = Instant.now()

    // Newest first
    @RequiresApi(Build.VERSION_CODES.O)
    val all: List<PollNotification> = listOf(
        PollNotification(
            id = "notif_1",
            type = NotificationType.POLL_CREATED,
            pollId = SamplePolls.weekendTrip.id,
            pollQuestion = SamplePolls.weekendTrip.question,
            createdAt = now.minus(5, ChronoUnit.MINUTES),
            isRead = true,
        ),
        PollNotification(
            id = "notif_2",
            type = NotificationType.SUGGESTION_RECEIVED,
            pollId = SamplePolls.pizzaNight.id,
            pollQuestion = SamplePolls.pizzaNight.question,
            detail = "Tacos",
            createdAt = now.minus(30, ChronoUnit.MINUTES),
        ),
        PollNotification(
            id = "notif_3",
            type = NotificationType.NEW_VOTES,
            pollId = SamplePolls.pizzaNight.id,
            pollQuestion = SamplePolls.pizzaNight.question,
            detail = "3",
            createdAt = now.minus(1, ChronoUnit.HOURS),
        ),
        PollNotification(
            id = "notif_4",
            type = NotificationType.POLL_CLOSED,
            pollId = SamplePolls.movieNight.id,
            pollQuestion = SamplePolls.movieNight.question,
            detail = "Tie: Dune: Part Two · Oppenheimer",
            createdAt = now.minus(1, ChronoUnit.DAYS),
            isRead = true,
        ),
        PollNotification(
            id = "notif_5",
            type = NotificationType.NEW_VOTES,
            pollId = SamplePolls.movieNight.id,
            pollQuestion = SamplePolls.movieNight.question,
            detail = "10",
            createdAt = now.minus(2, ChronoUnit.DAYS),
            isRead = true,
        ),
        PollNotification(
            id = "notif_6",
            type = NotificationType.POLL_CREATED,
            pollId = SamplePolls.movieNight.id,
            pollQuestion = SamplePolls.movieNight.question,
            createdAt = now.minus(3, ChronoUnit.DAYS),
            isRead = true,
        ),
    )
}
