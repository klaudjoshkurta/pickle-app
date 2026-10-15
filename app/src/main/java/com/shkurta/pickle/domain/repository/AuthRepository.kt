package com.shkurta.pickle.domain.repository

import kotlinx.coroutines.flow.StateFlow

data class AuthSession(val initialized: Boolean = false, val userId: String? = null)

interface AuthRepository {
    val currentUserId: String?
    /** Name and DiceBear face picked at sign-up (user metadata). */
    val displayName: String?
    val avatarUrl: String?
    val isAuthenticated: StateFlow<Boolean>
    val session: StateFlow<AuthSession>

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        avatarUrl: String,
    ): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
}
