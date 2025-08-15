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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.debanshu.xshareit.domain.AppViewModel
import com.debanshu.xshareit.domain.model.ConnectionDTO
import com.debanshu.xshareit.domain.model.ConnectionType
import com.debanshu.xshareit.ui.theme.XShareItTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import qrgenerator.qrkitpainter.rememberQrKitPainter
import qrscanner.CameraLens
import qrscanner.OverlayShape
import qrscanner.QrScanner

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Preview
fun App() {
    val viewModel = koinViewModel<AppViewModel>()
    XShareItTheme {
        val uiState by viewModel.uiState.collectAsState()
        Scaffold {
            when (uiState) {
                AppViewModel.XShareItUiState.SelectionState -> {
                    SelectionScreen(setConnectionType = viewModel::setConnectionType)
                }

                is AppViewModel.XShareItUiState.ReceiverCompletedState -> {}
                is AppViewModel.XShareItUiState.ReceiverConsumingState -> {}
                AppViewModel.XShareItUiState.ReceiverInitState -> {
                    ReceiverInitScreen()
                }

                AppViewModel.XShareItUiState.SenderCompletedState -> {}
                is AppViewModel.XShareItUiState.SenderEmittingState -> {}
                is AppViewModel.XShareItUiState.SenderWaitingState -> {
                    SenderWaitingScreen(
                        (uiState as AppViewModel.XShareItUiState.SenderWaitingState)
                            .connection
                    )
                }

                is AppViewModel.XShareItUiState.ErrorState -> {}
            }
        }
    }
}

@Composable
fun SelectionScreen(
    setConnectionType: (ConnectionType) -> Unit
){
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
fun SenderWaitingScreen(connectionDTO: ConnectionDTO) {
    val painter = rememberQrKitPainter(data = "${connectionDTO.ip}:${connectionDTO.port}")
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${connectionDTO.ip}:${connectionDTO.port}")
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 20.dp)
                .size(200.dp)
                .background(XShareItTheme.colorScheme.onSurface)
        )
    }
}

@Composable
fun ReceiverInitScreen() {
    val zoomLevels = listOf(1f, 2f, 3f)
    var selectedZoomIndex = 0

    var qrCodeURL by remember { mutableStateOf("") }
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
        // QR Scanner Camera Preview
        QrScanner(
            modifier = Modifier.fillMaxSize(),
            flashlightOn = flashlightOn,
            cameraLens = cameraLens,
            openImagePicker = openImagePicker,
            onCompletion = { qrCodeURL = it },
            zoomLevel = currentZoomLevel,
            maxZoomLevel = 3f,
            imagePickerHandler = { openImagePicker = it },
            onFailure = {
                print(it)
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
        if (qrCodeURL.isNotEmpty()) {
            Text("QR Code URL: $qrCodeURL")
        }
    }
}