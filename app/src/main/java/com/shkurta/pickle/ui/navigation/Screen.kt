package com.shkurta.pickle.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface
Screen {
    @Serializable
    data object Onboarding : Screen

    @Serializable
    data class Auth(val signUp: Boolean = false) : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data class Poll(val pollId: String) : Screen

    @Serializable
    data class CreatePoll(val starterId: String? = null, val templateId: String? = null) : Screen

    @Serializable
    data object Templates : Screen

    @Serializable
    data object Notifications : Screen

    @Serializable
    data object Menu : Screen
}
