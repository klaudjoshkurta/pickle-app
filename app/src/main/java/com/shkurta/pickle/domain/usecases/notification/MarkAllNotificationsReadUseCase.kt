package com.shkurta.pickle.domain.usecases.notification

import com.shkurta.pickle.domain.repository.NotificationRepository
import javax.inject.Inject

class MarkAllNotificationsReadUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke() = repository.markAllRead()
}
