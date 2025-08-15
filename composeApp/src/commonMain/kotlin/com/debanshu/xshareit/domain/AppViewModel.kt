package com.debanshu.xshareit.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debanshu.xshareit.data.getDeviceIpAddress
import com.debanshu.xshareit.domain.model.ConnectionDTO
import com.debanshu.xshareit.domain.model.ConnectionType
import com.debanshu.xshareit.domain.model.SenderDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class AppViewModel : ViewModel() {
    private val _connectionType = MutableStateFlow<ConnectionType?>(null)
    private val _uiState = MutableStateFlow(XShareItUiState.SelectionState)
    private val token = "my_secure_token"
    private val port = 8080

    val uiState = combine(
        _uiState,
        _connectionType
    ) { _, connectionState ->
        when(connectionState){
            ConnectionType.Receiver -> XShareItUiState.ReceiverInitState

            ConnectionType.Sender -> XShareItUiState.SenderWaitingState(
                    connection = ConnectionDTO(
                        ip = getDeviceIpAddress() ?: "Unknown",
                        port = port
                    )
                )

           else -> XShareItUiState.SelectionState
        }
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = XShareItUiState.SelectionState,
    )

    fun setConnectionType(connectionType: ConnectionType) {
        _connectionType.tryEmit(connectionType)
    }
    sealed class XShareItUiState {
        object SelectionState : XShareItUiState()

        data class SenderWaitingState(
            val connection: ConnectionDTO
        ) : XShareItUiState()
        data class SenderEmittingState(
            val senderDTO: SenderDTO
        ): XShareItUiState()
        data object SenderCompletedState: XShareItUiState()

        data object ReceiverInitState : XShareItUiState()
        data class ReceiverConsumingState(
            val connection: ConnectionDTO
        ): XShareItUiState()
        data class ReceiverCompletedState(
            val data: String
        ): XShareItUiState()

        data class ErrorState(val error: String) : XShareItUiState()
    }
}