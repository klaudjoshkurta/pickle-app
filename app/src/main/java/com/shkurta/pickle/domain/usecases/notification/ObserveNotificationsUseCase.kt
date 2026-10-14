package com.shkurta.pickle.domain.usecases.notification

import com.shkurta.pickle.domain.repository.NotificationRepository
import javax.inject.Inject

class ObserveNotificationsUseCase @Inject constructor(private val repository: NotificationRepository) {
    operator fun invoke() = repository.notifications
}
