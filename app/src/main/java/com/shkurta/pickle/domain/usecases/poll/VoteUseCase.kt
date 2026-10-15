package com.shkurta.pickle.domain.usecases.poll

import com.shkurta.pickle.domain.repository.PollRepository
import javax.inject.Inject

class VoteUseCase @Inject constructor(private val repository: PollRepository) {
    suspend operator fun invoke(pollId: String, optionIds: Set<String>) = repository.vote(pollId, optionIds)
}
