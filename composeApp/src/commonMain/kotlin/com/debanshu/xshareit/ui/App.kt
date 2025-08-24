package com.debanshu.xshareit.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.debanshu.xshareit.domain.AppViewModel
import com.debanshu.xshareit.domain.model.ConnectionDTO
import com.debanshu.xshareit.domain.model.ConnectionType
import com.debanshu.xshareit.domain.navigation.NavigatorLaunchedEffect
import com.debanshu.xshareit.domain.navigation.Route
import com.debanshu.xshareit.ui.components.ErrorScreen
import com.debanshu.xshareit.ui.components.LoadingScreen
import com.debanshu.xshareit.ui.screens.CompletionScreen
import com.debanshu.xshareit.ui.screens.ReceiverWaitingScreen
import com.debanshu.xshareit.ui.screens.SelectionScreen
import com.debanshu.xshareit.ui.screens.SenderEmittingScreen
import com.debanshu.xshareit.ui.screens.SenderScanningScreen
import com.debanshu.xshareit.ui.theme.XShareItTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Preview
fun App() {
    val viewModel = koinViewModel<AppViewModel>()
    val navController = rememberNavController()
    
    XShareItTheme {
        val uiState by viewModel.uiState.collectAsState()

        NavigatorLaunchedEffect(navController = navController)

        Scaffold {
            NavHost(
                navController = navController,
                startDestination = Route.Selection,
            ) {
                composable<Route.Selection> {
                    when (uiState) {
                        is AppViewModel.XShareItUiState.LoadingState -> {
                            LoadingScreen(message = "Setting up connection...")
                        }
                        else -> {
                            SelectionScreen(
                                onConnectionTypeSelected = viewModel::setConnectionType
                            )
                        }
                    }
                }

                composable<Route.SenderInit> {
                    SenderScanningScreen { scannedData ->
                        parseQrCodeAndNavigate(scannedData, viewModel)
                    }
                }
                
                composable<Route.SenderEmitting> {
                    when (val state = uiState) {
                        is AppViewModel.XShareItUiState.SenderEmittingState -> {
                            SenderEmittingScreen(
                                connection = state.connection,
                                onDataSent = { data ->
                                    viewModel.sendData(data, state.connection)
                                }
                            )
                        }
                        is AppViewModel.XShareItUiState.LoadingState -> {
                            LoadingScreen(message = "Sending data...")
                        }
                        else -> {
                            ErrorScreen(
                                error = "Invalid state for sending data",
                                onRetry = { viewModel.setConnectionType(com.debanshu.xshareit.domain.model.ConnectionType.Sender) }
                            )
                        }
                    }
                }

                composable<Route.SenderCompleted> {
                    when (val state = uiState) {
                        is AppViewModel.XShareItUiState.SenderCompletedState -> {
                            CompletionScreen(
                                title = "Data Sent Successfully!",
                                message = "Your data has been sent to the receiver.",
                                data = state.data,
                                onBackToStart = { 
                                    navController.navigate(Route.Selection) {
                                        popUpTo(Route.Selection) { inclusive = true }
                                    }
                                }
                            )
                        }
                        else -> {
                            LoadingScreen(message = "Processing...")
                        }
                    }
                }

                composable<Route.ReceiverInit> {
                    when (val state = uiState) {
                        is AppViewModel.XShareItUiState.ReceiverWaitingState -> {
                            ReceiverWaitingScreen(connection = state.connection)
                        }
                        is AppViewModel.XShareItUiState.LoadingState -> {
                            LoadingScreen(message = "Starting receiver...")
                        }
                        else -> {
                            ErrorScreen(
                                error = "Failed to start receiver",
                                onRetry = { viewModel.setConnectionType(ConnectionType.Receiver) }
                            )
                        }
                    }
                }

                composable<Route.ReceiverConsuming> {
                    LoadingScreen(message = "Receiving data...")
                }

                composable<Route.ReceiverCompleted> {
                    when (val state = uiState) {
                        is AppViewModel.XShareItUiState.ReceiverCompletedState -> {
                            CompletionScreen(
                                title = "Data Received Successfully!",
                                message = "You have received data from the sender.",
                                data = state.data,
                                onBackToStart = { 
                                    navController.navigate(Route.Selection) {
                                        popUpTo(Route.Selection) { inclusive = true }
                                    }
                                }
                            )
                        }
                        else -> {
                            LoadingScreen(message = "Processing received data...")
                        }
                    }
                }

                composable<Route.Error> {
                    when (val state = uiState) {
                        is AppViewModel.XShareItUiState.ErrorState -> {
                            ErrorScreen(
                                error = state.error,
                                onRetry = { 
                                    navController.navigate(Route.Selection) {
                                        popUpTo(Route.Selection) { inclusive = true }
                                    }
                                }
                            )
                        }
                        else -> {
                            ErrorScreen(
                                error = "An unknown error occurred",
                                onRetry = { 
                                    navController.navigate(Route.Selection) {
                                        popUpTo(Route.Selection) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseQrCodeAndNavigate(scannedData: String, viewModel: AppViewModel) {
    val data = scannedData.split(":")
    if (data.size == 2) {
        try {
            val connection = ConnectionDTO(
                ip = data[0],
                port = data[1].toInt()
            )
            viewModel.setUiState(
                AppViewModel.XShareItUiState.SenderEmittingState(connection)
            )
        } catch (e: NumberFormatException) {
            viewModel.setUiState(
                AppViewModel.XShareItUiState.ErrorState("Invalid QR code format")
            )
        }
    } else {
        viewModel.setUiState(
            AppViewModel.XShareItUiState.ErrorState("Invalid QR code data")
        )
    }
}
