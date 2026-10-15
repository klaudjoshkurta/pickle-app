package com.shkurta.pickle.data.auth

import com.shkurta.pickle.domain.repository.AuthRepository
import com.shkurta.pickle.domain.repository.AuthSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Repository-lifetime cache. All production access is on the main dispatcher. */
class AccountCache<T>(
    private val auth: AuthRepository,
    private val empty: T,
    private val demo: T = empty,
) {
    private fun initial(session: AuthSession) =
        if (session.initialized && session.userId == null) demo else empty
    private val mutableState = MutableStateFlow(initial(auth.session.value))
    val state = mutableState.asStateFlow()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch(start = CoroutineStart.UNDISPATCHED) {
            var previous = auth.session.value
            auth.session.collect { session ->
                if (session != previous) {
                    previous = session
                    mutableState.value = initial(session)
                }
            }
        }
    }

    suspend fun publish(session: AuthSession, value: T) {
        currentCoroutineContext().ensureActive()
        if (auth.session.value != session) throw CancellationException("Account changed")
        mutableState.value = value
    }
}
