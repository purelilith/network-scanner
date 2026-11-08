package com.purelilith.networkscanner.data.base

import androidx.room.withTransaction
import com.purelilith.networkscanner.data.scanner.DeviceInfo
import com.purelilith.networkscanner.data.scanner.DeviceType
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val db: AppDatabase) {

    private val dao = db.scanDao()

    fun observeDevices(): Flow<List<DeviceEntity>> = dao.observeDevices()

    fun getSeenTimes(key: String): Flow<List<Long>> {
        return dao.observeSeenTimes(key)
    }

    suspend fun updateDeviceType(deviceKey: String, newType: String) {
        db.withTransaction {
            dao.updateDeviceType(deviceKey, newType)
        }
    }

    fun observeIgnoredDevices(): Flow<List<DeviceEntity>> = dao.observeIgnoredDevices()

    fun observeScans(): Flow<List<ScanEntity>> = dao.observeScans()

    internal fun deviceKey(device: DeviceInfo): String =
        device.hostname?.lowercase()?.let { "h:$it" } ?: "ip:${device.ip}"

    suspend fun updateCustomName(deviceKey: String, newName: String) {
        db.withTransaction {
            dao.updateCustomName(deviceKey, newName)
        }
    }

    suspend fun getDevicesInScan(scanId: Long): List<DeviceEntity> {
        return dao.getDevicesInScan(scanId)
    }

    suspend fun setIgnoredStatus(deviceKey: String, ignored: Boolean) {
        db.withTransaction {
            dao.updateIgnoredStatus(deviceKey, ignored)
        }
    }

    suspend fun getIgnoredKeys(): Set<String> {
        return dao.getIgnoredDeviceKeys().toSet()
    }

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
        db.withTransaction {
            dao.deleteDevice(deviceKey)
        }
    }

    suspend fun deleteAllDevices() {
        db.withTransaction {
            dao.deleteAllDevices()
        }
    }

    suspend fun saveScan(devices: List<DeviceInfo>, startedAt: Long, finishedAt: Long) {
        db.withTransaction {
            val ignoredKeys = dao.getIgnoredDeviceKeys().toSet()

            val activeDevices = devices.filter { device ->
                ignoredKeys.contains(deviceKey(device)) == false
            }

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

                val finalType = if (old != null && !old.typeIsGuess) {
                    old.deviceType
                } else {
                    device.type.name
                }

                val finalTypeIsGuess = if (old != null && !old.typeIsGuess) {
                    false
                } else {
                    device.typeIsGuess
                }

                DeviceEntity(
                    deviceKey = key,
                    lastIp = device.ip,
                    hostname = device.hostname,
                    title = device.name.title,
                    nameSource = device.name.source.name,
                    deviceType = finalType,
                    typeIsGuess = finalTypeIsGuess,
                    customName = old?.customName ?: "",
                    isIgnored = false,
                    firstSeen = old?.firstSeen ?: finishedAt,
                    lastSeen = finishedAt
                )
            }

            if (entities.isNotEmpty()) {
                dao.upsertDevices(entities)
                dao.insertSightings(entities.map { SightingEntity(scanId, it.deviceKey) })
            }
        }
    }
}