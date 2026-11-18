=== ABOUT: The map of the app — which screen leads to which ===
package com.aibuilt.blackvault.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aibuilt.blackvault.ui.screens.HistoryScreen
import com.aibuilt.blackvault.ui.screens.PayScreen
import com.aibuilt.blackvault.ui.screens.ReceiptScreen
import com.aibuilt.blackvault.ui.screens.RunScreen
import com.aibuilt.blackvault.ui.screens.SettingsScreen
import com.aibuilt.blackvault.ui.screens.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val PAY = "pay"
    const val RUN = "run"
    const val RECEIPT = "receipt"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun VaultNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(onDone = {
                nav.navigate(Routes.PAY) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }

        composable(Routes.PAY) {
            PayScreen(
                onRun = { id -> nav.navigate("${Routes.RUN}/$id") },
                onHistory = { nav.navigate(Routes.HISTORY) },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = "${Routes.RUN}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            RunScreen(
                onReceipt = { id ->
                    nav.navigate("${Routes.RECEIPT}/$id") {
                        popUpTo(Routes.PAY)
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = "${Routes.RECEIPT}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            ReceiptScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onOpen = { id -> nav.navigate("${Routes.RECEIPT}/$id") },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
