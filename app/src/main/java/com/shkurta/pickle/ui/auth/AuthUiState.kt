package com.shkurta.pickle.ui.auth

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Error(val message: String) : AuthUiState
    data object Success : AuthUiState
}
