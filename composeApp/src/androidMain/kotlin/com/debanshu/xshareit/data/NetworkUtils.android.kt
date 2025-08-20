package com.debanshu.xshareit.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import org.koin.mp.KoinPlatform
import java.net.NetworkInterface
import java.util.*

actual fun getDeviceIpAddress(): String? {
    return try {
        // First try to get IP from network interfaces (works for both WiFi and mobile data)
        getIpFromNetworkInterface() ?: getIpFromWifiManager()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getIpFromNetworkInterface(): String? {
    try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (networkInterface in interfaces) {
            if (networkInterface.isLoopback || !networkInterface.isUp) continue

            val addresses = Collections.list(networkInterface.inetAddresses)
            for (address in addresses) {
                if (!address.isLoopbackAddress && address.isSiteLocalAddress) {
                    val ip = address.hostAddress
                    // Check if it's IPv4
                    if (ip?.contains(':') == false) {
                        return ip
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

private fun getIpFromWifiManager(): String? {
    return try {
        val context = KoinPlatform.getKoin().get<Context>()
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ip = wifiInfo.ipAddress

        // Convert IP address from int to string format
        String.format(
            Locale.getDefault(),
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

