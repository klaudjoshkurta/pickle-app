package com.shkurta.pickle.domain.usecases.auth

import com.shkurta.pickle.domain.repository.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.signOut()
}
