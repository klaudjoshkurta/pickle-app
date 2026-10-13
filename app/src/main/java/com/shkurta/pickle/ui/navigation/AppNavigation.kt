package com.shkurta.pickle.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.shkurta.pickle.ui.create.CreatePollScreen
import com.shkurta.pickle.ui.home.HomeScreen
import com.shkurta.pickle.ui.poll.PollScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home,
    ) {
        composable<Screen.Home> {
            HomeScreen(
                onPollClick = { pollId -> navController.navigate(Screen.Poll(pollId)) },
                onCreateClick = { navController.navigate(Screen.CreatePoll) }
            )
        }
        composable<Screen.CreatePoll> {
            CreatePollScreen(
                onBack = { navController.popBackStack() },
                onCreated = { pollId ->
                    navController.navigate(Screen.Poll(pollId)) {
                        popUpTo<Screen.CreatePoll> { inclusive = true }
                    }
                }
            )
        }
        composable<Screen.Poll> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.Poll>()
            PollScreen(
                pollId = route.pollId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.Notifications> {

        }
        composable<Screen.Menu> {

        }
    }
}
