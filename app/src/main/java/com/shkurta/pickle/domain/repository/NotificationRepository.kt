package com.shkurta.pickle.domain.repository

import com.shkurta.pickle.domain.model.NotificationType
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollNotification
import kotlinx.coroutines.flow.StateFlow

interface NotificationRepository {
    val notifications: StateFlow<List<PollNotification>>

    suspend fun refresh()
    suspend fun add(type: NotificationType, poll: Poll, detail: String? = null)
    suspend fun markRead(id: String)
    suspend fun markAllRead()
}
