package com.shkurta.pickle.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

internal fun NavDestination?.isAuthEntry(): Boolean =
    this?.hasRoute<Screen.Onboarding>() == true || this?.hasRoute<Screen.Login>() == true ||
        this?.hasRoute<Screen.Register>() == true

internal fun NavHostController.applyAuthRedirect(redirect: AuthRedirect) {
    when (redirect) {
        AuthRedirect.HOME -> navigate(Screen.Home) { popUpTo(0) { inclusive = true } }
        AuthRedirect.ONBOARDING -> navigate(Screen.Onboarding) { popUpTo(0) { inclusive = true } }
        AuthRedirect.NONE -> Unit
    }
}

/** Replace the auth form while keeping the screen that opened it directly underneath. */
internal fun NavHostController.switchAuthScreen(destination: Screen) {
    require(destination == Screen.Login || destination == Screen.Register)
    val currentId = currentDestination?.id ?: return
    navigate(destination) {
        popUpTo(currentId) { inclusive = true }
        launchSingleTop = true
    }
}
