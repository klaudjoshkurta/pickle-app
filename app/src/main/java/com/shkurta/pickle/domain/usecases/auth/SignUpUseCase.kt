package com.shkurta.pickle.domain.usecases.auth

import com.shkurta.pickle.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String,
        avatarUrl: String,
        language: String = "en",
    ): Result<Unit> {
        // Match the confirmation template's two supported languages, including regional tags.
        val emailLanguage = if (language.substringBefore('-').substringBefore('_').equals("el", ignoreCase = true)) "el" else "en"
        return repository.signUp(email, password, displayName, avatarUrl, emailLanguage)
    }
}
