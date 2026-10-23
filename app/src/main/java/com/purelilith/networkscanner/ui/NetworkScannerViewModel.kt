package com.purelilith.networkscanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.purelilith.networkscanner.data.base.AppDatabase
import com.purelilith.networkscanner.data.base.DeviceEntity
import com.purelilith.networkscanner.data.base.ScanEntity
import com.purelilith.networkscanner.data.base.ScanRepository
import com.purelilith.networkscanner.data.scanner.DeviceInfo
import com.purelilith.networkscanner.data.scanner.NetworkScanner

class NetworkScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScanRepository(AppDatabase.get(application))

    private val _devices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val devices: StateFlow<List<DeviceInfo>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastScanTime = MutableStateFlow<Long?>(null)
    val lastScanTime: StateFlow<Long?> = _lastScanTime.asStateFlow()

    val knownDevices: StateFlow<List<DeviceEntity>> = repository.observeDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanHistory: StateFlow<List<ScanEntity>> = repository.observeScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startScan() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val startedAt = System.currentTimeMillis()
                val found = NetworkScanner.scan(getApplication<Application>())
                val finishedAt = System.currentTimeMillis()
                _devices.value = found
                if (found.isNotEmpty()) {
                    repository.saveScan(found, startedAt, finishedAt)
                }
                _lastScanTime.value = finishedAt
            } finally {
                _isScanning.value = false
            }
        }
    }
}

fun formatLastScanTime(lastTime: Long?): String {
    if (lastTime == null) return "Сеть еще не сканировали"

    val diffMinutes = (System.currentTimeMillis() - lastTime) / 60000
    return when {
        diffMinutes < 1 -> "Только что"
        diffMinutes == 1L -> "1 минуту назад"
        else -> "$diffMinutes минут назад"
    }
}