package com.purelilith.networkscanner.data.base

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val deviceKey: String,
    val lastIp: String?,
    val hostname: String?,
    val title: String,
    val nameSource: String,
    val customName: String = "",
    val isIgnored: Boolean = false,
    val firstSeen: Long,
    val lastSeen: Long
)

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey (autoGenerate = true) val id: Long? = null,
    val startedAt: Long,
    val durationMs: Long,
    val deviceCount: Int,
)

@Entity(
    tableName = "sightings",
    primaryKeys = ["scanId", "deviceKey"],
    indices =[Index("deviceKey")]
)
data class SightingEntity(
    val scanId: Long,
    val deviceKey: String,
)