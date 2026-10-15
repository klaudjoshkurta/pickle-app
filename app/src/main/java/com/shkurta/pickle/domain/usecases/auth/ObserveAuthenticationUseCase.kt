package com.shkurta.pickle.domain.usecases.auth

import com.shkurta.pickle.domain.repository.AuthRepository
import javax.inject.Inject

class ObserveAuthenticationUseCase @Inject constructor(private val repository: AuthRepository) {
    val session get() = repository.session
    operator fun invoke() = repository.isAuthenticated
}
