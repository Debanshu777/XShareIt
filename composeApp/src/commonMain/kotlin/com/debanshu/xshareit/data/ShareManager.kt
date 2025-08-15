package com.debanshu.xshareit.data

import com.debanshu.xshareit.domain.model.ConnectionType

class ShareManager(
    private val type: ConnectionType
) {
    fun invoke() =
        when (type) {
            is ConnectionType.Receiver -> {
                Server()
            }

            is ConnectionType.Sender -> {
                Client()
            }
        }
}