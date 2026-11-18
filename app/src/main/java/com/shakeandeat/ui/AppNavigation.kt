package com.shakeandeat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shakeandeat.ui.groups.GroupDetailRoute
import com.shakeandeat.ui.chat.ChatRoute
import com.shakeandeat.ui.groups.GroupsRoute
import com.shakeandeat.ui.login.LoginRoute
import com.shakeandeat.ui.pick.TodaysPickRoute

object Routes {
    const val LOGIN = "login"
    const val GROUPS = "groups"
    const val GROUP = "group/{gid}"
    const val PICK = "group/{gid}/pick"
    const val CHAT = "group/{gid}/chat"
}

@Composable
fun AppNavigation(sessionViewModel: SessionViewModel = viewModel()) {
    val navController = rememberNavController()
    val user by sessionViewModel.user.collectAsStateWithLifecycle()

    // Firebase remembers the session, so a returning user skips the login screen.
    val startDestination = remember {
        if (sessionViewModel.user.value != null) Routes.GROUPS else Routes.LOGIN
    }

    // Signing in moves to Groups and signing out returns to Login. Popping the old screen exits the app
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
            GroupsRoute(
                email = user?.email,
                onOpenGroup = { code -> navController.navigate("group/$code") },
                onSignOut = sessionViewModel::signOut,
            )
        }
        composable(
            route = Routes.GROUP,
            arguments = listOf(navArgument("gid") { type = NavType.StringType }),
        ) { entry ->
            val gid = entry.arguments?.getString("gid").orEmpty()
            GroupDetailRoute(
                onBack = { navController.popBackStack() },
                onOpenPick = { navController.navigate("group/$gid/pick") { launchSingleTop = true } },
                onOpenChat = { navController.navigate("group/$gid/chat") { launchSingleTop = true } },
            )
        }
        composable(
            route = Routes.PICK,
            arguments = listOf(navArgument("gid") { type = NavType.StringType }),
        ) { entry ->
            val gid = entry.arguments?.getString("gid").orEmpty()
            TodaysPickRoute(
                onBack = { navController.popBackStack() },
                onOpenChat = { navController.navigate("group/$gid/chat") { launchSingleTop = true } },
            )
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("gid") { type = NavType.StringType }),
        ) {
            ChatRoute(onBack = { navController.popBackStack() })
        }
    }
}
