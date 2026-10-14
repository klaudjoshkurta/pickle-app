package com.shkurta.pickle.data.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUserId: String?
    /** Name and DiceBear face picked at sign-up (user metadata). */
    val displayName: String?
    val avatarUrl: String?
    val isAuthenticated: StateFlow<Boolean>

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        avatarUrl: String,
    ): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
}
