package com.shkurta.pickle.ui.components

import java.util.logging.Logger
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shkurta.pickle.domain.model.InvalidRemoteDataException
import com.shkurta.pickle.domain.model.SavedRefreshException
import com.shkurta.pickle.domain.model.SignInRequiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActionState(val busy: Boolean = false, val message: String? = null, val saved: Boolean = false)

val LocalActionsEnabled = staticCompositionLocalOf { true }

@Composable
fun ActionFrame(state: ActionState, onRetry: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            CompositionLocalProvider(LocalActionsEnabled provides !state.busy, content = content)
        }
        if (state.busy || state.message != null) {
            Box(Modifier.navigationBarsPadding()) { ActionStatus(state, onRetry) }
        }
    }
}

/** One in-flight action per screen. Errors stay visible until retried or dismissed. */
class ActionRunner(private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(ActionState())
    val state = mutableState.asStateFlow()
    private var generation = 0L
    private var job: Job? = null
    private var retry: (() -> Unit)? = null

    fun run(block: suspend () -> Unit) {
        if (mutableState.value.busy) return
        val operation = ++generation
        retry = { run(block) }
        mutableState.value = ActionState(busy = true)
        job = scope.launch {
            try {
                block()
                if (operation == generation) mutableState.value = ActionState()
            } catch (e: CancellationException) {
                if (operation == generation) mutableState.value = ActionState()
                throw e
            } catch (e: Exception) {
                if (operation != generation) return@launch
                // Never log request bodies, user data, credentials, or raw server messages.
                Logger.getLogger("PickleAction").warning("Action failed: ${e.javaClass.simpleName}")
                mutableState.value = ActionState(
                    message = when (e) {
                        is SavedRefreshException -> "Saved. Couldn't refresh the latest data."
                        is SignInRequiredException -> "Sign in to use live polls."
                        is InvalidRemoteDataException -> "Some data couldn't be loaded. Please refresh."
                        else -> "Couldn't complete this request. Check your connection and try again."
                    },
                    saved = e is SavedRefreshException,
                )
                // A committed write must never be replayed by the Retry button.
                if (e is SavedRefreshException) retry = null
            }
        }
    }

    fun cancel() {
        generation++
        job?.cancel()
        retry = null
        mutableState.value = ActionState()
    }

    fun retry(refresh: () -> Unit) {
        if (mutableState.value.saved) refresh() else retry?.invoke()
    }
}

@Composable
fun ActionStatus(state: ActionState, onRetry: () -> Unit) {
    Surface {
    Column(Modifier.fillMaxWidth()) {
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let {
            Text(it, Modifier.padding(horizontal = 16.dp))
            TextButton(onClick = onRetry, enabled = !state.busy) {
                Text(if (state.saved) "Refresh" else "Retry")
            }
        }
    }
    }
}
