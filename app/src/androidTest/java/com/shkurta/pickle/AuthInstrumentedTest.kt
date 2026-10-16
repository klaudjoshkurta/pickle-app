package com.shkurta.pickle

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.shkurta.pickle.domain.repository.*
import com.shkurta.pickle.domain.usecases.auth.*
import com.shkurta.pickle.ui.auth.*
import com.shkurta.pickle.ui.navigation.*
import com.shkurta.pickle.ui.theme.PickleTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

/** Auth fixtures use a fake repository and never create a real account. */
class AuthInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val auth = object : AuthRepository {
        override val currentUserId: String? = null
        override val displayName: String? = null
        override val avatarUrl: String? = null
        override val isAuthenticated = MutableStateFlow(false)
        override val session = MutableStateFlow(AuthSession(true))
        override suspend fun signIn(email: String, password: String) = Result.failure<Unit>(IllegalStateException())
        override suspend fun signUp(email: String, password: String, displayName: String, avatarUrl: String) = Result.failure<Unit>(IllegalStateException())
        override suspend fun signOut() = Unit
    }
    private fun render(greek: Boolean = false, large: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            val context = LocalContext.current
            val localized = remember {
                context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(if (greek) "el" else "en"))
                })
            }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalContext provides localized,
                LocalConfiguration provides localized.resources.configuration,
                LocalDensity provides Density(density.density, if (large) 1.5f else 1f)) {
                PickleTheme {
                    Box(Modifier.width(if (large) 320.dp else 412.dp).heightIn(max = if (large) 568.dp else 917.dp).fillMaxHeight().testTag("auth-fixture")) { content() }
                }
            }
        }
    }
    private fun login(vm: LoginViewModel) = render {
        val state by vm.uiState.collectAsState()
        LoginContent(state, {}, {}, vm::setEmail, vm::setPassword, vm::touch, vm::submit)
    }
    private fun registration(vm: RegisterViewModel, greek: Boolean = false, large: Boolean = false) = render(greek, large) {
        val state by vm.uiState.collectAsState()
        RegisterContent(state, {}, {}, vm::setName, vm::setEmail, vm::setPassword, vm::touch,
            vm::nextStep, vm::previousStep, vm::selectAvatar, vm::shuffleAvatars, vm::submit)
    }
    private fun capture(name: String) {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "screenshots/$name.png")
        file.parentFile!!.mkdirs()
        compose.onNodeWithTag("auth-fixture").captureToImage().asAndroidBitmap().let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Test fun loginLabelsValidationAndPersistentRequestFailure() {
        val vm = LoginViewModel(SavedStateHandle(), SignInUseCase(auth))
        login(vm)
        capture("auth-login")
        compose.onNodeWithText("Log in", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Enter a valid email address.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Email").performTextInput("alex@example.com")
        compose.onNodeWithContentDescription("Password").performScrollTo().performTextInput("secret")
        compose.onNodeWithContentDescription("Show password").performClick()
        compose.onNodeWithContentDescription("Hide password").assertExists()
        compose.onNodeWithText("Log in", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Couldn’t log in.", substring = true).performScrollTo().assertIsDisplayed()
        capture("auth-login-error")
    }
    @Test fun registrationBackKeepsDetailsAndAvatarSelectionIsAccessible() {
        val vm = RegisterViewModel(SavedStateHandle(), SignUpUseCase(auth))
        vm.setName("Alex"); vm.setEmail("alex@example.com"); vm.setPassword("secret")
        registration(vm)
        capture("auth-register-details")
        compose.onNodeWithText("Continue").performScrollTo().performClick()
        compose.onNodeWithText("Step 2 of 2").assertIsDisplayed()
        compose.onNodeWithContentDescription("Avatar option 1").performScrollTo().performClick().assertIsSelected()
        capture("auth-register-avatar")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Account details").assertIsDisplayed()
        assertEquals("secret", vm.uiState.value.password)
        assertNotNull(vm.uiState.value.selectedSeed)
    }
    @Test fun narrowLargeTextRegistrationCanReachActionWithKeyboard() {
        val vm = RegisterViewModel(SavedStateHandle(), SignUpUseCase(auth))
        registration(vm, large = true)
        compose.onNodeWithContentDescription("Your name").performScrollTo().performTextInput("Alex")
        compose.onNodeWithContentDescription("Email").performScrollTo().performTextInput("alex@example.com")
        compose.onNodeWithContentDescription("Password").performScrollTo().performTextInput("secret")
        compose.onNodeWithText("Continue").performScrollTo().assertIsDisplayed()
        capture("auth-register-keyboard-large")
        compose.onNodeWithContentDescription("Password").performImeAction()
        compose.onNodeWithText("Step 2 of 2").assertIsDisplayed()
        capture("auth-register-avatar-large")
    }
    @Test fun greekRegistrationAtLargeFont() {
        registration(RegisterViewModel(SavedStateHandle(), SignUpUseCase(auth)), greek = true, large = true)
        compose.onNodeWithText("Στοιχεία λογαριασμού").assertExists()
        capture("auth-register-greek-large")
        compose.onNodeWithText("Συνέχεια").performScrollTo().performClick()
        compose.onNodeWithText("Πληκτρολογήστε μια έγκυρη διεύθυνση email.").performScrollTo().assertIsDisplayed()
    }
    @Test fun narrowLargeTextLogin() {
        render(greek = true, large = true) { LoginContent(LoginUiState(), {}, {}, {}, {}, {}, {}) }
        capture("auth-login-greek-large")
        compose.onNodeWithText("Σύνδεση", substring = false).performScrollTo().assertIsDisplayed()
    }
    @Test fun authCrossLinksReplaceDestinationAndBackReturnsToOrigin() {
        lateinit var nav: NavHostController
        render {
            nav = rememberNavController()
            NavHost(nav, startDestination = Screen.Onboarding) {
                composable<Screen.Onboarding> { Text("Origin") }
                composable<Screen.Login> { Text("Login destination") }
                composable<Screen.Register> { Text("Register destination") }
            }
        }
        compose.runOnIdle { nav.navigate(Screen.Login) }
        repeat(3) {
            compose.runOnIdle { nav.switchAuthScreen(Screen.Register) }
            compose.runOnIdle { assertTrue(nav.currentDestination!!.hasRoute<Screen.Register>()); nav.switchAuthScreen(Screen.Login) }
        }
        compose.runOnIdle { nav.popBackStack() }
        compose.runOnIdle { assertTrue(nav.currentDestination!!.hasRoute<Screen.Onboarding>()) }
    }
    @Test fun authenticationFromEitherFormClearsEntryScreens() {
        lateinit var nav: NavHostController
        render {
            nav = rememberNavController()
            NavHost(nav, startDestination = Screen.Onboarding) {
                composable<Screen.Onboarding> { Text("Origin") }
                composable<Screen.Login> { Text("Login destination") }
                composable<Screen.Register> { Text("Register destination") }
                composable<Screen.Home> { Text("Home destination") }
            }
        }
        listOf(Screen.Login, Screen.Register).forEach { destination ->
            compose.runOnIdle { nav.navigate(destination) }
            compose.runOnIdle {
                assertTrue(nav.currentDestination.isAuthEntry())
                nav.applyAuthRedirect(authRedirect(null, "signed-in", nav.currentDestination.isAuthEntry()))
            }
            compose.runOnIdle {
                assertTrue(nav.currentDestination!!.hasRoute<Screen.Home>())
                assertNull(nav.previousBackStackEntry)
            }
        }
    }
}
