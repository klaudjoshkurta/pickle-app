package com.shkurta.pickle.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.shkurta.pickle.ui.auth.AuthScreen
import com.shkurta.pickle.ui.auth.AuthViewModel
import com.shkurta.pickle.ui.create.CreatePollScreen
import com.shkurta.pickle.ui.home.HomeScreen
import com.shkurta.pickle.ui.menu.MenuScreen
import com.shkurta.pickle.ui.notifications.NotificationsScreen
import com.shkurta.pickle.ui.onboarding.OnboardingScreen
import com.shkurta.pickle.ui.poll.PollScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val session by authViewModel.session.collectAsStateWithLifecycle()
    var previousUserId by rememberSaveable { mutableStateOf<String?>(null) }
    if (!session.initialized) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) { androidx.compose.material3.CircularProgressIndicator() }
        return
    }
    val openCreate: () -> Unit = {
        navController.navigate(if (session.userId == null) Screen.Auth() else Screen.CreatePoll())
    }
    LaunchedEffect(session) {
        val destination = navController.currentDestination
        val atEntry = destination?.hasRoute<Screen.Onboarding>() == true || destination?.hasRoute<Screen.Auth>() == true
        when (authRedirect(previousUserId, session.userId, atEntry)) {
            AuthRedirect.HOME -> navController.navigate(Screen.Home) { popUpTo(0) { inclusive = true } }
            AuthRedirect.ONBOARDING -> navController.navigate(Screen.Onboarding) { popUpTo(0) { inclusive = true } }
            AuthRedirect.NONE -> Unit
        }
        previousUserId = session.userId
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding,
        enterTransition = sharedAxisEnter,
        exitTransition = defaultExit,
        popEnterTransition = defaultPopEnter,
        popExitTransition = sharedAxisPopExit,
    ) {
        composable<Screen.Onboarding>(
            enterTransition = fadeThroughEnter,
            popEnterTransition = sharedAxisPopEnter,
        ) {
            OnboardingScreen(
                onCreateAccount = {
                    navController.navigate(Screen.Auth(signUp = true))
                },
                onTryIt = {
                    navController.navigate(Screen.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLogIn = {
                    navController.navigate(Screen.Auth(signUp = false))
                },
            )
        }
        composable<Screen.Auth> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.Auth>()
            AuthScreen(
                initialSignUp = route.signUp,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Screen.Home>(
            // Arriving from onboarding/auth replaces the whole back stack, so fade rather than slide
            enterTransition = {
                val fromRoot = initialState.destination.hasRoute<Screen.Onboarding>() ||
                    initialState.destination.hasRoute<Screen.Auth>()
                if (fromRoot) fadeThroughEnter() else sharedAxisEnter()
            },
        ) {
            HomeScreen(
                onPollClick = { pollId -> navController.navigate(Screen.Poll(pollId)) },
                onCreateClick = openCreate,
                onNotificationsClick = { navController.navigate(Screen.Notifications) },
                onMenuClick = { navController.navigate(Screen.Menu) },
                onTemplatesClick = { navController.navigate(Screen.Templates) },
                onStarterClick = { starter -> navController.navigate(if(session.userId==null) Screen.Auth() else Screen.CreatePoll(starterId=starter)) }
            )
        }
        composable<Screen.Templates> {
            com.shkurta.pickle.ui.templates.TemplatesScreen(
                onBack={navController.popBackStack()},
                onUse={starter,template -> navController.navigate(if(session.userId==null) Screen.Auth() else Screen.CreatePoll(starter,template))},
            )
        }
        composable<Screen.CreatePoll>(
            enterTransition = sheetEnter,
            popExitTransition = sheetPopExit,
            exitTransition = fadeThroughExit,
        ) {
            CreatePollScreen(
                onBack = { navController.popBackStack() },
                onCreated = { pollId ->
                    navController.navigate(Screen.Poll(pollId)) {
                        popUpTo<Screen.CreatePoll> { inclusive = true }
                    }
                }
            )
        }
        composable<Screen.Poll> {
            PollScreen(
                onTemplateSaved = { navController.navigate(Screen.Templates) },
                isDemo = session.userId == null,
                onSignIn = { navController.navigate(Screen.Auth()) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.Notifications> {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onPollClick = { pollId -> navController.navigate(Screen.Poll(pollId)) }
            )
        }
        composable<Screen.Menu> {
            MenuScreen(
                onBack = { navController.popBackStack() },
                onCreatePollClick = openCreate,
                onNotificationsClick = { navController.navigate(Screen.Notifications) },
                onSignedOut = {
                    navController.navigate(Screen.Onboarding) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
