package com.shkurta.pickle.data.auth

import com.shkurta.pickle.domain.repository.AuthRepository

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    override val currentUserId: String?
        get() = auth.currentUserOrNull()?.id

    override val displayName: String?
        get() = metadataString("display_name")

    override val avatarUrl: String?
        get() = metadataString("avatar_url")

    private fun metadataString(key: String): String? =
        (auth.currentUserOrNull()?.userMetadata?.get(key) as? JsonPrimitive)
            ?.contentOrNull
            ?.takeIf { it.isNotBlank() }

    init {
        scope.launch {
            auth.sessionStatus.collect { status ->
                _isAuthenticated.value = status is SessionStatus.Authenticated
            }
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        avatarUrl: String,
    ): Result<Unit> = runCatching {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("display_name", displayName)
                put("avatar_url", avatarUrl)
            }
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}
