package com.shkurta.pickle.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.shkurta.pickle.data.sample.SampleNotifications
import com.shkurta.pickle.domain.model.NotificationType
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.UUID

/** In-memory notification store, newest first. State is lost when the process dies. */
class NotificationRepository(initialNotifications: List<PollNotification>) {

    private val _notifications = MutableStateFlow(initialNotifications)
    val notifications: StateFlow<List<PollNotification>> = _notifications.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun add(type: NotificationType, poll: Poll, detail: String? = null) {
        val notification = PollNotification(
            id = "notif_${UUID.randomUUID()}",
            type = type,
            pollId = poll.id,
            pollQuestion = poll.question,
            detail = detail,
            createdAt = Instant.now(),
        )
        _notifications.update { listOf(notification) + it }
    }

    fun markRead(id: String) {
        _notifications.update { notifications ->
            notifications.map { if (it.id == id) it.copy(isRead = true) else it }
        }
    }

    fun markAllRead() {
        _notifications.update { notifications -> notifications.map { it.copy(isRead = true) } }
    }

    companion object {
        @get:RequiresApi(Build.VERSION_CODES.O)
        val Default: NotificationRepository by lazy { NotificationRepository(SampleNotifications.all) }
    }
}
