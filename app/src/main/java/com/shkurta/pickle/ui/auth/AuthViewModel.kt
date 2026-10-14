package com.shkurta.pickle.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.domain.usecases.auth.ObserveAuthenticationUseCase
import com.shkurta.pickle.domain.usecases.auth.SignInUseCase
import com.shkurta.pickle.domain.usecases.auth.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    observeAuthentication: ObserveAuthenticationUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState(
        isSignUp = savedStateHandle["isSignUp"] ?: savedStateHandle["signUp"] ?: false,
        step = SignUpStep.valueOf(savedStateHandle["step"] ?: SignUpStep.Name.name),
        name = savedStateHandle["name"] ?: "",
        avatarSeeds = savedStateHandle.get<ArrayList<String>>("avatarSeeds")?.toList() ?: DiceBear.randomSeeds(),
        selectedSeed = savedStateHandle["selectedSeed"],
        email = savedStateHandle["email"] ?: "",
    ))
    val uiState = _uiState.asStateFlow()
    val isAuthenticated = observeAuthentication()

    fun initialize(initialSignUp: Boolean) {
        if (savedStateHandle.get<Boolean>("initialized") == true) return
        savedStateHandle["initialized"] = true
        update(uiState.value.copy(isSignUp = initialSignUp))
    }

    private fun update(state: AuthUiState) {
        savedStateHandle["isSignUp"] = state.isSignUp
        savedStateHandle["step"] = state.step.name
        savedStateHandle["name"] = state.name
        savedStateHandle["avatarSeeds"] = ArrayList(state.avatarSeeds)
        savedStateHandle["selectedSeed"] = state.selectedSeed
        savedStateHandle["email"] = state.email
        _uiState.value = state
    }

    fun setName(value: String) = update(uiState.value.copy(name = value))
    fun setEmail(value: String) = update(uiState.value.copy(email = value))
    fun setPassword(value: String) = update(uiState.value.copy(password = value))
    fun selectAvatar(seed: String) = update(uiState.value.copy(selectedSeed = seed))
    fun shuffleAvatars() = update(uiState.value.copy(avatarSeeds = DiceBear.randomSeeds(), selectedSeed = null))
    fun toggleMode() = update(uiState.value.copy(isSignUp = !uiState.value.isSignUp, step = SignUpStep.Name))
    fun previousStep() {
        if (uiState.value.canGoBackStep)
            update(uiState.value.copy(step = SignUpStep.entries[uiState.value.step.ordinal - 1]))
    }
    fun nextStep() {
        val state = uiState.value
        if (!state.isStepValid) return
        if (state.step == SignUpStep.Password) submit()
        else update(state.copy(step = SignUpStep.entries[state.step.ordinal + 1]))
    }

    fun submit() {
        val state = uiState.value
        if (state.isSignUp) {
            val seed = state.selectedSeed ?: return
            if (state.name.isNotBlank() && state.isEmailValid && state.isPasswordValid)
                signUp(state.email.trim(), state.password, state.name.trim(), DiceBear.avatarUrl(seed))
        } else if (state.canSignIn) {
            signIn(state.email.trim(), state.password)
        }
    }

    private fun signIn(email: String, password: String) {
        update(uiState.value.copy(status = AuthStatus.Loading))
        viewModelScope.launch {
            signInUseCase(email, password)
                .onSuccess { update(uiState.value.copy(status = AuthStatus.Success)) }
                .onFailure { update(uiState.value.copy(status = AuthStatus.Error(it.message ?: "Sign in failed"))) }
        }
    }

    private fun signUp(email: String, password: String, displayName: String, avatarUrl: String) {
        update(uiState.value.copy(status = AuthStatus.Loading))
        viewModelScope.launch {
            signUpUseCase(email, password, displayName, avatarUrl)
                .onSuccess { update(uiState.value.copy(status = AuthStatus.Success)) }
                .onFailure { update(uiState.value.copy(status = AuthStatus.Error(it.message ?: "Sign up failed"))) }
        }
    }

    fun clearError() {
        if (uiState.value.status is AuthStatus.Error) update(uiState.value.copy(status = AuthStatus.Idle))
    }
}
