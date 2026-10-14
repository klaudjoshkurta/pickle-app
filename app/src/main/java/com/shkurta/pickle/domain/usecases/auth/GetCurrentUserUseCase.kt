package com.shkurta.pickle.domain.usecases.auth

import com.shkurta.pickle.domain.model.UserProfile
import com.shkurta.pickle.domain.repository.AuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): UserProfile = UserProfile(
        repository.currentUserId, repository.displayName, repository.avatarUrl,
    )
}
