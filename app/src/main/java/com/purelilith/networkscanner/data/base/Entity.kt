package com.purelilith.networkscanner.data.base

import androidx.compose.ui.tooling.preview.Preview
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val deviceKey: String,
    val lastIP: String,
    val hostname: String?,
    val title: String,
    val nameSource: String,
    val firstSeen: Long,
    val lastSeen: Long
)

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey (autoGenerate = true) val id: Long?
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