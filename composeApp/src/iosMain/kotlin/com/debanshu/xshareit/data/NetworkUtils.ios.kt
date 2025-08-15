package com.debanshu.xshareit.data

import kotlinx.cinterop.*
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.posix.*

actual fun getDeviceIpAddress(): String? {
    return try {
        getIPAddress()
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun getIPAddress(): String? {
    memScoped {
        val ifaddr = alloc<CPointerVar<ifaddrs>>()
        
        if (getifaddrs(ifaddr.ptr) != 0) {
            return null
        }
        
        var ptr = ifaddr.value
        var address: String? = null
        
        while (ptr != null) {
            val networkInterface = ptr.pointed
            val addrFamily = networkInterface.ifa_addr?.pointed?.sa_family?.toUByte()
            
            if (addrFamily == AF_INET.toUByte() || addrFamily == AF_INET6.toUByte()) {
                val name = networkInterface.ifa_name?.toKString() ?: ""
                
                // Check for WiFi, Ethernet, and Cellular interfaces
                // wifi = ["en0"]
                // wired = ["en2", "en3", "en4"]  
                // cellular = ["pdp_ip0","pdp_ip1","pdp_ip2","pdp_ip3"]
                if (name == "en0" || name == "en2" || name == "en3" || name == "en4" || 
                    name == "pdp_ip0" || name == "pdp_ip1" || name == "pdp_ip2" || name == "pdp_ip3") {
                    
                    val hostname = ByteArray(NI_MAXHOST.toInt())
                    
                    hostname.usePinned { pinnedHostname ->
                        val result = getnameinfo(
                            networkInterface.ifa_addr,
                            (networkInterface.ifa_addr?.pointed?.sa_len?.toUInt() ?: 0u),
                            pinnedHostname.addressOf(0),
                            hostname.size.convert(),
                            null,
                            0u,
                            NI_NUMERICHOST
                        )
                        
                        if (result == 0) {
                            address = pinnedHostname.get().toKString()
                            
                            // Prefer IPv4 addresses, but if we find one, we can break
                            if (addrFamily == AF_INET.toUByte()) {
                                freeifaddrs(ifaddr.value)
                                return address
                            }
                        }
                    }
                }
            }
            
            ptr = networkInterface.ifa_next
        }
        
        freeifaddrs(ifaddr.value)
        return address
    }
}
