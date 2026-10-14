package com.shkurta.pickle.ui.auth

enum class SignUpStep { Name, Avatar, Email, Password }

sealed interface AuthStatus {
    data object Idle : AuthStatus
    data object Loading : AuthStatus
    data class Error(val message: String) : AuthStatus
    data object Success : AuthStatus
}

data class AuthUiState(
    val isSignUp: Boolean = false,
    val step: SignUpStep = SignUpStep.Name,
    val name: String = "",
    val avatarSeeds: List<String> = emptyList(),
    val selectedSeed: String? = null,
    val email: String = "",
    val password: String = "",
    val status: AuthStatus = AuthStatus.Idle,
) {
    val isLoading: Boolean get() = status is AuthStatus.Loading
    val isEmailValid: Boolean get() = email.trim().contains('@')
    val isPasswordValid: Boolean get() = password.length >= 6
    val isStepValid: Boolean get() = when (step) {
        SignUpStep.Name -> name.isNotBlank()
        SignUpStep.Avatar -> selectedSeed != null
        SignUpStep.Email -> isEmailValid
        SignUpStep.Password -> isPasswordValid
    }
    val canSignIn: Boolean get() = email.isNotBlank() && password.isNotBlank()
    val canGoBackStep: Boolean get() = isSignUp && step != SignUpStep.Name
}
