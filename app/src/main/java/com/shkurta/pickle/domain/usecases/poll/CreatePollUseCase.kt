package com.shkurta.pickle.domain.usecases.poll

import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollSettings
import com.shkurta.pickle.domain.model.isValidPollInput
import com.shkurta.pickle.domain.repository.AuthRepository
import com.shkurta.pickle.domain.repository.PollRepository
import java.time.Instant
import javax.inject.Inject

class CreatePollUseCase @Inject constructor(
    private val polls: PollRepository,
    private val auth: AuthRepository,
) {
    suspend operator fun invoke(
        question: String,
        optionTexts: List<String>,
        settings: PollSettings,
        closesAt: Instant?,
        expectedVoters: Int?,
    ): Poll? {
        val creatorId = auth.currentUserId ?: return null
        if (!isValidPollInput(question, optionTexts)) return null
        return polls.createPoll(question, optionTexts, settings, closesAt, expectedVoters, creatorId)
    }
}
