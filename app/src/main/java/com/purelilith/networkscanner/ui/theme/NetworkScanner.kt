package com.purelilith.networkscanner.ui.theme

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.Socket

object NetworkScanner {

    private const val TAG = "NetworkScanner"
    private val PORTS_TO_CHECK: List<Int> = listOf(0, 22, 8080, 9100, 8009)

    private suspend fun isPortOpen(ip: String, port: Int): Boolean = withContext(Dispatchers.IO){
        try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, port), 200)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkDevicePortByIP(ip: String): DeviceInfo = coroutineScope {
        val hostnameJob = async { getHostName(ip) }
        val portJobs = PORTS_TO_CHECK.map { port ->
            async { if (isPortOpen(ip, port)) port else null }
        }
        val hostname = hostnameJob.await()
        val openPorts = portJobs.awaitAll().filterNotNull()
        val guessedType = when {
            9100 in openPorts -> "Возможно, принтер"
            8009 in openPorts -> "Возможно, Chromecast"
            80 in openPorts || 8080 in openPorts -> "Есть веб-интерфейс (возможно, роутер)"
            22 in openPorts -> "Есть SSH (возможно, сервер/Linux-устройство)"
            else -> "Тип неизвестен"
        }

        DeviceInfo(ip = ip, openPorts = openPorts, guessedType = guessedType, hostname = hostname)
    }

    suspend fun scan(context: Context): List<DeviceInfo> = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val dhcpInfo = wifiManager.dhcpInfo
        val ipInt = dhcpInfo.ipAddress
        if (ipInt == 0) {
            return@withContext emptyList()
        }

        val baseIp = String.format(
            "%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff
        )

        val aliveJobs = (1..254).map { lastOctet ->
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

        val aliveHosts = aliveJobs.awaitAll().filterNotNull()
        val deviceJobs = aliveHosts.map {ip ->
            async { checkDevicePortByIP(ip)}
        }
        deviceJobs.awaitAll()


    }

    suspend fun getHostName(ip: String): String? = withContext(Dispatchers.IO) {
        try {
            val hostname = InetAddress.getByName(ip).canonicalHostName
            if (hostname == ip) null else hostname
        } catch (e: Exception) {
            null
        }
    }
}

