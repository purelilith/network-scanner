package com.purelilith.networkscanner.data.scanner

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object NetworkScanner {

    private val PORTS_TO_CHECK: List<Int> = listOf(80, 22, 8080, 9100, 8009)
    private const val PING_CONCURRENCY = 64
    private const val CHECK_CONCURRENCY = 16

    private data class ResolutionCacheKey(
        val hostname: String?,
        val mdnsName: String?,
        val services: List<String>,
        val openPorts: List<Int>
    )

    private data class ResolvedDeviceMeta(
        val name: FriendlyDeviceName,
        val typeResult: TypeResult
    )

    private suspend fun isPortOpen(ip: String, port: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 200)
                }
                true
            } catch (_: Exception) {
                false
            }
        }

    private fun resolveMetaCached(
        cache: MutableMap<ResolutionCacheKey, ResolvedDeviceMeta>,
        hostname: String?,
        mdnsName: String?,
        services: List<String>,
        openPorts: List<Int>,
        portGuess: String?
    ): ResolvedDeviceMeta {
        val key = ResolutionCacheKey(
            hostname = hostname,
            mdnsName = mdnsName,
            services = services,
            openPorts = openPorts
        )
        return cache.getOrPut(key) {
            val name = FriendlyName.resolve(
                hostname = hostname,
                mdnsName = mdnsName,
                services = services,
                portGuess = portGuess
            )
            val typeResult = FriendlyName.resolveType(hostname, services, openPorts)
            ResolvedDeviceMeta(name = name, typeResult = typeResult)
        }
    }

    private suspend fun checkDevice(
        ip: String,
        mdnsFound: List<MdnsDiscovery.Found>,
        checkSemaphore: Semaphore,
        metaCache: MutableMap<ResolutionCacheKey, ResolvedDeviceMeta>
    ): DeviceInfo = checkSemaphore.withPermit {
        coroutineScope {
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

            val services = mdnsFound.map { it.serviceType }.distinct()
            val mdnsName = MdnsDiscovery.displayName(mdnsFound)

            val meta = synchronized(metaCache) {
                resolveMetaCached(
                    cache = metaCache,
                    hostname = hostname,
                    mdnsName = mdnsName,
                    services = services,
                    openPorts = openPorts,
                    portGuess = portGuess
                )
            }

            DeviceInfo(
                ip = ip,
                openPorts = openPorts,
                hostname = hostname,
                name = meta.name,
                type = meta.typeResult.type,
                typeIsGuess = meta.typeResult.isGuess,
                mdnsServices = services
            )
        }
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

        val pingSemaphore = Semaphore(PING_CONCURRENCY)
        val aliveJobs = (1..254).map { lastOctet ->
            async {
                pingSemaphore.withPermit {
                    val host = "$baseIp.$lastOctet"
                    try {
                        val address = InetAddress.getByName(host)
                        if (address.isReachable(300)) host else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }

        val aliveHosts = aliveJobs.awaitAll().filterNotNull()
        val mdnsByIp = mdnsDeferred.await().groupBy { it.ip }
        val allHosts = (aliveHosts + mdnsByIp.keys).distinct()

        val checkSemaphore = Semaphore(CHECK_CONCURRENCY)
        val metaCache = HashMap<ResolutionCacheKey, ResolvedDeviceMeta>(allHosts.size.coerceAtLeast(16))

        allHosts.map { ip ->
            async { checkDevice(ip, mdnsByIp[ip].orEmpty(), checkSemaphore, metaCache) }
        }.awaitAll()
    }

    suspend fun getHostName(ip: String): String? = withContext(Dispatchers.IO) {
        try {
            val hostname = InetAddress.getByName(ip).canonicalHostName
            if (hostname == ip) null else hostname
        } catch (_: Exception) {
            null
        }
    }
}