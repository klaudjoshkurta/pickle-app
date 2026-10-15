package com.shkurta.pickle.ui.poll

import com.shkurta.pickle.ui.components.ActionRunner
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
import javax.inject.Inject

data class PollUiState(
    val isDemo: Boolean = false,
    val poll: Poll? = null,
    val myVoteOptionIds: Set<String> = emptySet(),
    val lastSuggestionStatus: OptionStatus? = null,
    val selectedOptionIds: Set<String> = emptySet(),
    val isChangingVote: Boolean = false,
    val isOpen: Boolean = false,
    val isCreator: Boolean = false,
    val results: PollResults? = null,
    val pendingSuggestions: List<PollOption> = emptyList(),
) {
    val myVoteOptionId: String? get() = myVoteOptionIds.firstOrNull()
    val selectedOptionId: String? get() = selectedOptionIds.firstOrNull()
    val hasVoted: Boolean get() = myVoteOptionIds.isNotEmpty()
    val showResults: Boolean get() = poll != null && (isDemo || hasVoted || poll.settings.showResultsBeforeVoting || !isOpen)
    val canVote: Boolean get() = !isDemo && isOpen && (!hasVoted || poll?.settings?.allowVoteChange == true)
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
            return fromBallot(poll,userId,setOfNotNull(myVoteOptionId),setOfNotNull(selectedOptionId),isChangingVote,lastSuggestionStatus)
        }
        fun fromBallot(poll: Poll?, userId: String, myVoteOptionIds: Set<String>, selectedOptionIds: Set<String> = myVoteOptionIds, isChangingVote: Boolean = false, lastSuggestionStatus: OptionStatus? = null): PollUiState {
            val isOpen = poll?.isOpen() == true
            val isCreator = poll?.isCreator(userId) == true
            return PollUiState(
                poll = poll,
                myVoteOptionIds = myVoteOptionIds,
                lastSuggestionStatus = lastSuggestionStatus,
                selectedOptionIds = selectedOptionIds,
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
    val selectedOptionIds: Set<String>,
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
    private val templates: com.shkurta.pickle.domain.repository.TemplateRepository,
    private val refreshPolls: com.shkurta.pickle.domain.usecases.poll.RefreshPollsUseCase,
) : ViewModel() {
    private val actions = ActionRunner(viewModelScope)
    val actionState = actions.state
    private val pollId = requireNotNull(savedStateHandle.get<String>("pollId")) {
        "Missing pollId navigation argument"
    }
    private val votes = observeMyVotes()
    private var previousVote = votes.value[pollId]
    private val selectedOption = savedStateHandle.getStateFlow("selectedOptionIds", ArrayList<String>(previousVote.orEmpty().toList()))
    private val changingVote = savedStateHandle.getStateFlow("isChangingVote", false)
    private val suggestionStatus = MutableStateFlow<OptionStatus?>(null)
    private val interaction = combine(selectedOption, changingVote, suggestionStatus) { selected, changing, status -> PollInteraction(selected.toSet(),changing,status) }

    val uiState = combine(observePoll(pollId), votes, observeAuthentication.session, interaction) {
            poll, myVotes, _, actions ->
        PollUiState.fromBallot(
            poll, getCurrentUser().userId ?: "", myVotes[pollId].orEmpty(),
            actions.selectedOptionIds, actions.isChangingVote, actions.lastSuggestionStatus,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PollUiState())

    val savedTemplateId = MutableStateFlow<String?>(null)
    val personalTemplates = templates.templates
    fun clearSavedTemplate() { savedTemplateId.value=null }
    fun saveTemplate(name:String, icon:String, schedule:com.shkurta.pickle.domain.model.WeeklySchedule?) = actions.run {
        try { savedTemplateId.value=templates.save(pollId,name,icon,schedule) }
        catch(e:com.shkurta.pickle.domain.model.SavedRefreshException) { savedTemplateId.value=e.savedId; throw e }
    }
    fun refresh() = actions.run { refreshPolls(); templates.refresh() }
    fun retry() = actions.retry(::refresh)

    init {
        var previousUser = getCurrentUser().userId
        observeAuthentication.session.map { it.userId }.distinctUntilChanged().onEach {
            actions.cancel(); savedTemplateId.value=null; suggestionStatus.value=null
            if(it != previousUser) {
                previousUser=it
                savedStateHandle["selectedOptionIds"]=arrayListOf<String>(); savedStateHandle["isChangingVote"]=false
            }
            refresh()
        }.launchIn(viewModelScope)
        votes.map { it[pollId] }.distinctUntilChanged().onEach { vote ->
            if (vote != previousVote) {
                previousVote = vote
                savedStateHandle["selectedOptionIds"] = ArrayList<String>(vote.orEmpty().toList())
                savedStateHandle["isChangingVote"] = false
            }
        }.launchIn(viewModelScope)
    }

    fun selectOption(optionId: String) {
        if(actionState.value.busy) return
        val selected=selectedOption.value.toSet()
        savedStateHandle["selectedOptionIds"] = ArrayList(if(uiState.value.poll?.settings?.votingMode == com.shkurta.pickle.domain.model.VotingMode.MULTIPLE) {
            if(optionId in selected) selected-optionId else selected+optionId
        } else setOf(optionId))
    }
    fun changeVote() { savedStateHandle["isChangingVote"] = true }
    fun keepVote() {
        savedStateHandle["selectedOptionIds"] = ArrayList<String>(votes.value[pollId].orEmpty().toList())
        savedStateHandle["isChangingVote"] = false
    }
    fun vote() {
        val optionIds = selectedOption.value.toSet()
        if(optionIds.isEmpty()) return
        actions.run {
            voteUseCase(pollId, optionIds)
            savedStateHandle["isChangingVote"] = false
        }
    }
    fun suggestOption(text: String) {
        actions.run { suggestionStatus.value = suggestOptionUseCase(pollId, text) }
    }
    fun clearSuggestionStatus() { suggestionStatus.value = null }
    fun moderate(optionId: String, approve: Boolean) {
        actions.run { moderateOptionUseCase(pollId, optionId, approve) }
    }
    fun close() { actions.run { closePollUseCase(pollId) } }
}
