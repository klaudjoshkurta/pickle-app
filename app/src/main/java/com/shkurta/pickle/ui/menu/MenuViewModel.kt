package com.shkurta.pickle.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.data.NotificationRepository
import com.shkurta.pickle.data.PollRepository
import com.shkurta.pickle.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    pollRepository: PollRepository,
    notificationRepository: NotificationRepository,
) : ViewModel() {

    val currentUserId: String get() = authRepository.currentUserId ?: ""

    val polls = pollRepository.polls
    val notifications = notificationRepository.notifications

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
