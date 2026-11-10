package com.purelilith.networkscanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.purelilith.networkscanner.data.scanner.DeviceType
import com.purelilith.networkscanner.ui.NetworkScannerViewModel
import com.purelilith.networkscanner.ui.components.AppButton
import com.purelilith.networkscanner.ui.components.DeviceRow
import com.purelilith.networkscanner.ui.components.devicesCountText
import com.purelilith.networkscanner.ui.formatLastScanTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: NetworkScannerViewModel,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.devices.collectAsState()
    val knownDevices by viewModel.knownDevices.collectAsState()
    val ignoredDevices by viewModel.ignoredDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val lastScanTime by viewModel.lastScanTime.collectAsState()

    val knownByIp = remember(knownDevices) { knownDevices.associateBy { it.lastIp } }
    val ignoredIps = remember(ignoredDevices) { ignoredDevices.mapNotNull { it.lastIp }.toSet() }
    val visibleDevices = remember(devices, ignoredIps) {
        devices.filter { it.ip !in ignoredIps }
    }

    var selectedKey by remember { mutableStateOf<String?>(null) }
    val selectedDevice = knownDevices.firstOrNull { it.deviceKey == selectedKey }

    PullToRefreshBox(
        isRefreshing = isScanning,
        onRefresh = { viewModel.startScan() },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ScannerHeader(
                    count = visibleDevices.size,
                    lastScanTime = lastScanTime,
                    isScanning = isScanning,
                    onScan = { viewModel.startScan() }
                )
            }

            if (lastScanTime != null && !isScanning && visibleDevices.isEmpty()) {
                item {
                    Text(
                        text = "Устройства не найдены. Проверьте, что телефон подключён к Wi-Fi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(visibleDevices, key = { it.ip }) { device ->
                val entity = knownByIp[device.ip]
                val title = entity?.customName?.takeIf { it.isNotBlank() } ?: device.name.title
                val type = if (entity != null) DeviceType.fromName(entity.deviceType) else device.type
                val typeIsGuess = if (entity != null) entity.typeIsGuess else device.typeIsGuess

                DeviceRow(
                    title = title,
                    type = type,
                    isGuess = typeIsGuess && type != DeviceType.UNKNOWN,
                    trailingText = device.ip,
                    onClick = if (entity != null) ({ selectedKey = entity.deviceKey }) else null
                )
            }
        }
    }

    selectedDevice?.let { device ->
        DeviceDetailsSheet(
            device = device,
            viewModel = viewModel,
            onDismiss = { selectedKey = null }
        )
    }
}

@Composable
private fun ScannerHeader(
    count: Int,
    lastScanTime: Long?,
    isScanning: Boolean,
    onScan: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        if (lastScanTime == null) {
            Text(
                text = "Кто в вашей сети",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Нажмите «Сканировать», чтобы увидеть устройства, подключённые к Wi-Fi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "${devicesCountText(count)} в сети",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = formatLastScanTime(lastScanTime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        AppButton(
            text = if (isScanning) "Сканирую…" else "Сканировать",
            onClick = onScan,
            icon = Icons.Rounded.Refresh,
            enabled = !isScanning,
            loading = isScanning,
            modifier = Modifier.fillMaxWidth()
        )
    }
}