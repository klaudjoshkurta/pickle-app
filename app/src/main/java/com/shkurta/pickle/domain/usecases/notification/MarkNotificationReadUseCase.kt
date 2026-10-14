package com.shkurta.pickle.domain.usecases.notification

import com.shkurta.pickle.domain.repository.NotificationRepository
import javax.inject.Inject

class MarkNotificationReadUseCase @Inject constructor(private val repository: NotificationRepository) {
    suspend operator fun invoke(id: String) = repository.markRead(id)
}
