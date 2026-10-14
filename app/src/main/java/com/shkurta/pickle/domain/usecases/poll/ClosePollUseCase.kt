package com.shkurta.pickle.domain.usecases.poll

import com.shkurta.pickle.domain.repository.PollRepository
import javax.inject.Inject

class ClosePollUseCase @Inject constructor(private val repository: PollRepository) {
    suspend operator fun invoke(pollId: String) = repository.close(pollId)
}
