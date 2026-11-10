package com.purelilith.networkscanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.purelilith.networkscanner.data.scanner.DeviceType
import com.purelilith.networkscanner.ui.NetworkScannerViewModel
import com.purelilith.networkscanner.ui.components.AppButton
import com.purelilith.networkscanner.ui.components.ButtonKind
import com.purelilith.networkscanner.ui.components.DeviceRow
import com.purelilith.networkscanner.ui.components.EmptyState
import com.purelilith.networkscanner.ui.components.HeaderIconButton
import com.purelilith.networkscanner.ui.components.ScreenHeader

@Composable
fun BlacklistScreen(
    viewModel: NetworkScannerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ignoredDevices by viewModel.ignoredDevices.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Чёрный список",
            subtitle = "Эти устройства скрыты из сканирования",
            modifier = Modifier.padding(start = 8.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
            navigationIcon = {
                HeaderIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    description = "Назад",
                    onClick = onBack
                )
            }
        )

        if (ignoredDevices.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Block,
                title = "Список пуст",
                text = "Скрытые устройства появятся здесь, и вы сможете вернуть их в любой момент.",
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
                items(ignoredDevices, key = { it.deviceKey }) { device ->
                    DeviceRow(
                        title = device.customName.ifBlank { device.title },
                        type = DeviceType.fromName(device.deviceType),
                        trailing = {
                            AppButton(
                                text = "Вернуть",
                                onClick = { viewModel.setDeviceIgnored(device.deviceKey, false) },
                                kind = ButtonKind.Tonal
                            )
                        }
                    )
                }
            }
        }
    }
}