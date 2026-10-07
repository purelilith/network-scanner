package com.purelilith.networkscanner.ui.theme

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.Inet4Address
import java.util.Collections
import kotlin.coroutines.resume

object MdnsDiscovery {

    private const val TAG = "MdnsDiscovery"

    private val SERVICE_TYPES = listOf(
        "_googlecast._tcp", "_airplay._tcp", "_ipp._tcp", "_printer._tcp",
        "_hap._tcp", "_spotify-connect._tcp", "_smb._tcp", "_http._tcp"
    )

    private val NAME_KEYS = listOf("fn", "ty", "model", "md")

    data class Found(
        val ip: String,
        val name: String,
        val serviceType: String,
        val attrs: Map<String, String>
    )


    suspend fun discover(context: Context, timeoutMs: Long = 4000): List<Found> = coroutineScope {

        val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        val results = Collections.synchronizedList(mutableListOf<Found>())
        val resolveMutex = Mutex()

        val listeners = SERVICE_TYPES.map { type ->
            val listener = object : NsdManager.DiscoveryListener {

                override fun onServiceFound(info: NsdServiceInfo) {
                    launch {
                        resolveMutex.withLock {
                            val resolved = resolve(nsd, info) ?: return@withLock
                            val ip =
                                (resolved.host as? Inet4Address)?.hostAddress ?: return@withLock
                            val attrs = resolved.attributes
                                .mapValues { (_, bytes) ->
                                    bytes?.toString(Charsets.UTF_8).orEmpty()
                                }
                            Log.d(
                                TAG,
                                "found ip=$ip type=$type name=${resolved.serviceName} attrs=$attrs"
                            )
                            results += Found(ip, resolved.serviceName, type, attrs)
                        }
                    }
                }

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.d(TAG, "start failed: $serviceType code=$errorCode")
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onDiscoveryStarted(serviceType: String) {
                    Log.d(TAG, "started: $serviceType")
                }
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onServiceLost(info: NsdServiceInfo) {}
            }
            nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            listener
        }

        delay(timeoutMs)
        listeners.forEach { runCatching { nsd.stopServiceDiscovery(it) } }
        results.toList()
    }


    fun displayName(found: List<Found>): String? =
        NAME_KEYS.firstNotNullOfOrNull { key ->
            found.firstNotNullOfOrNull { it.attrs[key]?.takeIf(String::isNotBlank) }
        }

    suspend fun resolve(nsd: NsdManager, info: NsdServiceInfo): NsdServiceInfo? = withTimeoutOrNull(1500) {
        suspendCancellableCoroutine<NsdServiceInfo?> { cont ->
            nsd.resolveService(info, object : NsdManager.ResolveListener {
                override fun onResolveFailed(si: NsdServiceInfo, errorCode: Int) {
                    if (cont.isActive) cont.resume(null)
                }

                override fun onServiceResolved(si: NsdServiceInfo) {
                    if (cont.isActive) cont.resume(si)
                }
            })
        }
    }
}

