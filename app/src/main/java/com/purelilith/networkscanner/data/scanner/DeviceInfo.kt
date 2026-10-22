package com.purelilith.networkscanner.data.scanner

data class DeviceInfo(
    val ip: String,
    val openPorts: List<Int>,
    val hostname: String?,
    val name: FriendlyDeviceName,
    val mdnsServices: List<String> = emptyList()
)
