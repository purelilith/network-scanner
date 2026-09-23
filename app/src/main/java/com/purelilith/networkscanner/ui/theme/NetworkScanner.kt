package com.purelilith.networkscanner.ui.theme

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import java.net.InetAddress

object NetworkScanner {

    private const val TAG = "NetworkScanner"

    suspend fun scan(context: Context): List<String> = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val dhcpInfo = wifiManager.dhcpInfo
        val ipInt = dhcpInfo.ipAddress

        val baseIp = String.format(
            "%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff
        )

        val jobs = (1..254).map { lastOctet ->
            async {
                val host = "$baseIp.$lastOctet"
                try {
                    val address = InetAddress.getByName(host)
                    if (address.isReachable(300)) {
                        Log.d(TAG, "Device found: $host")
                        host
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
        }

        jobs.awaitAll().filterNotNull()
    }
}