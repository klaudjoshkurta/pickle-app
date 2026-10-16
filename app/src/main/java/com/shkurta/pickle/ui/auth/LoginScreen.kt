package com.shkurta.pickle.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shkurta.pickle.R
import com.shkurta.pickle.ui.components.*

@Composable
fun LoginScreen(onBack: () -> Unit, onRegister: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginContent(state, onBack, onRegister, viewModel::setEmail, viewModel::setPassword, viewModel::touch, viewModel::submit)
}

@Composable
fun LoginContent(
    state: LoginUiState,
    onBack: () -> Unit,
    onRegister: () -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onTouch: (AuthField) -> Unit,
    onSubmit: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val submit = { focus.clearFocus(); onSubmit() }
    AuthLayout(onBack, state.isLoading) {
        AuthHeader(stringResource(R.string.log_in_title), stringResource(R.string.log_in_subtitle))
        Spacer(Modifier.height(32.dp))
        AuthEmail(state.email, onEmail, !state.isLoading, state.emailError, { onTouch(AuthField.Email) })
        Spacer(Modifier.height(20.dp))
        AuthPassword(state.password, onPassword, !state.isLoading, state.passwordError,
            stringResource(R.string.auth_password_required), { onTouch(AuthField.Password) }, submit)
        Spacer(Modifier.height(24.dp))
        RequestFeedback(state.status, registration = false)
        PickleButton(stringResource(R.string.log_in), submit, Modifier.fillMaxWidth(),
            style = PickleButtonStyle.Teal, enabled = !state.isLoading && state.status != AuthStatus.Success, loading = state.isLoading)
        AuthSwitch(stringResource(R.string.switch_to_sign_up), !state.isLoading, onRegister)
    }
}
