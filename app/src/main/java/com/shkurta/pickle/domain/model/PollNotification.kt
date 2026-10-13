package com.shkurta.pickle.domain.model

import java.time.Instant

data class PollNotification(
    val id: String,
    val type: NotificationType,
    val pollId: String,
    val pollQuestion: String,
    val detail: String? = null, // option text, outcome or vote count, depending on type
    val createdAt: Instant,
    val isRead: Boolean = false,
)

enum class NotificationType {
    POLL_CREATED,
    POLL_CLOSED,
    NEW_VOTES,
    SUGGESTION_RECEIVED,
    SUGGESTION_APPROVED,
    SUGGESTION_REJECTED,
}
