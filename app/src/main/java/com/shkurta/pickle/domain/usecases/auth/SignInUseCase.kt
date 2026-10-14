package com.shkurta.pickle.domain.usecases.auth

import com.shkurta.pickle.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> = repository.signIn(email, password)
}
