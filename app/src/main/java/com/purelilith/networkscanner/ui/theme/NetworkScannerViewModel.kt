package com.purelilith.networkscanner.ui.theme

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkScannerViewModel: ViewModel() {

    private val _devices = MutableStateFlow<List<String>>(emptyList())
    val devices: StateFlow<List<String>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow<Boolean>(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastScanTime = MutableStateFlow<Long?>(null)
    val lastScanTime: StateFlow<Long?> = _lastScanTime.asStateFlow()

    fun startScan(context: Context) {
        viewModelScope.launch{
            _isScanning.value = true
            _devices.value = NetworkScanner.scan(context)
            _lastScanTime.value = System.currentTimeMillis()
            _isScanning.value = false
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