package com.shkurta.pickle.ui

import com.shkurta.pickle.ui.navigation.AuthRedirect
import com.shkurta.pickle.ui.navigation.authRedirect
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthRedirectTest {
    @Test fun restoredSessionPreservesDetailScreen() {
        assertEquals(AuthRedirect.NONE, authRedirect(null, "restored", false))
        assertEquals(AuthRedirect.NONE, authRedirect("restored", "restored", false))
    }
    @Test fun signInLeavesEntryScreensAndSwitchingAccountsClearsDetails() {
        assertEquals(AuthRedirect.HOME, authRedirect(null, "signed-in", true))
        assertEquals(AuthRedirect.HOME, authRedirect("old", "new", false))
    }
    @Test fun expiredSessionReturnsToOnboardingAndGuestCanStayInDemo() {
        assertEquals(AuthRedirect.ONBOARDING, authRedirect("expired", null, false))
        assertEquals(AuthRedirect.NONE, authRedirect(null, null, false))
    }
}
