package com.shkurta.pickle.data.remote.dto

import com.shkurta.pickle.domain.model.NotificationType
import com.shkurta.pickle.domain.model.PollNotification
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    @SerialName("poll_id") val pollId: String,
    @SerialName("poll_question") val pollQuestion: String,
    val detail: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_read") val isRead: Boolean = false,
) {
    fun toDomain(): PollNotification = PollNotification(
        id = id,
        type = NotificationType.valueOf(type),
        pollId = pollId,
        pollQuestion = pollQuestion,
        detail = detail,
        createdAt = Instant.parse(createdAt),
        isRead = isRead,
    )
}

@Serializable
data class CreateNotificationDto(
    @SerialName("user_id") val userId: String,
    val type: String,
    @SerialName("poll_id") val pollId: String,
    @SerialName("poll_question") val pollQuestion: String,
    val detail: String? = null,
)
