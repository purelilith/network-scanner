package com.purelilith.networkscanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.purelilith.networkscanner.data.base.DeviceEntity
import com.purelilith.networkscanner.data.scanner.DeviceType
import com.purelilith.networkscanner.data.scanner.NameSource
import com.purelilith.networkscanner.ui.NetworkScannerViewModel
import com.purelilith.networkscanner.ui.components.AppButton
import com.purelilith.networkscanner.ui.components.ButtonKind
import com.purelilith.networkscanner.ui.components.DeviceBadge
import com.purelilith.networkscanner.ui.components.InfoLine
import com.purelilith.networkscanner.ui.components.SectionTitle
import com.purelilith.networkscanner.ui.components.formatDateTime
import com.purelilith.networkscanner.ui.components.icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsSheet(
    device: DeviceEntity,
    viewModel: NetworkScannerViewModel,
    onDismiss: () -> Unit,
    onIgnore: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val seenTimesFlow = remember(device.deviceKey) {
        viewModel.observeDeviceSeenTimes(device.deviceKey)
    }
    val seenTimes by seenTimesFlow.collectAsState(initial = emptyList())

    val seenTexts = remember(seenTimes) { seenTimes.take(8).map { formatDateTime(it) } }
    val firstSeenText = remember(device.firstSeen) { formatDateTime(device.firstSeen) }
    val lastSeenText = remember(device.lastSeen) { formatDateTime(device.lastSeen) }

    var nameInput by remember(device.deviceKey) { mutableStateOf(device.customName) }
    var typeMenuOpen by remember { mutableStateOf(false) }

    val currentType = DeviceType.fromName(device.deviceType)
    val nameSourceLabel = NameSource.entries
        .firstOrNull { it.name == device.nameSource }?.label ?: "—"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DeviceBadge(currentType)
                Column {
                    Text(
                        text = device.customName.ifBlank { device.title },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = device.lastIp ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Своё название")
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    placeholder = { Text(device.title) },
                    modifier = Modifier.fillMaxWidth()
                )
                AppButton(
                    text = "Сохранить название",
                    onClick = { viewModel.updateCustomName(device.deviceKey, nameInput.trim()) },
                    enabled = nameInput.trim() != device.customName,
                    kind = ButtonKind.Tonal,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Тип устройства")
                Box {
                    AppButton(
                        text = currentType.label,
                        onClick = { typeMenuOpen = true },
                        icon = currentType.icon(),
                        kind = ButtonKind.Tonal,
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = typeMenuOpen,
                        onDismissRequest = { typeMenuOpen = false }
                    ) {
                        DeviceType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    viewModel.updateDeviceType(device.deviceKey, type.name)
                                    typeMenuOpen = false
                                }
                            )
                        }
                    }
                }
                if (device.typeIsGuess && currentType != DeviceType.UNKNOWN) {
                    Text(
                        text = "Тип определён по косвенным признакам. Если это не так, выберите нужный.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Сведения")
                InfoLine("Имя хоста", device.hostname ?: "—")
                InfoLine("Название определено", nameSourceLabel)
                InfoLine("Впервые в сети", firstSeenText)
                InfoLine("Последний раз", lastSeenText)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Когда видели")
                if (seenTexts.isEmpty()) {
                    Text(
                        text = "В сохранённых сканированиях этого устройства нет.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    seenTexts.forEach { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (seenTimes.size > 8) {
                        Text(
                            text = "и другие",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (onIgnore != null || onDelete != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (onIgnore != null) {
                        AppButton(
                            text = "В чёрный список",
                            onClick = onIgnore,
                            kind = ButtonKind.Tonal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (onDelete != null) {
                        AppButton(
                            text = "Удалить",
                            onClick = onDelete,
                            kind = ButtonKind.Danger,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}