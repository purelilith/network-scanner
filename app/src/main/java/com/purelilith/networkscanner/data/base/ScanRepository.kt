package com.purelilith.networkscanner.data.base

import androidx.room.withTransaction
import com.purelilith.networkscanner.data.scanner.DeviceInfo
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val db: AppDatabase) {

    private val dao = db.scanDao()

    fun observeDevices(): Flow<List<DeviceEntity>> = dao.observeDevices()

    fun observeIgnoredDevices(): Flow<List<DeviceEntity>> = dao.observeIgnoredDevices()

    fun observeScans(): Flow<List<ScanEntity>> = dao.observeScans()

    fun getSeenTimes(key: String): Flow<List<Long>> = dao.observeSeenTimes(key)

    internal fun deviceKey(device: DeviceInfo): String =
        device.hostname?.lowercase()?.let { "h:$it" } ?: "ip:${device.ip}"

    suspend fun updateDeviceType(deviceKey: String, newType: String) {
        dao.updateDeviceType(deviceKey, newType)
    }

    suspend fun updateCustomName(deviceKey: String, newName: String) {
        dao.updateCustomName(deviceKey, newName)
    }

    suspend fun getDevicesInScan(scanId: Long): List<DeviceEntity> =
        dao.getDevicesInScan(scanId)

    suspend fun setIgnoredStatus(deviceKey: String, ignored: Boolean) {
        dao.updateIgnoredStatus(deviceKey, ignored)
    }

    suspend fun getIgnoredKeys(): Set<String> =
        dao.getIgnoredDeviceKeys().toSet()

    suspend fun deleteScan(scanId: Long) {
        db.withTransaction {
            dao.deleteSightingsForScan(scanId)
            dao.deleteScan(scanId)
        }
    }

    suspend fun deleteAllScans() {
        db.withTransaction {
            dao.deleteAllSightings()
            dao.deleteAllScans()
        }
    }

    suspend fun deleteDevice(deviceKey: String) {
        dao.deleteDevice(deviceKey)
    }

    suspend fun deleteAllDevices() {
        dao.deleteAllDevices()
    }

    suspend fun saveScan(
        activeDevices: List<DeviceInfo>,
        startedAt: Long,
        finishedAt: Long
    ) {
        if (activeDevices.isEmpty()) return

        db.withTransaction {
            val byKey = activeDevices.associateBy { deviceKey(it) }

            val scanId = dao.insertScan(
                ScanEntity(
                    startedAt = startedAt,
                    deviceCount = byKey.size
                )
            )

            val existing = dao.getDevicesByKeys(byKey.keys.toList())
                .associateBy { it.deviceKey }

            val entities = byKey.map { (key, device) ->
                val old = existing[key]
                val keepOldType = old != null && !old.typeIsGuess

                DeviceEntity(
                    deviceKey = key,
                    lastIp = device.ip,
                    hostname = device.hostname,
                    title = device.name.title,
                    nameSource = device.name.source.name,
                    deviceType = if (keepOldType) old.deviceType else device.type.name,
                    typeIsGuess = if (keepOldType) false else device.typeIsGuess,
                    customName = old?.customName ?: "",
                    isIgnored = false,
                    firstSeen = old?.firstSeen ?: finishedAt,
                    lastSeen = finishedAt
                )
            }

            dao.upsertDevices(entities)
            dao.insertSightings(entities.map { SightingEntity(scanId, it.deviceKey) })
        }
    }
}