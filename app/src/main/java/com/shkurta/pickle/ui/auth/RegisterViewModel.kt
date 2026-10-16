package com.shkurta.pickle.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.pickle.domain.model.cancellationSafeResult
import com.shkurta.pickle.domain.usecases.auth.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val signUp: SignUpUseCase,
) : ViewModel() {
    // Passwords stay in memory. A recreated flow always resumes at details.
    private val mutableState = MutableStateFlow(RegisterUiState(
        name = savedStateHandle["name"] ?: "",
        email = savedStateHandle["email"] ?: "",
        avatarSeeds = savedStateHandle.get<ArrayList<String>>("avatarSeeds")?.toList() ?: DiceBear.randomSeeds(),
        selectedSeed = savedStateHandle["selectedSeed"],
    ))
    val uiState = mutableState.asStateFlow()
    private fun edit(transform: (RegisterUiState) -> RegisterUiState) {
        if (uiState.value.isLoading) return
        mutableState.value = transform(uiState.value).copy(status = AuthStatus.Idle)
        val state = uiState.value
        savedStateHandle["name"] = state.name
        savedStateHandle["email"] = state.email
        savedStateHandle["avatarSeeds"] = ArrayList(state.avatarSeeds)
        savedStateHandle["selectedSeed"] = state.selectedSeed
    }
    fun setName(value: String) = edit { it.copy(name = value) }
    fun setEmail(value: String) = edit { it.copy(email = value) }
    fun setPassword(value: String) = edit { it.copy(password = value) }
    fun touch(field: AuthField) {
        if (!uiState.value.isLoading) mutableState.value = uiState.value.copy(touched = uiState.value.touched + field)
    }
    fun selectAvatar(seed: String) = edit { if (seed in it.avatarSeeds) it.copy(selectedSeed = seed) else it }
    fun shuffleAvatars() = edit { it.copy(avatarSeeds = DiceBear.randomSeeds(), selectedSeed = null, avatarAttempted = false) }
    fun previousStep() = edit { it.copy(step = RegistrationStep.Details) }
    fun nextStep() = edit {
        it.copy(touched = AuthField.entries.toSet(), step = if (it.detailsValid) RegistrationStep.Avatar else RegistrationStep.Details)
    }
    fun submit(language: String = "en") {
        val state = uiState.value
        if (state.isLoading || state.status == AuthStatus.Success) return
        if (!state.detailsValid) { nextStep(); return }
        mutableState.value = state.copy(avatarAttempted = true)
        val seed = state.selectedSeed?.takeIf { it in state.avatarSeeds } ?: return
        mutableState.value = mutableState.value.copy(status = AuthStatus.Loading)
        viewModelScope.launch {
            val result = cancellationSafeResult {
                signUp(state.email.trim(), state.password, state.name.trim(), DiceBear.avatarUrl(seed), language).getOrThrow()
            }
            mutableState.value = mutableState.value.copy(status = if (result.isSuccess) AuthStatus.Success else AuthStatus.Error)
        }
    }
}
