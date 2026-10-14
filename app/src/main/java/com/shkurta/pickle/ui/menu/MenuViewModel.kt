package com.shkurta.pickle.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.domain.usecases.auth.GetCurrentUserUseCase
import com.shkurta.pickle.domain.usecases.auth.ObserveAuthenticationUseCase
import com.shkurta.pickle.domain.usecases.auth.SignOutUseCase
import com.shkurta.pickle.domain.usecases.notification.ObserveNotificationsUseCase
import com.shkurta.pickle.domain.usecases.poll.ObservePollsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuUiState(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val createdCount: Int = 0,
    val openCount: Int = 0,
    val voteCount: Int = 0,
    val unreadCount: Int = 0,
)

@HiltViewModel
class MenuViewModel @Inject constructor(
    observePolls: ObservePollsUseCase,
    observeNotifications: ObserveNotificationsUseCase,
    observeAuthentication: ObserveAuthenticationUseCase,
    getCurrentUser: GetCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {
    val uiState = combine(observePolls(), observeNotifications(), observeAuthentication()) { polls, notifications, _ ->
        val user = getCurrentUser()
        val myPolls = polls.filter { it.isCreator(user.userId ?: "") }
        MenuUiState(
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            createdCount = myPolls.size,
            openCount = myPolls.count { it.isOpen() },
            voteCount = myPolls.sumOf { it.totalVotes },
            unreadCount = notifications.count { !it.isRead },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuUiState(
        displayName = getCurrentUser().displayName,
        avatarUrl = getCurrentUser().avatarUrl,
    ))

    fun signOut() { viewModelScope.launch { signOutUseCase() } }
}
