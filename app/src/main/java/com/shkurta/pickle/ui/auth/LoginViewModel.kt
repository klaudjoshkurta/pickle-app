package com.shkurta.pickle.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.domain.model.cancellationSafeResult
import com.shkurta.pickle.domain.usecases.auth.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val signIn: SignInUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginUiState(email = savedStateHandle["email"] ?: ""))
    val uiState = mutableState.asStateFlow()
    private fun edit(transform: (LoginUiState) -> LoginUiState) {
        if (uiState.value.isLoading) return
        mutableState.value = transform(uiState.value).copy(status = AuthStatus.Idle)
        savedStateHandle["email"] = uiState.value.email
    }
    fun setEmail(value: String) = edit { it.copy(email = value) }
    fun setPassword(value: String) = edit { it.copy(password = value) }
    fun touch(field: AuthField) {
        if (!uiState.value.isLoading) mutableState.value = uiState.value.copy(touched = uiState.value.touched + field)
    }
    fun submit() {
        val state = uiState.value
        if (state.isLoading || state.status == AuthStatus.Success) return
        mutableState.value = state.copy(touched = setOf(AuthField.Email, AuthField.Password))
        if (!state.isValid) return
        mutableState.value = mutableState.value.copy(status = AuthStatus.Loading)
        viewModelScope.launch {
            val result = cancellationSafeResult { signIn(state.email.trim(), state.password).getOrThrow() }
            mutableState.value = mutableState.value.copy(status = if (result.isSuccess) AuthStatus.Success else AuthStatus.Error)
        }
    }
}
