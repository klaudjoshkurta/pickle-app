package com.shkurta.pickle.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.data.PollRepository
import com.shkurta.pickle.data.auth.AuthRepository
import com.shkurta.pickle.domain.model.PollSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class CreatePollViewModel @Inject constructor(
    private val pollRepository: PollRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _createdPollId = MutableSharedFlow<String>()
    val createdPollId: SharedFlow<String> = _createdPollId.asSharedFlow()

    fun createPoll(
        question: String,
        optionTexts: List<String>,
        settings: PollSettings,
        closesAt: Instant?,
        expectedVoters: Int?,
    ) {
        viewModelScope.launch {
            val poll = pollRepository.createPoll(
                question = question,
                optionTexts = optionTexts,
                settings = settings,
                closesAt = closesAt,
                expectedVoters = expectedVoters,
                creatorId = authRepository.currentUserId ?: return@launch,
            )
            if (poll != null) {
                _createdPollId.emit(poll.id)
            }
        }
    }
}
