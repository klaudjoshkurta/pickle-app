package com.shkurta.pickle.domain.usecases.poll

import com.shkurta.pickle.domain.repository.PollRepository
import javax.inject.Inject

class ObserveMyVotesUseCase @Inject constructor(private val repository: PollRepository) {
    operator fun invoke(): kotlinx.coroutines.flow.StateFlow<Map<String, Set<String>>> = repository.myVotes
}
