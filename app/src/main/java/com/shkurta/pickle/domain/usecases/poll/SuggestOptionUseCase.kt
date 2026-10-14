package com.shkurta.pickle.domain.usecases.poll

import com.shkurta.pickle.domain.model.OptionStatus
import com.shkurta.pickle.domain.repository.AuthRepository
import com.shkurta.pickle.domain.repository.PollRepository
import javax.inject.Inject

class SuggestOptionUseCase @Inject constructor(
    private val polls: PollRepository,
    private val auth: AuthRepository,
) {
    suspend operator fun invoke(pollId: String, text: String): OptionStatus? =
        polls.suggestOption(pollId, text, auth.currentUserId ?: "")
}
