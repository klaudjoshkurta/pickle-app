package com.shkurta.pickle.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.data.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    val notifications = notificationRepository.notifications

    fun markRead(id: String) {
        viewModelScope.launch { notificationRepository.markRead(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { notificationRepository.markAllRead() }
    }
}
