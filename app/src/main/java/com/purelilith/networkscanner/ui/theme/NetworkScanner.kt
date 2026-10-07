package com.purelilith.networkscanner.ui.theme

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object NetworkScanner {

    private const val TAG = "NetworkScanner"
    private val PORTS_TO_CHECK: List<Int> = listOf(80, 22, 8080, 9100, 8009)

    private suspend fun isPortOpen(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 200)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkDevice(
        ip: String,
        mdnsFound: List<MdnsDiscovery.Found>
    ): DeviceInfo = coroutineScope {
        val hostnameJob = async { getHostName(ip) }
        val portJobs = PORTS_TO_CHECK.map { port ->
            async { if (isPortOpen(ip, port)) port else null }
        }
        val hostname = hostnameJob.await()
        val openPorts = portJobs.awaitAll().filterNotNull()

        val portGuess = when {
            9100 in openPorts -> "Возможно, принтер"
            8009 in openPorts -> "Возможно, Chromecast"
            80 in openPorts || 8080 in openPorts -> "Есть веб-интерфейс (возможно, роутер)"
            22 in openPorts -> "Есть SSH (возможно, сервер/Linux-устройство)"
            else -> null
        }

        val services = mdnsFound.map { it.serviceType }

        val name = FriendlyName.resolve(
            hostname = hostname,
            mdnsName = MdnsDiscovery.displayName(mdnsFound),
            services = services,
            portGuess = portGuess
        )

        DeviceInfo(
            ip = ip,
            openPorts = openPorts,
            hostname = hostname,
            name = name,
            mdnsServices = services
        )
    }

    suspend fun scan(context: Context): List<DeviceInfo> = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val ipInt = wifiManager.dhcpInfo.ipAddress
        if (ipInt == 0) {
            return@withContext emptyList()
        }

        val baseIp = String.format(
            "%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff
        )

        val mdnsDeferred = async {
            runCatching { MdnsDiscovery.discover(context) }.getOrDefault(emptyList())
        }

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
        val mdnsByIp = mdnsDeferred.await().groupBy { it.ip }
        val allHosts = (aliveHosts + mdnsByIp.keys).distinct()

        allHosts.map { ip ->
            async { checkDevice(ip, mdnsByIp[ip].orEmpty()) }
        }.awaitAll()
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