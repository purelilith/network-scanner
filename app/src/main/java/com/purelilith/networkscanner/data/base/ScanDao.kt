package com.purelilith.networkscanner.data.base

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Insert
    suspend fun insertScan(scan: ScanEntity): Long

    @Query("SELECT * FROM devices WHERE deviceKey IN (:keys)")
    suspend fun getDevicesByKeys(keys: List<String>): List<DeviceEntity>

    @Upsert
    suspend fun upsertDevices(devices: List<DeviceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSightings(sightings: List<SightingEntity>)

    @Query("SELECT * FROM devices ORDER BY lastSeen DESC")
    fun observeDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM scans ORDER BY startedAt DESC")
    fun observeScans(): Flow<List<ScanEntity>>

    @Query("SELECT d.* FROM devices d JOIN sightings s ON s.deviceKey = d.deviceKey WHERE s.scanId = :scanId")
    suspend fun getDevicesInScan(scanId: Long): List<DeviceEntity>

    @Query("SELECT sc.startedAt FROM scans sc JOIN sightings s ON s.scanId = sc.id WHERE s.deviceKey = :key ORDER BY sc.startedAt DESC")
    fun observeSeenTimes(key: String): Flow<List<Long>>
}