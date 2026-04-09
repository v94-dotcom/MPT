package com.mpt.masterpasswordtrainer.ui.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.mpt.masterpasswordtrainer.ui.screens.addentry.AddEntryScreen
import com.mpt.masterpasswordtrainer.ui.screens.challenge.ChallengeScreen
import com.mpt.masterpasswordtrainer.ui.screens.dashboard.DashboardScreen
import com.mpt.masterpasswordtrainer.ui.screens.onboarding.OnboardingScreen
import com.mpt.masterpasswordtrainer.ui.screens.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding?isReplay={isReplay}"
    const val DASHBOARD = "dashboard"
    const val ADD_ENTRY = "add_entry?isFromOnboarding={isFromOnboarding}&entryId={entryId}"
    const val CHALLENGE = "challenge/{entryId}"
    const val SETTINGS = "settings"

    fun onboarding(isReplay: Boolean = false) = "onboarding?isReplay=$isReplay"

    fun addEntry(isFromOnboarding: Boolean = false, entryId: String? = null): String {
        val base = "add_entry?isFromOnboarding=$isFromOnboarding"
        return if (entryId != null) "$base&entryId=$entryId" else base
    }

    fun challenge(entryId: String) = "challenge/$entryId"
}

@Composable
fun MPTNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = Routes.ONBOARDING,
            arguments = listOf(
                navArgument("isReplay") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val isReplay = backStackEntry.arguments?.getBoolean("isReplay") ?: false
            OnboardingScreen(navController = navController, isReplay = isReplay)
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(navController = navController)
        }
        composable(
            route = Routes.ADD_ENTRY,
            arguments = listOf(
                navArgument("isFromOnboarding") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("entryId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val isFromOnboarding = backStackEntry.arguments?.getBoolean("isFromOnboarding") ?: false
            val entryId = backStackEntry.arguments?.getString("entryId")
            AddEntryScreen(
                navController = navController,
                isFromOnboarding = isFromOnboarding,
                entryId = entryId
            )
        }
        composable(
            route = Routes.CHALLENGE,
            arguments = listOf(
                navArgument("entryId") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "mpt://challenge/{entryId}" }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
            ChallengeScreen(navController = navController, entryId = entryId)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
    }
}

fun getStartDestination(context: Context): String {
    val prefs = context.getSharedPreferences("mpt_prefs", Context.MODE_PRIVATE)
    val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
    return if (onboardingCompleted) Routes.DASHBOARD else Routes.onboarding()
}
