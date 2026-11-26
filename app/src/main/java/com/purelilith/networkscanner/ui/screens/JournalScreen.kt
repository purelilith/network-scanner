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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.purelilith.networkscanner.data.base.ScanEntity
import com.purelilith.networkscanner.data.scanner.DeviceType
import com.purelilith.networkscanner.ui.NetworkScannerViewModel
import com.purelilith.networkscanner.ui.components.AppButton
import com.purelilith.networkscanner.ui.components.ButtonKind
import com.purelilith.networkscanner.ui.components.ConfirmDialog
import com.purelilith.networkscanner.ui.components.DetailOverlay
import com.purelilith.networkscanner.ui.components.DeviceRow
import com.purelilith.networkscanner.ui.components.EmptyState
import com.purelilith.networkscanner.ui.components.HeaderIconButton
import com.purelilith.networkscanner.ui.components.IconBadge
import com.purelilith.networkscanner.ui.components.ListCard
import com.purelilith.networkscanner.ui.components.ScreenHeader
import com.purelilith.networkscanner.ui.components.devicesCountText
import com.purelilith.networkscanner.ui.components.formatDateTime
import com.purelilith.networkscanner.ui.components.rememberLastNonNull

@Composable
fun JournalScreen(
    viewModel: NetworkScannerViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.scanHistory.collectAsState()
    val scanDevices by viewModel.selectedScanDevices.collectAsState()

    var selectedScan by remember { mutableStateOf<ScanEntity?>(null) }
    var scanToDelete by remember { mutableStateOf<ScanEntity?>(null) }
    var showDeleteAll by remember { mutableStateOf(false) }

    val shownScan = rememberLastNonNull(selectedScan)

    BackHandler(enabled = selectedScan != null) {
        selectedScan = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "Журнал",
                subtitle = if (history.isEmpty()) null else "Сканирований: ${history.size}",
                modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 12.dp),
                actions = {
                    HeaderIconButton(
                        iconRes = R.drawable.ic_act_delete_sweep,
                        description = "Очистить журнал",
                        onClick = { showDeleteAll = true },
                        enabled = history.isNotEmpty()
                    )
                }
            )

            if (history.isEmpty()) {
                EmptyState(
                    iconRes = R.drawable.ic_tab_journal,
                    title = "Журнал пуст",
                    text = "Здесь появится запись о каждом сканировании сети.",
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
                    items(history, key = { it.id ?: it.startedAt }) { scan ->
                        ListCard(
                            onClick = {
                                selectedScan = scan
                                scan.id?.let { viewModel.loadDevicesForScan(it) }
                            }
                        ) {
                            IconBadge(iconRes = R.drawable.ic_tab_journal)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = formatDateTime(scan.startedAt),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = devicesCountText(scan.deviceCount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        DetailOverlay(visible = selectedScan != null) {
            if (shownScan != null) {
                ScanDetailsScreen(
                    scan = shownScan,
                    devices = scanDevices,
                    onBack = { selectedScan = null },
                    onDelete = { scanToDelete = shownScan }
                )
            }
        }
    }

    scanToDelete?.let { scan ->
        ConfirmDialog(
            title = "Удалить сканирование?",
            text = "Запись от ${formatDateTime(scan.startedAt)} будет удалена. Сами устройства останутся в списке.",
            confirmText = "Удалить",
            destructive = true,
            onConfirm = {
                scan.id?.let { viewModel.deleteScan(it) }
                scanToDelete = null
                selectedScan = null
            },
            onDismiss = { scanToDelete = null }
        )
    }

    if (showDeleteAll) {
        ConfirmDialog(
            title = "Очистить журнал?",
            text = "Все записи о сканированиях будут удалены. Сами устройства останутся в списке.",
            confirmText = "Очистить",
            destructive = true,
            onConfirm = {
                viewModel.deleteAllScans()
                showDeleteAll = false
            },
            onDismiss = { showDeleteAll = false }
        )
    }
}

@Composable
private fun ScanDetailsScreen(
    scan: ScanEntity,
    devices: List<DeviceEntity>,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = formatDateTime(scan.startedAt),
            subtitle = devicesCountText(scan.deviceCount),
            modifier = Modifier.padding(start = 8.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
            navigationIcon = {
                HeaderIconButton(
                    iconRes = R.drawable.ic_act_back,
                    description = "Назад",
                    onClick = onBack
                )
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (devices.isEmpty()) {
                item {
                    Text(
                        text = "В этом сканировании нет сохранённых устройств.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(devices, key = { it.deviceKey }) { device ->
                DeviceRow(
                    title = device.customName.ifBlank { device.title },
                    type = DeviceType.fromName(device.deviceType),
                    trailingText = device.lastIp
                )
            }

            item {
                AppButton(
                    text = "Удалить сканирование",
                    onClick = onDelete,
                    kind = ButtonKind.Danger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}