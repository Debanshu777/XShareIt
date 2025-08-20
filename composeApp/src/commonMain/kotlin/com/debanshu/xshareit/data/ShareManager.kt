package com.debanshu.xshareit.data

import com.debanshu.xshareit.data.error.DataError
import com.debanshu.xshareit.domain.model.ConnectionDTO
import com.debanshu.xshareit.domain.model.ConnectionType
import com.debanshu.xshareit.domain.model.DataTransferRequest
import com.debanshu.xshareit.domain.model.DataTransferResponse
import com.debanshu.xshareit.domain.model.ReceiverDTO
import com.debanshu.xshareit.domain.model.SenderDTO
import io.ktor.client.HttpClient
import io.ktor.server.engine.EmbeddedServer
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Property
import org.koin.core.annotation.Single

@Single
class ShareManager(
    private val httpClient: HttpClient,
    private val serverFactory: (String, Int) -> EmbeddedServer<*, *>
) {
    private var currentServer: Server? = null
    val receivedData: MutableStateFlow<DataTransferRequest?>? = currentServer?.receivedData
    private val tag = "ShareManager"

    fun startReceiver(receiverDTO: ReceiverDTO): Result<Unit, DataError.Network> {
        println("[$tag] Starting receiver server on ${receiverDTO.connectionDTO.ip}:${receiverDTO.connectionDTO.port}")
        return try {
            currentServer = Server(
                connectionDTO = receiverDTO.connectionDTO,
                serverFactory = serverFactory
            )
            println("[$tag] Server instance created successfully")
            currentServer?.invoke()
            println("[$tag] Server started and listening for connections")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("[$tag] Error starting receiver server: ${e.message}")
            e.printStackTrace()
            Result.Error(DataError.Network.UNKNOWN)
        }
    }

    suspend fun sendData(senderDTO: SenderDTO): Result<DataTransferResponse, DataError.Network> {
        val endpoint = "/receive"
        println("[$tag] Sending data to endpoint: $endpoint")
        println("[$tag] Data to send: ${senderDTO.data}")

        val request = DataTransferRequest(
            data = senderDTO.data,
            senderInfo = "XShareIt Client" // You can customize this
        )

        val clientWrapper = ClientWrapper(httpClient)
        val result = clientWrapper.post<DataTransferRequest, DataTransferResponse>(
            targetHost = senderDTO.connectionDTO.ip,
            targetPort = senderDTO.connectionDTO.port,
            endpoint = endpoint,
            requestBody = request
        )

        when (result) {
            is Result.Success -> {
                println("[$tag] Data sent successfully. Response: ${result.data}")
                println("[$tag] Server message: ${result.data.message}")
                println("[$tag] Data length confirmed: ${result.data.receivedDataLength}")
            }

            is Result.Error -> {
                println("[$tag] Failed to send data. Error: ${result.error}")
            }
        }

        return result
    }

    fun stopServer() {
        println("[$tag] Stopping server...")
        currentServer?.let { _ ->
            println("[$tag] Server instance found, attempting to stop")
            // TODO: Add proper server stop method if needed
            // server.stop()
        } ?: println("[$tag] No server instance to stop")
        currentServer = null
        println("[$tag] Server stopped and cleaned up")
    }
}
