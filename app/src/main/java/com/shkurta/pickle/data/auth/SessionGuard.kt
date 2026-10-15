package com.shkurta.pickle.data.auth

import com.shkurta.pickle.domain.model.SignInRequiredException
import com.shkurta.pickle.domain.repository.AuthRepository
import com.shkurta.pickle.domain.repository.AuthSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Cancel work on account changes; also check before publishing results. */
@Singleton
class SessionGuard @Inject constructor(private val auth: AuthRepository) {
    suspend fun ready(): AuthSession = auth.session.first { it.initialized }

    suspend fun <T> authenticated(block: suspend (String) -> T): T = coroutineScope {
        val session = ready()
        val user = session.userId ?: throw SignInRequiredException()
        val request = currentCoroutineContext()[Job]!!
        val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
            auth.session.first { it != session }
            request.cancel(CancellationException("Account changed"))
        }
        try {
            val result = block(user)
            ensureActive()
            if (auth.session.value != session) throw CancellationException("Account changed")
            result
        } finally {
            watcher.cancel()
        }
    }
}
