package com.debanshu.xshareit.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debanshu.xshareit.data.Result
import com.debanshu.xshareit.data.ShareManager
import com.debanshu.xshareit.data.getDeviceIpAddress
import com.debanshu.xshareit.domain.model.ConnectionDTO
import com.debanshu.xshareit.domain.model.ConnectionType
import com.debanshu.xshareit.domain.model.ReceiverDTO
import com.debanshu.xshareit.domain.model.SenderDTO
import com.debanshu.xshareit.domain.navigation.NavigationManager
import com.debanshu.xshareit.domain.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent

@KoinViewModel
class AppViewModel(
    private var shareManager: ShareManager
) : ViewModel(), KoinComponent {
    private val _connectionType = MutableStateFlow<ConnectionType?>(null)
    private val _uiState: MutableStateFlow<XShareItUiState?> = MutableStateFlow(null)
    private val port = 8080

    private val tag = "AppViewModel"

    init {
        viewModelScope.launch {
            shareManager.receivedData.collect { data ->
                println("[$tag] Received data: $data")
                data?.let {
                    println("[$tag] Processing received data: ${it.data}")
                    _uiState.tryEmit(XShareItUiState.ReceiverCompletedState(it.data))
                    // Navigate to completed screen when data is received
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(50)
                        NavigationManager.navigateToAsync(Route.ReceiverCompleted)
                    }
                }
            }
        }
    }

    val uiState = combine(
        _uiState,
        _connectionType
    ) { state, connectionState ->
        state?.let {
            it
        } ?: when (connectionState) {
            ConnectionType.Receiver -> {
                val connection = ConnectionDTO(
                    ip = getDeviceIpAddress() ?: "Unknown",
                    port = port
                )
                val result = shareManager.startReceiver(
                    ReceiverDTO(connection)
                )
                when (result) {
                    is Result.Error -> XShareItUiState.ErrorState(error = result.error.name)
                    is Result.Success -> XShareItUiState.ReceiverWaitingState(
                        connection = connection
                    )
                }
            }

            ConnectionType.Sender -> XShareItUiState.SenderInitState

            else -> XShareItUiState.SelectionState
        }
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = XShareItUiState.SelectionState,
    )

    fun setConnectionType(connectionType: ConnectionType) {
        println("[$tag] Setting connection type: $connectionType")
        _connectionType.tryEmit(connectionType)
        
        // Navigate based on connection type with slight delay to ensure state is updated
        viewModelScope.launch {
            kotlinx.coroutines.delay(50) // Small delay to ensure state is processed
            when (connectionType) {
                ConnectionType.Sender -> NavigationManager.navigateToAsync(Route.SenderInit)
                ConnectionType.Receiver -> NavigationManager.navigateToAsync(Route.ReceiverInit)
            }
        }
    }

    fun sendData(data: String,connectionDTO: ConnectionDTO) {
        println("[$tag] Attempting to send data: $data")
        viewModelScope.launch {
            shareManager.sendData(
                SenderDTO(
                    connectionDTO = ConnectionDTO(
                        ip = connectionDTO.ip,
                        port = connectionDTO.port
                    ),
                    data = data
                )
            ).let { result ->
                when (result) {
                    is Result.Success -> {
                        println("[$tag] Data sent successfully, updating UI to completed state")
                        _uiState.tryEmit(XShareItUiState.SenderCompletedState(data))
                        kotlinx.coroutines.delay(50)
                        NavigationManager.navigateToAsync(Route.SenderCompleted)
                    }

                    is Result.Error -> {
                        val errorMessage = "Failed to send data: ${result.error}"
                        println("[$tag] Failed to send data: $errorMessage")
                        _uiState.tryEmit(XShareItUiState.ErrorState(errorMessage))
                        kotlinx.coroutines.delay(50)
                        NavigationManager.navigateToAsync(Route.Error)
                    }
                }
            }
        }
    }

    fun stopConnection() {
        println("[$tag] Stopping connection and cleaning up")
        shareManager.stopServer()
        println("[$tag] Connection stopped and ShareManager cleaned up")
    }

    fun setUiState(uiState: XShareItUiState) {
        println("[$tag] Manually setting UI state to: $uiState")
        _uiState.tryEmit(uiState)
        
        // Navigate based on UI state if needed
        viewModelScope.launch {
            kotlinx.coroutines.delay(50) // Small delay to ensure state is processed
            when (uiState) {
                is XShareItUiState.SenderEmittingState -> NavigationManager.navigateToAsync(Route.SenderEmitting)
                is XShareItUiState.ErrorState -> NavigationManager.navigateToAsync(Route.Error)
                else -> { /* Other states don't require immediate navigation */ }
            }
        }
    }

    override fun onCleared() {
        println("[$tag] ViewModel being cleared, cleaning up resources")
        super.onCleared()
        stopConnection()
        println("[$tag] ViewModel cleanup completed")
    }

    sealed class XShareItUiState {
        object SelectionState : XShareItUiState()

        data class ReceiverWaitingState(
            val connection: ConnectionDTO
        ) : XShareItUiState()

        data class ReceiverConsumingState(
            val senderDTO: SenderDTO
        ) : XShareItUiState()

        data class ReceiverCompletedState(val data:String) : XShareItUiState()

        data object SenderInitState : XShareItUiState()
        data class SenderEmittingState(
            val connection: ConnectionDTO
        ) : XShareItUiState()

        data class SenderCompletedState(
            val data: String
        ) : XShareItUiState()

        data class ErrorState(val error: String) : XShareItUiState()
    }
}