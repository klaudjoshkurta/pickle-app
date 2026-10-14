package com.shkurta.pickle.domain.usecases.poll

import com.shkurta.pickle.domain.repository.PollRepository
import javax.inject.Inject

class RefreshPollsUseCase @Inject constructor(private val repository: PollRepository) {
    suspend operator fun invoke() = repository.refresh()
}
