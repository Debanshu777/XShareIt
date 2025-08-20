package com.debanshu.xshareit.domain.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Centralized navigation manager that emits navigation events
 * to be consumed by the NavigatorLaunchedEffect composable.
 * 
 * This separates navigation logic from ViewModels and UI components,
 * providing a clean way to handle navigation throughout the app.
 */
object NavigationManager {
    private val _navigationEvents = MutableSharedFlow<Route>(
        replay = 1,
        extraBufferCapacity = 64
    )
    val navigationEvents: SharedFlow<Route> = _navigationEvents

    /**
     * Navigate to a specific route synchronously.
     * Returns true if the event was emitted successfully.
     */
    fun navigateTo(route: Route): Boolean {
        println("[NavigationManager] Navigating to: $route")
        return _navigationEvents.tryEmit(route).also { success ->
            if (!success) {
                println("[NavigationManager] Failed to emit navigation event for: $route")
            }
        }
    }

    /**
     * Navigate to a specific route asynchronously.
     * Use this when you need to ensure the navigation event is delivered.
     */
    suspend fun navigateToAsync(route: Route) {
        println("[NavigationManager] Navigating async to: $route")
        _navigationEvents.emit(route)
    }
}
