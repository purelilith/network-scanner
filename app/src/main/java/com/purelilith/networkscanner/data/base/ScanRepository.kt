package com.purelilith.networkscanner.data.base

import androidx.room.withTransaction
import com.purelilith.networkscanner.data.scanner.DeviceInfo


class ScanRepository(private val db: AppDatabase) {

    private val dao = db.scanDao()

    fun observeDevices() = dao.observeDevices()

    fun observeScans() = dao.observeScans()

    internal fun deviceKey(device: DeviceInfo): String =
        device.hostname?.lowercase()?.let { "h:$it" } ?: "ip:${device.ip}"

    suspend fun updateCustomName(deviceKey: String, newName: String) {
        db.withTransaction {
            dao.updateCustomName(deviceKey, newName)
        }
    }

    suspend fun saveScan(devices: List<DeviceInfo>, startedAt: Long, finishedAt: Long) {
        db.withTransaction {
            val byKey = devices.associateBy { deviceKey(it) }

            val scanId = dao.insertScan(
                ScanEntity(
                    startedAt = startedAt,
                    durationMs = finishedAt - startedAt,
                    deviceCount = byKey.size
                )
            )

            val existing = dao.getDevicesByKeys(byKey.keys.toList())
                .associateBy { it.deviceKey }

            val entities = byKey.map { (key, device) ->
                DeviceEntity(
                    deviceKey = key,
                    lastIp = device.ip,
                    hostname = device.hostname,
                    title = device.name.title,
                    nameSource = device.name.source.name,
                    customName = existing[key]?.customName ?: "",
                    firstSeen = existing[key]?.firstSeen ?: finishedAt,
                    lastSeen = finishedAt
                )
            }

            dao.upsertDevices(entities)
            dao.insertSightings(entities.map { SightingEntity(scanId, it.deviceKey) })
        }
    }
}