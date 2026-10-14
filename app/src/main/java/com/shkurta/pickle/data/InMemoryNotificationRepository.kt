package com.shkurta.pickle.data

import com.shkurta.pickle.domain.repository.NotificationRepository

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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryNotificationRepository @Inject constructor() : NotificationRepository {

    private val _notifications = MutableStateFlow(SampleNotifications.all)
    override val notifications: StateFlow<List<PollNotification>> = _notifications.asStateFlow()

    override suspend fun refresh() { /* no-op for in-memory */ }

    override suspend fun add(type: NotificationType, poll: Poll, detail: String?) {
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

    override suspend fun markRead(id: String) {
        _notifications.update { notifications ->
            notifications.map { if (it.id == id) it.copy(isRead = true) else it }
        }
    }

    override suspend fun markAllRead() {
        _notifications.update { notifications -> notifications.map { it.copy(isRead = true) } }
    }
}
