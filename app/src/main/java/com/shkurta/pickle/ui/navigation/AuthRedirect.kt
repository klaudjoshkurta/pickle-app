package com.shkurta.pickle.ui.navigation

enum class AuthRedirect { HOME, ONBOARDING, NONE }

/** A restored session preserves detail screens; an account change clears their back stack. */
fun authRedirect(previousUserId: String?, userId: String?, atEntry: Boolean): AuthRedirect = when {
    userId != null && (atEntry || (previousUserId != null && previousUserId != userId)) -> AuthRedirect.HOME
    userId == null && previousUserId != null -> AuthRedirect.ONBOARDING
    else -> AuthRedirect.NONE
}
