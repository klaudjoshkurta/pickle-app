package com.shkurta.pickle.domain.usecases.poll

import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.repository.PollRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObservePollUseCase @Inject constructor(private val repository: PollRepository) {
    operator fun invoke(pollId: String): Flow<Poll?> =
        repository.polls.map { polls -> polls.firstOrNull { it.id == pollId } }
}
