package com.debanshu.xshareit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.debanshu.xshareit.domain.AppViewModel
import com.debanshu.xshareit.domain.model.ConnectionDTO
import com.debanshu.xshareit.domain.model.ConnectionType
import com.debanshu.xshareit.domain.navigation.NavigatorLaunchedEffect
import com.debanshu.xshareit.domain.navigation.Route
import com.debanshu.xshareit.ui.theme.XShareItTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import qrgenerator.qrkitpainter.rememberQrKitPainter
import qrscanner.CameraLens
import qrscanner.OverlayShape
import qrscanner.QrScanner

//send -> scan, client
//receive -> generate, server
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Preview
fun App() {
    val viewModel = koinViewModel<AppViewModel>()
    val navController = rememberNavController()
    XShareItTheme {
        val uiState by viewModel.uiState.collectAsState()

        // Listen and respond to navigation events
        NavigatorLaunchedEffect(navController = navController)

        Scaffold {
            NavHost(
                navController = navController,
                startDestination = Route.Selection,
            ) {
                composable<Route.Selection> {
                    SelectionScreen { connectionType ->
                        println("[App] Selection screen - User selected: $connectionType")
                        viewModel.setConnectionType(connectionType)
                    }
                }

                composable<Route.SenderInit> {
                    println("[App] Displaying SenderInit screen")
                    SenderScaningScreen { scannedData ->
                        println("[App] QR code scanned: $scannedData")
                        val data = scannedData.split(":")
                        if (data.size == 2) {
                            viewModel.setUiState(
                                AppViewModel.XShareItUiState.SenderEmittingState(
                                    ConnectionDTO(
                                        data[0], data[1].toInt()
                                    )
                                )
                            )
                        }
                    }
                }
                composable<Route.SenderEmitting> {
                    if (uiState is AppViewModel.XShareItUiState.SenderEmittingState) {
                        val connection = (uiState as AppViewModel.XShareItUiState
                        .SenderEmittingState)
                            .connection
                        SenderEmittingScreen(
                            connection,
                            onDataSent = { data ->
                                viewModel.sendData(data, connection)
                            }
                        )
                    }
                }

                composable<Route.SenderCompleted> {
                    if (uiState is AppViewModel.XShareItUiState.SenderCompletedState) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Send Completed ${(uiState as AppViewModel.XShareItUiState.SenderCompletedState).data}")
                        }
                    }
                }



                composable<Route.ReceiverInit> {
                    println("[App] Displaying ReceiverInit screen")
                    if (uiState is AppViewModel.XShareItUiState.ReceiverWaitingState) {
                        ReciverWaitingScreen((uiState as AppViewModel.XShareItUiState.ReceiverWaitingState).connection)
                    }
                }

                composable<Route.ReceiverConsuming> {
                    // TODO: Implement receiver consuming screen
                }

                composable<Route.ReceiverCompleted> {
                    if (uiState is AppViewModel.XShareItUiState.ReceiverCompletedState) {
                        Text("Receiver Completed ${(uiState as AppViewModel.XShareItUiState.ReceiverCompletedState).data}")
                    }
                }

                composable<Route.Error> {
                    if (uiState is AppViewModel.XShareItUiState.ErrorState) {
                        Text("Error: ${(uiState as AppViewModel.XShareItUiState.ErrorState).error}")
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionScreen(
    setConnectionType: (ConnectionType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            setConnectionType(ConnectionType.Sender)
        }) {
            Text(text = "Send")
        }
        Button(onClick = {
            setConnectionType(ConnectionType.Receiver)
        }) {
            Text(text = "Receive")
        }
    }
}

@Composable
fun ReciverWaitingScreen(connectionDTO: ConnectionDTO) {
    val painter = rememberQrKitPainter(data = "${connectionDTO.ip}:${connectionDTO.port}")
    Column(
        modifier = Modifier.fillMaxSize().background(color = XShareItTheme.colorScheme.surface),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${connectionDTO.ip}:${connectionDTO.port}")
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .background(XShareItTheme.colorScheme.surfaceContainerLow)
                .padding(5.dp)
        )
    }
}

@Composable
fun SenderEmittingScreen(
    connectionDTO: ConnectionDTO,
    onDataSent: (String) -> Unit,
) {
    var textToSend by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sending Data to: ${connectionDTO.ip}:${connectionDTO.port}")
        
        OutlinedTextField(
            value = textToSend,
            onValueChange = { textToSend = it },
            label = { Text("Enter data to send") },
            placeholder = { Text("Type your message here...") },
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        Button(
            onClick = {
                if (textToSend.isNotBlank()) {
                    onDataSent(textToSend)
                }
            },
            enabled = textToSend.isNotBlank()
        ) {
            Text("Send Data")
        }
    }
}

@Composable
fun SenderScaningScreen(
    onSuccessfulScan: (String) -> Unit,
) {
    val zoomLevels = listOf(1f, 2f, 3f)
    val selectedZoomIndex = 0
    val coroutineScope = rememberCoroutineScope()
    var flashlightOn by remember { mutableStateOf(false) }
    var openImagePicker by remember { mutableStateOf(false) }
    var overlayShape by remember { mutableStateOf(OverlayShape.Square) }
    var cameraLens by remember { mutableStateOf(CameraLens.Back) }
    var currentZoomLevel by remember { mutableStateOf(zoomLevels[selectedZoomIndex]) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        QrScanner(
            modifier = Modifier.fillMaxSize(),
            flashlightOn = flashlightOn,
            cameraLens = cameraLens,
            openImagePicker = openImagePicker,
            onCompletion = {
                coroutineScope.launch {
                    delay(1000)
                    onSuccessfulScan(it)
                }
            },
            zoomLevel = currentZoomLevel,
            maxZoomLevel = 3f,
            imagePickerHandler = { openImagePicker = it },
            onFailure = {
                print("Error Here $it")
            },
            overlayShape = overlayShape
        )

        Button(
            modifier = Modifier.align(Alignment.Center).padding(top = 12.dp),
            onClick = { openImagePicker = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5144D8))
        ) {
            Text(
                text = "Select Image",
            )
        }
    }
}