package com.debanshu.xshareit.domain.model

sealed class ConnectionType {
    data object Sender : ConnectionType()
    data object Receiver : ConnectionType()
}