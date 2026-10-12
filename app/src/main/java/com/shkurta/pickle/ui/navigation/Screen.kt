package com.shkurta.pickle.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data object Poll : Screen

    @Serializable
    data object Notifications : Screen

    @Serializable
    data object Menu : Screen
}
