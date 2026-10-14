package com.shkurta.pickle.ui.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.domain.model.MIN_POLL_OPTIONS
import com.shkurta.pickle.domain.model.PollSettings
import com.shkurta.pickle.domain.model.distinctOptionTexts
import com.shkurta.pickle.domain.model.isValidPollInput
import com.shkurta.pickle.domain.usecases.poll.CreatePollUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

const val MAX_POLL_OPTIONS = 10

enum class Deadline(val label: String, val hours: Long?) {
    NONE("No deadline", null),
    ONE_HOUR("In 1 hour", 1),
    ONE_DAY("1 day", 24),
    THREE_DAYS("3 days", 72),
    ONE_WEEK("1 week", 168),
}

data class CreatePollUiState(
    val question: String = "",
    val options: List<String> = List(MIN_POLL_OPTIONS) { "" },
    val settings: PollSettings = PollSettings(),
    val deadline: Deadline = Deadline.NONE,
    val expectedVoters: String = "",
) {
    val filledOptions: Int get() = distinctOptionTexts(options).size
    val isValid: Boolean get() = isValidPollInput(question, options)
}

@HiltViewModel
class CreatePollViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val createPollUseCase: CreatePollUseCase,
) : ViewModel() {
    private val defaults = PollSettings()
    private val _uiState = MutableStateFlow(CreatePollUiState(
        question = savedStateHandle["question"] ?: "",
        options = savedStateHandle.get<ArrayList<String>>("options")?.toList()
            ?: List(MIN_POLL_OPTIONS) { "" },
        settings = PollSettings(
            allowSuggestions = savedStateHandle["allowSuggestions"] ?: defaults.allowSuggestions,
            requireApproval = savedStateHandle["requireApproval"] ?: defaults.requireApproval,
            showResultsBeforeVoting = savedStateHandle["showResultsBeforeVoting"] ?: defaults.showResultsBeforeVoting,
            allowVoteChange = savedStateHandle["allowVoteChange"] ?: defaults.allowVoteChange,
        ),
        deadline = Deadline.valueOf(savedStateHandle["deadline"] ?: Deadline.NONE.name),
        expectedVoters = savedStateHandle["expectedVoters"] ?: "",
    ))
    val uiState = _uiState.asStateFlow()
    private val created = Channel<String>(Channel.BUFFERED)
    val createdPollId = created.receiveAsFlow()

    private fun update(state: CreatePollUiState) {
        savedStateHandle["question"] = state.question
        savedStateHandle["options"] = ArrayList(state.options)
        savedStateHandle["allowSuggestions"] = state.settings.allowSuggestions
        savedStateHandle["requireApproval"] = state.settings.requireApproval
        savedStateHandle["showResultsBeforeVoting"] = state.settings.showResultsBeforeVoting
        savedStateHandle["allowVoteChange"] = state.settings.allowVoteChange
        savedStateHandle["deadline"] = state.deadline.name
        savedStateHandle["expectedVoters"] = state.expectedVoters
        _uiState.value = state
    }

    fun setQuestion(value: String) = update(uiState.value.copy(question = value))
    fun setOption(index: Int, value: String) {
        if (index !in uiState.value.options.indices) return
        update(uiState.value.copy(options = uiState.value.options.mapIndexed { i, text -> if (i == index) value else text }))
    }
    fun addOption() {
        if (uiState.value.options.size < MAX_POLL_OPTIONS) update(uiState.value.copy(options = uiState.value.options + ""))
    }
    fun removeOption(index: Int) {
        if (uiState.value.options.size > MIN_POLL_OPTIONS && index in uiState.value.options.indices)
            update(uiState.value.copy(options = uiState.value.options.filterIndexed { i, _ -> i != index }))
    }
    fun setSettings(value: PollSettings) = update(uiState.value.copy(settings = value))
    fun setDeadline(value: Deadline) = update(uiState.value.copy(deadline = value))
    fun setExpectedVoters(value: String) =
        update(uiState.value.copy(expectedVoters = value.filter(Char::isDigit).take(4)))

    fun createPoll() {
        val state = uiState.value
        if (!state.isValid) return
        viewModelScope.launch {
            val poll = createPollUseCase(
                question = state.question,
                optionTexts = state.options,
                settings = state.settings,
                closesAt = state.deadline.hours?.let { Instant.now().plus(it, ChronoUnit.HOURS) },
                expectedVoters = state.expectedVoters.toIntOrNull(),
            )
            if (poll != null) created.send(poll.id)
        }
    }
}
