package com.hhc558.passcontrol.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hhc558.passcontrol.PassControlApp
import com.hhc558.passcontrol.ui.edit.EditScreen
import com.hhc558.passcontrol.ui.forgot.ForgotScreen
import com.hhc558.passcontrol.ui.login.LoginScreen
import com.hhc558.passcontrol.ui.main.MainScreen
import com.hhc558.passcontrol.ui.preview.ImportPreviewScreen
import com.hhc558.passcontrol.ui.setup.SetupScreen

@Composable
fun AppNavHost() {
    val context = LocalContext.current
    val app = context.applicationContext as PassControlApp
    val startDestination = remember {
        if (app.container.vaultRepository.isInitialized()) "login" else "setup"
    }
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable("setup") { SetupScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("forgot") { ForgotScreen(navController) }
        composable("main") { MainScreen(navController) }
        composable(
            route = "edit?accountId={accountId}",
            arguments = listOf(navArgument("accountId") { type = NavType.LongType; defaultValue = -1L })
        ) { entry ->
            EditScreen(navController, entry.arguments?.getLong("accountId") ?: -1L)
        }
        composable("import_preview") { ImportPreviewScreen(navController) }
    }
}