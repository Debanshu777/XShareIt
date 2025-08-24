package com.debanshu.xshareit.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debanshu.xshareit.data.Result
import com.debanshu.xshareit.data.ShareManager

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
    private val shareManager: ShareManager
) : ViewModel(), KoinComponent {
    private val _connectionType = MutableStateFlow<ConnectionType?>(null)
    private val _uiState: MutableStateFlow<XShareItUiState?> = MutableStateFlow(null)
    private val _isLoading = MutableStateFlow(false)
    private val port = 8080

    private val tag = "AppViewModel"

    init {
        viewModelScope.launch {
            shareManager.receivedData.collect { data ->
                println("[$tag] Received data: $data")
                data?.let {
                    println("[$tag] Processing received data: ${it.data}")
                    _uiState.value = XShareItUiState.ReceiverCompletedState(it.data)
                    viewModelScope.launch {
                        NavigationManager.navigateToAsync(Route.ReceiverCompleted)
                    }
                }
            }
        }
    }

    val uiState = combine(
        _uiState,
        _connectionType,
        _isLoading
    ) { state, connectionState, isLoading ->
        if (isLoading) {
            XShareItUiState.LoadingState
        } else {
            state ?: when (connectionState) {
                ConnectionType.Receiver -> initializeReceiver()
                ConnectionType.Sender -> XShareItUiState.SenderInitState
                else -> XShareItUiState.SelectionState
            }
        }
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = XShareItUiState.SelectionState,
    )

    private fun initializeReceiver(): XShareItUiState {
        val deviceIp = try {
            com.debanshu.xshareit.data.getDeviceIpAddress() ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
        val connection = ConnectionDTO(
            ip = deviceIp,
            port = port
        )
        val result = shareManager.startReceiver(ReceiverDTO(connection))
        return when (result) {
            is Result.Error -> XShareItUiState.ErrorState(error = result.error.name)
            is Result.Success -> XShareItUiState.ReceiverWaitingState(connection = connection)
        }
    }

    fun setConnectionType(connectionType: ConnectionType) {
        println("[$tag] Setting connection type: $connectionType")
        _isLoading.value = true
        _connectionType.value = connectionType
        
        viewModelScope.launch {
            try {
                when (connectionType) {
                    ConnectionType.Sender -> NavigationManager.navigateToAsync(Route.SenderInit)
                    ConnectionType.Receiver -> NavigationManager.navigateToAsync(Route.ReceiverInit)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendData(data: String, connectionDTO: ConnectionDTO) {
        if (data.isBlank()) {
            println("[$tag] Cannot send empty data")
            return
        }
        
        println("[$tag] Attempting to send data: $data")
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val result = shareManager.sendData(
                    SenderDTO(
                        connectionDTO = connectionDTO,
                        data = data
                    )
                )
                
                when (result) {
                    is Result.Success -> {
                        println("[$tag] Data sent successfully")
                        _uiState.value = XShareItUiState.SenderCompletedState(data)
                        NavigationManager.navigateToAsync(Route.SenderCompleted)
                    }
                    is Result.Error -> {
                        val errorMessage = "Failed to send data: ${result.error}"
                        println("[$tag] $errorMessage")
                        _uiState.value = XShareItUiState.ErrorState(errorMessage)
                        NavigationManager.navigateToAsync(Route.Error)
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun stopConnection() {
        println("[$tag] Stopping connection and cleaning up")
        shareManager.stopServer()
        println("[$tag] Connection stopped and ShareManager cleaned up")
    }

    fun setUiState(uiState: XShareItUiState) {
        println("[$tag] Setting UI state to: $uiState")
        _uiState.value = uiState
        
        viewModelScope.launch {
            when (uiState) {
                is XShareItUiState.SenderEmittingState -> NavigationManager.navigateToAsync(Route.SenderEmitting)
                is XShareItUiState.ErrorState -> NavigationManager.navigateToAsync(Route.Error)
                else -> { /* Other states handled by navigation flow */ }
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
        data object SelectionState : XShareItUiState()
        data object LoadingState : XShareItUiState()

        data class ReceiverWaitingState(
            val connection: ConnectionDTO
        ) : XShareItUiState()

        data class ReceiverConsumingState(
            val senderDTO: SenderDTO
        ) : XShareItUiState()

        data class ReceiverCompletedState(
            val data: String
        ) : XShareItUiState()

        data object SenderInitState : XShareItUiState()
        
        data class SenderEmittingState(
            val connection: ConnectionDTO
        ) : XShareItUiState()

        data class SenderCompletedState(
            val data: String
        ) : XShareItUiState()

        data class ErrorState(
            val error: String
        ) : XShareItUiState()
    }
}