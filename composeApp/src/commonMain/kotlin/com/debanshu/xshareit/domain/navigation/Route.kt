package com.debanshu.xshareit.domain.navigation

import kotlinx.serialization.Serializable

/**
 * Sealed interface defining all possible navigation destinations in the app.
 * Each route is serializable to support type-safe navigation.
 */
sealed interface Route {
    @Serializable
    data object Selection : Route

    @Serializable
    data object SenderInit : Route

    @Serializable
    data object SenderEmitting : Route

    @Serializable
    data object SenderCompleted : Route

    @Serializable
    data object ReceiverInit : Route

    @Serializable
    data object ReceiverConsuming : Route

    @Serializable
    data object ReceiverCompleted : Route

    @Serializable
    data object Error : Route
}
