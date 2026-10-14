package com.shkurta.pickle.data.remote

import com.shkurta.pickle.domain.repository.NotificationRepository
import com.shkurta.pickle.domain.repository.AuthRepository
import com.shkurta.pickle.data.remote.dto.CreateNotificationDto
import com.shkurta.pickle.data.remote.dto.NotificationDto
import com.shkurta.pickle.domain.model.NotificationType
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollNotification
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseNotificationRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val authRepository: AuthRepository,
) : NotificationRepository {

    private val _notifications = MutableStateFlow<List<PollNotification>>(emptyList())
    override val notifications: StateFlow<List<PollNotification>> = _notifications.asStateFlow()

    override suspend fun refresh() {
        val userId = authRepository.currentUserId ?: return
        val result = postgrest.from("notifications")
            .select {
                filter { eq("user_id", userId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<NotificationDto>()

        _notifications.value = result.map { it.toDomain() }
    }

    override suspend fun add(type: NotificationType, poll: Poll, detail: String?) {
        val userId = authRepository.currentUserId ?: return
        val dto = CreateNotificationDto(
            userId = userId,
            type = type.name,
            pollId = poll.id,
            pollQuestion = poll.question,
            detail = detail,
        )
        postgrest.from("notifications").insert(dto)
        refresh()
    }

    override suspend fun markRead(id: String) {
        postgrest.from("notifications").update({
            set("is_read", true)
        }) {
            filter { eq("id", id) }
        }
        refresh()
    }

    override suspend fun markAllRead() {
        val userId = authRepository.currentUserId ?: return
        postgrest.from("notifications").update({
            set("is_read", true)
        }) {
            filter {
                eq("user_id", userId)
                eq("is_read", false)
            }
        }
        refresh()
    }
}
