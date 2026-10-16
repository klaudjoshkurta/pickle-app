package com.shkurta.pickle.ui.auth

enum class RegistrationStep { Details, Avatar }
enum class AuthField { Name, Email, Password }
sealed interface AuthStatus {
    data object Idle : AuthStatus
    data object Loading : AuthStatus
    data object Error : AuthStatus
    data object Success : AuthStatus
}
internal fun validEmail(value: String): Boolean =
    Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value.trim())

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val touched: Set<AuthField> = emptySet(),
    val status: AuthStatus = AuthStatus.Idle,
) {
    val isLoading get() = status == AuthStatus.Loading
    val emailError get() = AuthField.Email in touched && !validEmail(email)
    val passwordError get() = AuthField.Password in touched && password.isBlank()
    val isValid get() = validEmail(email) && password.isNotBlank()
}
data class RegisterUiState(
    val step: RegistrationStep = RegistrationStep.Details,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val avatarSeeds: List<String> = emptyList(),
    val selectedSeed: String? = null,
    val touched: Set<AuthField> = emptySet(),
    val avatarAttempted: Boolean = false,
    val status: AuthStatus = AuthStatus.Idle,
) {
    val isLoading get() = status == AuthStatus.Loading
    val nameError get() = AuthField.Name in touched && name.isBlank()
    val emailError get() = AuthField.Email in touched && !validEmail(email)
    val passwordError get() = AuthField.Password in touched && password.length < 6
    val detailsValid get() = name.isNotBlank() && validEmail(email) && password.length >= 6
}
