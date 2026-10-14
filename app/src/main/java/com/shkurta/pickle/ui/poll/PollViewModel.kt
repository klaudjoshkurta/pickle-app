package com.shkurta.pickle.ui.poll

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.domain.model.OptionStatus
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollOption
import com.shkurta.pickle.domain.model.PollResults
import com.shkurta.pickle.domain.model.toResults
import com.shkurta.pickle.domain.usecases.auth.GetCurrentUserUseCase
import com.shkurta.pickle.domain.usecases.auth.ObserveAuthenticationUseCase
import com.shkurta.pickle.domain.usecases.poll.ClosePollUseCase
import com.shkurta.pickle.domain.usecases.poll.ModerateOptionUseCase
import com.shkurta.pickle.domain.usecases.poll.ObserveMyVotesUseCase
import com.shkurta.pickle.domain.usecases.poll.ObservePollUseCase
import com.shkurta.pickle.domain.usecases.poll.SuggestOptionUseCase
import com.shkurta.pickle.domain.usecases.poll.VoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PollUiState(
    val poll: Poll? = null,
    val myVoteOptionId: String? = null,
    val lastSuggestionStatus: OptionStatus? = null,
    val selectedOptionId: String? = null,
    val isChangingVote: Boolean = false,
    val isOpen: Boolean = false,
    val isCreator: Boolean = false,
    val results: PollResults? = null,
    val pendingSuggestions: List<PollOption> = emptyList(),
) {
    val hasVoted: Boolean get() = myVoteOptionId != null
    val showResults: Boolean get() = poll != null && (hasVoted || poll.settings.showResultsBeforeVoting || !isOpen)
    val canVote: Boolean get() = isOpen && (!hasVoted || poll?.settings?.allowVoteChange == true)
    val isVoting: Boolean get() = canVote && (!hasVoted || isChangingVote)
    val showResultsSection: Boolean get() = showResults && (!isVoting || (results?.totalVotes ?: 0) > 0)
    val selectedOption: PollOption? get() = poll?.approvedOptions?.firstOrNull { it.id == selectedOptionId }

    companion object {
        fun from(
            poll: Poll?,
            userId: String,
            myVoteOptionId: String?,
            selectedOptionId: String? = myVoteOptionId,
            isChangingVote: Boolean = false,
            lastSuggestionStatus: OptionStatus? = null,
        ): PollUiState {
            val isOpen = poll?.isOpen() == true
            val isCreator = poll?.isCreator(userId) == true
            return PollUiState(
                poll = poll,
                myVoteOptionId = myVoteOptionId,
                lastSuggestionStatus = lastSuggestionStatus,
                selectedOptionId = selectedOptionId,
                isChangingVote = isChangingVote,
                isOpen = isOpen,
                isCreator = isCreator,
                results = poll?.toResults(),
                pendingSuggestions = if (isCreator && isOpen) poll!!.pendingSuggestions else emptyList(),
            )
        }
    }
}

private data class PollInteraction(
    val selectedOptionId: String?,
    val isChangingVote: Boolean,
    val lastSuggestionStatus: OptionStatus?,
)

@HiltViewModel
class PollViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observePoll: ObservePollUseCase,
    observeMyVotes: ObserveMyVotesUseCase,
    observeAuthentication: ObserveAuthenticationUseCase,
    getCurrentUser: GetCurrentUserUseCase,
    private val voteUseCase: VoteUseCase,
    private val suggestOptionUseCase: SuggestOptionUseCase,
    private val moderateOptionUseCase: ModerateOptionUseCase,
    private val closePollUseCase: ClosePollUseCase,
) : ViewModel() {
    private val pollId = requireNotNull(savedStateHandle.get<String>("pollId")) {
        "Missing pollId navigation argument"
    }
    private val votes = observeMyVotes()
    private var previousVote = votes.value[pollId]
    private val selectedOption = savedStateHandle.getStateFlow<String?>("selectedOptionId", previousVote)
    private val changingVote = savedStateHandle.getStateFlow("isChangingVote", false)
    private val suggestionStatus = MutableStateFlow<OptionStatus?>(null)
    private val interaction = combine(selectedOption, changingVote, suggestionStatus, ::PollInteraction)

    val uiState = combine(observePoll(pollId), votes, observeAuthentication(), interaction) {
            poll, myVotes, _, actions ->
        PollUiState.from(
            poll, getCurrentUser().userId ?: "", myVotes[pollId],
            actions.selectedOptionId, actions.isChangingVote, actions.lastSuggestionStatus,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PollUiState())

    init {
        votes.map { it[pollId] }.distinctUntilChanged().onEach { vote ->
            if (vote != previousVote) {
                previousVote = vote
                savedStateHandle["selectedOptionId"] = vote
                savedStateHandle["isChangingVote"] = false
            }
        }.launchIn(viewModelScope)
    }

    fun selectOption(optionId: String) { savedStateHandle["selectedOptionId"] = optionId }
    fun changeVote() { savedStateHandle["isChangingVote"] = true }
    fun keepVote() {
        savedStateHandle["selectedOptionId"] = votes.value[pollId]
        savedStateHandle["isChangingVote"] = false
    }
    fun vote() {
        val optionId = selectedOption.value ?: return
        savedStateHandle["isChangingVote"] = false
        viewModelScope.launch { voteUseCase(pollId, optionId) }
    }
    fun suggestOption(text: String) {
        viewModelScope.launch { suggestionStatus.value = suggestOptionUseCase(pollId, text) }
    }
    fun clearSuggestionStatus() { suggestionStatus.value = null }
    fun moderate(optionId: String, approve: Boolean) {
        viewModelScope.launch { moderateOptionUseCase(pollId, optionId, approve) }
    }
    fun close() { viewModelScope.launch { closePollUseCase(pollId) } }
}
