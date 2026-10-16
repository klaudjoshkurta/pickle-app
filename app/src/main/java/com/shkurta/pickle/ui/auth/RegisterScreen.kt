package com.shkurta.pickle.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shkurta.pickle.R
import com.shkurta.pickle.ui.components.*
import com.shkurta.pickle.ui.theme.Pickle

@Composable
fun RegisterScreen(onBack: () -> Unit, onLogin: () -> Unit, viewModel: RegisterViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RegisterContent(state, onBack, onLogin, viewModel::setName, viewModel::setEmail, viewModel::setPassword,
        viewModel::touch, viewModel::nextStep, viewModel::previousStep, viewModel::selectAvatar,
        viewModel::shuffleAvatars, viewModel::submit)
}

@Composable
fun RegisterContent(
    state: RegisterUiState,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onTouch: (AuthField) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSelectAvatar: (String) -> Unit,
    onShuffle: () -> Unit,
    onSubmit: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val details = state.step == RegistrationStep.Details
    val back = { if (!details) onPrevious() else onBack() }
    val next = { focus.clearFocus(); if (details) onNext() else onSubmit() }
    BackHandler(enabled = !details && !state.isLoading) { back() }
    AuthLayout(back, state.isLoading, state.step) {
        Text(stringResource(R.string.sign_up_step, if (details) 1 else 2, 2),
            style = MaterialTheme.typography.labelLarge, color = Pickle.colors.textAccent,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { if (details) 0.5f else 1f }, modifier = Modifier.fillMaxWidth(),
            color = Pickle.colors.teal, trackColor = Pickle.colors.lineSoft)
        Spacer(Modifier.height(24.dp))
        AuthHeader(
            stringResource(if (details) R.string.auth_details_title else R.string.sign_up_avatar_title),
            stringResource(if (details) R.string.auth_details_subtitle else R.string.sign_up_avatar_subtitle),
        )
        Spacer(Modifier.height(32.dp))
        // Each step gets fresh field focus state.
        key(state.step) {
            if (details) {
                PickleTextField(state.name, onName, stringResource(R.string.sign_up_name_placeholder),
                    label = stringResource(R.string.auth_name_label), enabled = !state.isLoading,
                    modifier = blurValidation { onTouch(AuthField.Name) },
                    isError = state.nameError, supportingText = if (state.nameError) stringResource(R.string.auth_name_required) else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }))
                Spacer(Modifier.height(20.dp))
                AuthEmail(state.email, onEmail, !state.isLoading, state.emailError, { onTouch(AuthField.Email) })
                Spacer(Modifier.height(20.dp))
                AuthPassword(state.password, onPassword, !state.isLoading, state.passwordError,
                    stringResource(R.string.auth_password_short), { onTouch(AuthField.Password) }, next,
                    helper = stringResource(R.string.auth_password_hint))
            } else {
                AvatarGrid(state.avatarSeeds, state.selectedSeed, !state.isLoading, onSelectAvatar)
                TextButton(onClick = onShuffle, enabled = !state.isLoading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.sign_up_shuffle))
                }
                if (state.avatarAttempted && state.selectedSeed == null) {
                    Text(stringResource(R.string.auth_avatar_required), color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        RequestFeedback(state.status, registration = true)
        PickleButton(stringResource(if (details) R.string.continue_button else R.string.create_account), next,
            Modifier.fillMaxWidth(), style = PickleButtonStyle.Teal,
            enabled = !state.isLoading && state.status != AuthStatus.Success, loading = state.isLoading)
        AuthSwitch(stringResource(R.string.switch_to_log_in), !state.isLoading, onLogin)
    }
}
