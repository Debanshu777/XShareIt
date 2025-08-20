package com.debanshu.xshareit.domain.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController

/**
 * This composable LaunchedEffect collects navigation events
 * from the NavigationManager and navigates to the next
 * emitted screen.
 * 
 * Place this composable at the top level of your navigation setup
 * to automatically handle all navigation events emitted by ViewModels
 * or other components.
 * 
 * @param navController The NavHostController to perform navigation
 */
@Composable
fun NavigatorLaunchedEffect(
    navController: NavHostController,
) {
    LaunchedEffect("NavigationEvents") {
        println("[NavigatorLaunchedEffect] Started collecting navigation events")
        NavigationManager.navigationEvents.collect { route ->
            println("[NavigatorLaunchedEffect] Received navigation event: $route")
            try {
                navController.navigate(route)
                println("[NavigatorLaunchedEffect] Successfully navigated to: $route")
            } catch (e: Exception) {
                println("[NavigatorLaunchedEffect] Failed to navigate to $route: ${e.message}")
            }
        }
    }
}
