package com.shkurta.pickle.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data class Poll(val pollId: String) : Screen

    @Serializable
    data object CreatePoll : Screen

    @Serializable
    data object Notifications : Screen

    @Serializable
    data object Menu : Screen
}
