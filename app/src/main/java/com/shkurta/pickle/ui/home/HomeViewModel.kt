package com.shkurta.pickle.ui.home

import com.shkurta.pickle.domain.model.enumOrDefault
import com.shkurta.pickle.ui.components.ActionRunner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.usecases.auth.GetCurrentUserUseCase
import com.shkurta.pickle.domain.usecases.auth.ObserveAuthenticationUseCase
import com.shkurta.pickle.domain.usecases.notification.ObserveNotificationsUseCase
import com.shkurta.pickle.domain.usecases.notification.RefreshNotificationsUseCase
import com.shkurta.pickle.domain.usecases.poll.ObservePollsUseCase
import com.shkurta.pickle.domain.usecases.poll.RefreshPollsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class HomeFilter(val label: Int, val sectionLabel: Int) {
    MY_POLLS(com.shkurta.pickle.R.string.my_polls, com.shkurta.pickle.R.string.live_now),
    CLOSED(com.shkurta.pickle.R.string.closed, com.shkurta.pickle.R.string.settled),
    TEMPLATES(com.shkurta.pickle.R.string.templates, com.shkurta.pickle.R.string.templates),
}

data class HomeUiState(
    val isDemo: Boolean = false,
    val filter: HomeFilter = HomeFilter.MY_POLLS,
    val polls: List<Poll> = emptyList(),
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val settledPolls: List<Poll> = emptyList(),
    val hasAnyPolls: Boolean = false,
    val hasLoaded: Boolean = false,
    val hasUnreadNotifications: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observePolls: ObservePollsUseCase,
    observeNotifications: ObserveNotificationsUseCase,
    observeAuthentication: ObserveAuthenticationUseCase,
    getCurrentUser: GetCurrentUserUseCase,
    private val refreshPolls: RefreshPollsUseCase,
    private val refreshNotifications: RefreshNotificationsUseCase,
) : ViewModel() {
    private val actions = ActionRunner(viewModelScope)
    val actionState = actions.state
    private val loaded = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val filter = savedStateHandle.getStateFlow("filter", HomeFilter.MY_POLLS.name)
    val uiState = combine(observePolls(), observeNotifications(), observeAuthentication.session, filter, loaded) {
            polls, notifications, _, filterName, hasLoaded ->
        val selectedFilter = enumOrDefault(filterName, HomeFilter.MY_POLLS)
        val user = getCurrentUser()
        val own = polls.filter { user.userId == null || it.isCreator(user.userId) }
        val settled = own.filter { !it.isOpen() }.sortedByDescending { it.closedAt ?: it.createdAt }
        HomeUiState(
            hasLoaded = hasLoaded,
            hasAnyPolls = own.isNotEmpty(),
            settledPolls = if (selectedFilter == HomeFilter.MY_POLLS) settled.take(3) else emptyList(),
            isDemo = user.userId == null,
            filter = selectedFilter,
            polls = if (selectedFilter == HomeFilter.MY_POLLS) own.filter { it.isOpen() }.sortedByDescending { it.createdAt } else settled,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            hasUnreadNotifications = notifications.any { !it.isRead },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(
        filter = enumOrDefault(filter.value, HomeFilter.MY_POLLS),
        displayName = getCurrentUser().displayName,
        avatarUrl = getCurrentUser().avatarUrl,
    ))

    init {
        observeAuthentication.session.onEach {
            loaded.value = false
            actions.cancel()
            if (it.initialized) refresh()
        }.launchIn(viewModelScope)
    }
    fun retry() = actions.retry(::refresh)

    fun selectFilter(filter: HomeFilter) { if (filter != HomeFilter.TEMPLATES) savedStateHandle["filter"] = filter.name }

    fun refresh() {
        actions.run {
            val polls = com.shkurta.pickle.domain.model.cancellationSafeResult { refreshPolls() }
            val notifications = com.shkurta.pickle.domain.model.cancellationSafeResult { refreshNotifications() }
            if (polls.isSuccess) loaded.value = true
            polls.getOrThrow()
            notifications.getOrThrow()
        }
    }
}
