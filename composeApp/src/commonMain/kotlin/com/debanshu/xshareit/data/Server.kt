package com.debanshu.xshareit.data

import com.debanshu.xshareit.domain.model.ConnectionDTO
import com.debanshu.xshareit.domain.model.DataTransferRequest
import com.debanshu.xshareit.domain.model.DataTransferResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.MutableStateFlow

class Server(
    val connectionDTO: ConnectionDTO,
    private val serverFactory: (String, Int) -> EmbeddedServer<*, *>,
) {
    private val tag = "Server"

    val receivedData = MutableStateFlow<DataTransferRequest?>(null)
    
    init {
        println("[$tag] Server initialized for ${connectionDTO.ip}:${connectionDTO.port}")
    }
    fun invoke() {
        println("[$tag] Creating embedded server for ${connectionDTO.ip}:${connectionDTO.port}")
        val server = serverFactory(connectionDTO.ip, connectionDTO.port)
        
        println("[$tag] Setting up routing endpoints")
        server.application.routing {
            post("/receive") {
                println("[$tag] Received POST request to /receive endpoint")
                
                try {
                    val request = call.receive<DataTransferRequest>()
                    println("[$tag] Received data: ${request.data}")
                    println("[$tag] Request timestamp: ${request.timestamp}")
                    println("[$tag] Sender info: ${request.senderInfo}")
                    
                    receivedData.value = request
                    
                    val response = DataTransferResponse(
                        success = true,
                        message = "Data received successfully",
                        receivedDataLength = request.data.length
                    )
                    
                    call.respond(HttpStatusCode.OK, response)
                    println("[$tag] Responded with success status and data length: ${request.data.length}")
                } catch (e: Exception) {
                    println("[$tag] Error processing request: ${e.message}")
                    e.printStackTrace()
                    
                    val errorResponse = DataTransferResponse(
                        success = false,
                        message = "Failed to process request: ${e.message}"
                    )
                    
                    call.respond(HttpStatusCode.BadRequest, errorResponse)
                }
            }
        }
        
        println("[$tag] Starting server on ${connectionDTO.ip}:${connectionDTO.port}")
        server.start(wait = false)
        println("[$tag] Server started successfully and listening for connections")
    }
}