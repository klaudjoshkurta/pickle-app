package com.shkurta.pickle.domain.usecases.auth

import com.shkurta.pickle.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, displayName: String, avatarUrl: String): Result<Unit> =
        repository.signUp(email, password, displayName, avatarUrl)
}
