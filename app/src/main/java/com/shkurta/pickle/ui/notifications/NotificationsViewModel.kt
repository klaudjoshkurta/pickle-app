package com.shkurta.pickle.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.domain.model.PollNotification
import com.shkurta.pickle.domain.usecases.notification.MarkAllNotificationsReadUseCase
import com.shkurta.pickle.domain.usecases.notification.MarkNotificationReadUseCase
import com.shkurta.pickle.domain.usecases.notification.ObserveNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(val notifications: List<PollNotification> = emptyList()) {
    val hasUnread: Boolean get() = notifications.any { !it.isRead }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    observeNotifications: ObserveNotificationsUseCase,
    private val markNotificationRead: MarkNotificationReadUseCase,
    private val markAllNotificationsRead: MarkAllNotificationsReadUseCase,
) : ViewModel() {
    val uiState = observeNotifications().map { NotificationsUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
            NotificationsUiState(observeNotifications().value))

    fun markRead(id: String) { viewModelScope.launch { markNotificationRead(id) } }
    fun markAllRead() { viewModelScope.launch { markAllNotificationsRead() } }
}
