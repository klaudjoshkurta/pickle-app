package com.shkurta.pickle.ui.poll

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.shkurta.pickle.data.PollRepository
import com.shkurta.pickle.data.auth.AuthRepository
import com.shkurta.pickle.domain.model.OptionStatus
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PollViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pollRepository: PollRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val pollId: String = savedStateHandle.toRoute<Screen.Poll>().pollId

    val currentUserId: String get() = authRepository.currentUserId ?: ""

    val poll: StateFlow<Poll?> = pollRepository.polls
        .map { polls -> polls.firstOrNull { it.id == pollId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val myVotes: StateFlow<Map<String, String>> = pollRepository.myVotes

    private val _lastSuggestionStatus = MutableStateFlow<OptionStatus?>(null)
    val lastSuggestionStatus: StateFlow<OptionStatus?> = _lastSuggestionStatus.asStateFlow()

    fun vote(optionId: String) {
        viewModelScope.launch { pollRepository.vote(pollId, optionId) }
    }

    fun suggestOption(text: String) {
        viewModelScope.launch {
            val status = pollRepository.suggestOption(pollId, text, currentUserId)
            _lastSuggestionStatus.value = status
        }
    }

    fun clearSuggestionStatus() {
        _lastSuggestionStatus.value = null
    }

    fun moderate(optionId: String, approve: Boolean) {
        viewModelScope.launch { pollRepository.moderate(pollId, optionId, approve) }
    }

    fun close() {
        viewModelScope.launch { pollRepository.close(pollId) }
    }
}
