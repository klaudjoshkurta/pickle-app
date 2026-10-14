package com.shkurta.pickle.ui.navigation

import androidx.compose.runtime.Composable
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
    val isAuthenticated by authViewModel.isAuthenticated.collectAsStateWithLifecycle()

    // When auth state changes to authenticated, navigate to Home
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            navController.navigate(Screen.Home) {
                popUpTo(0) { inclusive = true }
            }
        }
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
                onCreateClick = { navController.navigate(Screen.CreatePoll) },
                onNotificationsClick = { navController.navigate(Screen.Notifications) },
                onMenuClick = { navController.navigate(Screen.Menu) }
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
                onCreatePollClick = { navController.navigate(Screen.CreatePoll) },
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
