package com.purelilith.networkscanner.ui.theme

data class DeviceInfo(
    val ip: String,
    val openPorts: List<Int>,
    val guessedType: String
)
