package com.shakeandeat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shakeandeat.ui.groups.GroupsScreen
import com.shakeandeat.ui.login.LoginRoute

object Routes {
    const val LOGIN = "login"
    const val GROUPS = "groups"
}

@Composable
fun AppNavigation(sessionViewModel: SessionViewModel = viewModel()) {
    val navController = rememberNavController()
    val user by sessionViewModel.user.collectAsStateWithLifecycle()

    // Firebase remembers the session, so a returning user skips the login screen.
    val startDestination = remember {
        if (sessionViewModel.user.value != null) Routes.GROUPS else Routes.LOGIN
    }

    // Signing in moves to Groups and signing out returns to Login. Popping the old screen
    // means Back from Groups exits the app instead of returning to Login.
    LaunchedEffect(user) {
        val route = navController.currentDestination?.route ?: return@LaunchedEffect
        if (user == null && route != Routes.LOGIN) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(Routes.GROUPS) { inclusive = true }
            }
        } else if (user != null && route == Routes.LOGIN) {
            navController.navigate(Routes.GROUPS) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginRoute()
        }
        composable(Routes.GROUPS) {
            GroupsScreen(
                email = user?.email,
                onSignOut = sessionViewModel::signOut,
            )
        }
    }
}
