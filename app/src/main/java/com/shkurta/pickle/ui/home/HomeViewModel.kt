package com.shkurta.pickle.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.data.NotificationRepository
import com.shkurta.pickle.data.PollRepository
import com.shkurta.pickle.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pollRepository: PollRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    val polls = pollRepository.polls
    val notifications = notificationRepository.notifications

    val displayName: String? get() = authRepository.displayName
    val avatarUrl: String? get() = authRepository.avatarUrl

    init {
        viewModelScope.launch {
            pollRepository.refresh()
            notificationRepository.refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            pollRepository.refresh()
            notificationRepository.refresh()
        }
    }
}
