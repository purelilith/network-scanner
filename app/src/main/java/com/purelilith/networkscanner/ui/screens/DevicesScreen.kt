package com.purelilith.networkscanner.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.purelilith.networkscanner.R
import com.purelilith.networkscanner.data.base.DeviceEntity
import com.purelilith.networkscanner.data.scanner.DeviceType
import com.purelilith.networkscanner.ui.NetworkScannerViewModel
import com.purelilith.networkscanner.ui.components.ConfirmDialog
import com.purelilith.networkscanner.ui.components.DetailOverlay
import com.purelilith.networkscanner.ui.components.DeviceRow
import com.purelilith.networkscanner.ui.components.EmptyState
import com.purelilith.networkscanner.ui.components.HeaderIconButton
import com.purelilith.networkscanner.ui.components.ScreenHeader
import com.purelilith.networkscanner.ui.components.rememberLastNonNull

@Composable
fun DevicesScreen(
    viewModel: NetworkScannerViewModel,
    onOpenBlacklist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val knownDevices by viewModel.knownDevices.collectAsState()

    var selectedKey by remember { mutableStateOf<String?>(null) }
    var deviceToIgnore by remember { mutableStateOf<DeviceEntity?>(null) }
    var deviceToDelete by remember { mutableStateOf<DeviceEntity?>(null) }
    var showDeleteAll by remember { mutableStateOf(false) }

    val selectedDevice = knownDevices.firstOrNull { it.deviceKey == selectedKey }
    val shownDevice = rememberLastNonNull(selectedDevice)

    BackHandler(enabled = selectedDevice != null) {
        selectedKey = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "Устройства",
                subtitle = if (knownDevices.isEmpty()) null else "Сохранено: ${knownDevices.size}",
                modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 12.dp),
                actions = {
                    HeaderIconButton(
                        iconRes = R.drawable.ic_act_block,
                        description = "Чёрный список",
                        onClick = onOpenBlacklist
                    )
                    HeaderIconButton(
                        iconRes = R.drawable.ic_act_delete_sweep,
                        description = "Удалить все устройства",
                        onClick = { showDeleteAll = true },
                        enabled = knownDevices.isNotEmpty()
                    )
                }
            )

            if (knownDevices.isEmpty()) {
                EmptyState(
                    iconRes = R.drawable.ic_dev_unknown,
                    title = "Пока пусто",
                    text = "Устройства появятся здесь после первого сканирования.",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(knownDevices, key = { it.deviceKey }) { device ->
                        val type = DeviceType.fromName(device.deviceType)
                        DeviceRow(
                            title = device.customName.ifBlank { device.title },
                            type = type,
                            isGuess = device.typeIsGuess && type != DeviceType.UNKNOWN,
                            trailingText = device.lastIp,
                            onClick = { selectedKey = device.deviceKey }
                        )
                    }
                }
            }
        }

        DetailOverlay(visible = selectedDevice != null) {
            if (shownDevice != null) {
                DeviceDetailsScreen(
                    device = shownDevice,
                    viewModel = viewModel,
                    onBack = { selectedKey = null },
                    onIgnore = { deviceToIgnore = shownDevice },
                    onDelete = { deviceToDelete = shownDevice }
                )
            }
        }
    }

    deviceToIgnore?.let { device ->
        ConfirmDialog(
            title = "Скрыть устройство?",
            text = "«${device.customName.ifBlank { device.title }}» не будет показываться при сканировании. Вернуть его можно в чёрном списке.",
            confirmText = "Скрыть",
            onConfirm = {
                viewModel.setDeviceIgnored(device.deviceKey, true)
                deviceToIgnore = null
                selectedKey = null
            },
            onDismiss = { deviceToIgnore = null }
        )
    }

    deviceToDelete?.let { device ->
        ConfirmDialog(
            title = "Удалить устройство?",
            text = "«${device.customName.ifBlank { device.title }}» будет удалено из памяти приложения. Оно может снова появиться при следующем сканировании.",
            confirmText = "Удалить",
            destructive = true,
            onConfirm = {
                viewModel.deleteDevice(device.deviceKey)
                deviceToDelete = null
                selectedKey = null
            },
            onDismiss = { deviceToDelete = null }
        )
    }

    if (showDeleteAll) {
        ConfirmDialog(
            title = "Удалить все устройства?",
            text = "Будут удалены все сохранённые устройства вместе с вашими названиями и типами. Журнал сканирований не изменится.",
            confirmText = "Удалить все",
            destructive = true,
            onConfirm = {
                viewModel.deleteAllDevices()
                showDeleteAll = false
            },
            onDismiss = { showDeleteAll = false }
        )
    }
}