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
            shareManager.receivedData?.collect { data ->
                println("[$tag] Received data: $data")
                data?.let {
                    _uiState.tryEmit(XShareItUiState.ReceiverCompletedState(it.toString()))
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

    fun setConnectionType(connectionType: ConnectionType): Boolean {
        println("[$tag] Setting connection type: $connectionType")
        return _connectionType.tryEmit(connectionType)
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
                    }

                    is Result.Error -> {
                        val errorMessage = "Failed to send data: ${result.error}"
                        println("[$tag] Failed to send data: $errorMessage")
                        _uiState.tryEmit(XShareItUiState.ErrorState(errorMessage))
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

    fun setUiState(uiState: XShareItUiState): Boolean {
        println("[$tag] Manually setting UI state to: $uiState")
        return _uiState.tryEmit(uiState)
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