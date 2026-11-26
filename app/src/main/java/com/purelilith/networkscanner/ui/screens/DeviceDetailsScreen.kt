package com.purelilith.networkscanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.purelilith.networkscanner.data.base.DeviceEntity
import com.purelilith.networkscanner.data.scanner.DeviceType
import com.purelilith.networkscanner.data.scanner.NameSource
import com.purelilith.networkscanner.ui.NetworkScannerViewModel
import com.purelilith.networkscanner.ui.components.AppButton
import com.purelilith.networkscanner.ui.components.ButtonKind
import com.purelilith.networkscanner.ui.components.HeaderIconButton
import com.purelilith.networkscanner.ui.components.InfoLine
import com.purelilith.networkscanner.ui.components.ScreenHeader
import com.purelilith.networkscanner.ui.components.SectionTitle
import com.purelilith.networkscanner.ui.components.formatDateTime
import com.purelilith.networkscanner.ui.components.icon

@Composable
fun DeviceDetailsScreen(
    device: DeviceEntity,
    viewModel: NetworkScannerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onIgnore: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val seenTimesFlow = remember(device.deviceKey) {
        viewModel.observeDeviceSeenTimes(device.deviceKey)
    }
    val seenTimes by seenTimesFlow.collectAsState(initial = emptyList())

    val seenTexts = remember(seenTimes) { seenTimes.take(8).map { formatDateTime(it) } }
    val firstSeenText = remember(device.firstSeen) { formatDateTime(device.firstSeen) }
    val lastSeenText = remember(device.lastSeen) { formatDateTime(device.lastSeen) }

    var nameInput by remember(device.deviceKey) { mutableStateOf(device.customName) }
    var typesExpanded by remember(device.deviceKey) { mutableStateOf(false) }

    val currentType = DeviceType.fromName(device.deviceType)
    val nameSourceLabel = NameSource.entries
        .firstOrNull { it.name == device.nameSource }?.label ?: "—"

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = device.customName.ifBlank { device.title },
            subtitle = device.lastIp,
            modifier = Modifier.padding(start = 8.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
            navigationIcon = {
                HeaderIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    description = "Назад",
                    onClick = onBack
                )
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DetailsCard(title = "Своё название") {
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

            DetailsCard(title = "Тип устройства") {
                AppButton(
                    text = currentType.label,
                    onClick = { typesExpanded = !typesExpanded },
                    icon = currentType.icon(),
                    kind = ButtonKind.Tonal,
                    modifier = Modifier.fillMaxWidth()
                )
                if (typesExpanded) {
                    DeviceType.entries.forEach { type ->
                        TypeOption(
                            type = type,
                            selected = type == currentType,
                            onClick = {
                                viewModel.updateDeviceType(device.deviceKey, type.name)
                                typesExpanded = false
                            }
                        )
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

            DetailsCard(title = "Сведения") {
                InfoLine("Имя хоста", device.hostname ?: "—")
                InfoLine("Название определено", nameSourceLabel)
                InfoLine("Впервые в сети", firstSeenText)
                InfoLine("Последний раз", lastSeenText)
            }

            DetailsCard(title = "Когда видели") {
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

@Composable
private fun DetailsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(title)
            content()
        }
    }
}

@Composable
private fun TypeOption(
    type: DeviceType,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = type.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = type.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(selected = selected, onClick = null)
    }
}