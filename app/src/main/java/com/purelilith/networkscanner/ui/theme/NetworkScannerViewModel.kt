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

    fun startScan(context: Context) {
        viewModelScope.launch{
            _isScanning.value = true
            _devices.value = NetworkScanner.scan(context)
            _isScanning.value = false
        }

    }

}