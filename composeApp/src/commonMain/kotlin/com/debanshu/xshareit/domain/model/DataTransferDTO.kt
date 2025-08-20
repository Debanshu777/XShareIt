package com.debanshu.xshareit.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
@OptIn(ExperimentalTime::class)
data class DataTransferRequest(
    val data: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val senderInfo: String? = null
)

@Serializable
@OptIn(ExperimentalTime::class)
data class DataTransferResponse(
    val success: Boolean,
    val message: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val receivedDataLength: Int = 0
)
