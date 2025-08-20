package com.debanshu.xshareit.data

import com.debanshu.xshareit.data.error.DataError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ClientWrapper(val networkClient: HttpClient) {
    val tag = "ClientWrapper"
    suspend inline fun <reified T> get(
        targetHost: String,
        targetPort: Int,
        endpoint: String,
        queries: Map<String, String>?,
    ): Result<T, DataError.Network> {
        println("[$tag] Making GET request to: $endpoint")
        println("[$tag] Query parameters: $queries")

        val response = try {
            networkClient.get("http://$targetHost:$targetPort$endpoint") {
                if (queries != null) {
                    for ((key, value) in queries) {
                        parameter(key, value)
                    }
                }
            }
        } catch (e: UnresolvedAddressException) {
            println("[$tag] Network error - no internet connection: ${e.message}")
            return Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: SerializationException) {
            println("[$tag] Serialization error: ${e.message}")
            return Result.Error(DataError.Network.SERIALIZATION)
        } catch (e: Exception) {
            println("[$tag] Unknown error: ${e.message}")
            e.printStackTrace()
            return Result.Error(DataError.Network.UNKNOWN)
        }
        println("[$tag] Response status: ${response.status.value}")
        return when (response.status.value) {
            in 200..299 -> {
                println("[$tag] Success response received")
                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                val data = json.decodeFromString<T>(response.body())
                println("[$tag] Response data decoded successfully")
                Result.Success(data)
            }

            401 -> {
                println("[$tag] Unauthorized error (401)")
                Result.Error(DataError.Network.UNAUTHORIZED)
            }

            409 -> {
                println("[$tag] Conflict error (409)")
                Result.Error(DataError.Network.CONFLICT)
            }

            408 -> {
                println("[$tag] Request timeout error (408)")
                Result.Error(DataError.Network.REQUEST_TIMEOUT)
            }

            413 -> {
                println("[$tag] Payload too large error (413)")
                Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
            }

            in 500..599 -> {
                println("[$tag] Server error (${response.status.value})")
                Result.Error(DataError.Network.SERVER_ERROR)
            }

            else -> {
                println("[$tag] Unknown error (${response.status.value})")
                Result.Error(DataError.Network.UNKNOWN)
            }
        }
    }

    suspend inline fun <reified TRequest : Any, reified TResponse : Any> post(
        targetHost: String,
        targetPort: Int,
        endpoint: String,
        requestBody: TRequest
    ): Result<TResponse, DataError.Network> {
        println("[$tag] Making POST JSON request to: $endpoint")
        println("[$tag] Request body: $requestBody")

        val response = try {
            networkClient.post("http://$targetHost:$targetPort$endpoint") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        } catch (e: UnresolvedAddressException) {
            println("[$tag] Network error - no internet connection: ${e.message}")
            return Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: SerializationException) {
            println("[$tag] Serialization error: ${e.message}")
            return Result.Error(DataError.Network.SERIALIZATION)
        } catch (e: Exception) {
            println("[$tag] Unknown error: ${e.message}")
            e.printStackTrace()
            return Result.Error(DataError.Network.UNKNOWN)
        }
        
        println("[$tag] Response status: ${response.status.value}")
        return when (response.status.value) {
            in 200..299 -> {
                println("[$tag] Success response received")
                try {
                    val data = response.body<TResponse>()
                    println("[$tag] Response data decoded successfully: $data")
                    Result.Success(data)
                } catch (e: SerializationException) {
                    println("[$tag] Failed to deserialize response: ${e.message}")
                    Result.Error(DataError.Network.SERIALIZATION)
                }
            }

            401 -> {
                println("[$tag] Unauthorized error (401)")
                Result.Error(DataError.Network.UNAUTHORIZED)
            }
            409 -> {
                println("[$tag] Conflict error (409)")
                Result.Error(DataError.Network.CONFLICT)
            }
            408 -> {
                println("[$tag] Request timeout error (408)")
                Result.Error(DataError.Network.REQUEST_TIMEOUT)
            }
            413 -> {
                println("[$tag] Payload too large error (413)")
                Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
            }
            in 500..599 -> {
                println("[$tag] Server error (${response.status.value})")
                Result.Error(DataError.Network.SERVER_ERROR)
            }
            else -> {
                println("[$tag] Unknown error (${response.status.value})")
                Result.Error(DataError.Network.UNKNOWN)
            }
        }
    }
}