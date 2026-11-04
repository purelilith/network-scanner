package com.purelilith.networkscanner.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purelilith.networkscanner.data.base.DeviceEntity
import com.purelilith.networkscanner.data.scanner.FriendlyName
import com.purelilith.networkscanner.ui.theme.NetworkScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FriendlyName.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            NetworkScannerTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun DeviceCardContainer(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier,
    viewModel: NetworkScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val devices by viewModel.devices.collectAsState()
    val knownDevices by viewModel.knownDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val lastScanTime by viewModel.lastScanTime.collectAsState()

    val deviceMap = remember(knownDevices) {
        knownDevices.associateBy { it.lastIp }
    }

    val ignoredIps = remember(knownDevices) {
        knownDevices.filter { it.isIgnored }.mapNotNull { it.lastIp }.toSet()
    }

    val filteredDevices = remember(devices, ignoredIps) {
        devices.filter { device -> !ignoredIps.contains(device.ip) }
    }

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = isScanning,
        onRefresh = { viewModel.startScan() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Найдено ${filteredDevices.size} устройств")
            Text(text = formatLastScanTime(lastScanTime))
            Button(
                onClick = { viewModel.startScan() },
                enabled = !isScanning
            ) {
                Text(if (isScanning) "Сканирую..." else "Сканировать")
            }
            LazyColumn {
                items(filteredDevices) { device ->
                    val matchedEntity = deviceMap[device.ip]
                    val displayName = matchedEntity?.customName?.takeIf { it.isNotBlank() }
                        ?: device.name.title

                    var showEditDialog by remember { mutableStateOf(false) }

                    DeviceCardContainer(
                        onClick = {
                            if (matchedEntity != null) showEditDialog = true
                        }
                    ) {
                        Text(text = displayName)
                        device.name.detail?.let { Text(text = it) }
                        Text(text = "${device.ip} · ${device.name.source.label}")
                        Text(text = "hostname: ${device.hostname ?: "—"}")
                    }

                    if (showEditDialog && matchedEntity != null) {
                        EditDeviceNameDialog(
                            initialName = matchedEntity.customName.ifBlank { matchedEntity.title },
                            onDismiss = { showEditDialog = false },
                            onSave = { newName ->
                                viewModel.updateCustomName(matchedEntity.deviceKey, newName)
                                showEditDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DevicesScreen(
    modifier: Modifier = Modifier,
    viewModel: NetworkScannerViewModel = viewModel()
) {
    val knownDevices by viewModel.knownDevices.collectAsState()

    var deviceToIgnore by remember { mutableStateOf<DeviceEntity?>(null) }
    var deviceToEdit by remember { mutableStateOf<DeviceEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (knownDevices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Список известных устройств пуст")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text(
                        text = "Всего сохранено: ${knownDevices.size}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(knownDevices) { device ->
                    DeviceCardContainer(
                        onClick = { deviceToEdit = device }
                    ) {
                        Text(
                            text = device.customName.ifBlank { device.title },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(text = "IP: ${device.lastIp}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Ключ: ${device.deviceKey}", style = MaterialTheme.typography.bodySmall)

                        Button(
                            onClick = { deviceToIgnore = device },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("В черный список")
                        }
                    }
                }
            }
        }
    }

    deviceToEdit?.let { device ->
        EditDeviceNameDialog(
            initialName = device.customName.ifBlank { device.title },
            onDismiss = { deviceToEdit = null },
            onSave = { newName ->
                viewModel.updateCustomName(device.deviceKey, newName)
                deviceToEdit = null
            }
        )
    }

    deviceToIgnore?.let { device ->
        IgnoreDeviceDialog(
            deviceName = device.customName.ifBlank { device.title },
            onDismiss = { deviceToIgnore = null },
            onConfirm = {
                viewModel.setDeviceIgnored(device.deviceKey, true)
                deviceToIgnore = null
            }
        )
    }
}

@Composable
fun IgnoreDeviceDialog(
    deviceName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Черный список") },
        text = { Text("Добавить устройство «$deviceName» в черный список? Оно будет скрыто со всех экранов сканирования навсегда.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Игнорировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun BlacklistScreen(
    modifier: Modifier = Modifier,
    viewModel: NetworkScannerViewModel = viewModel()
) {
    val ignoredDevices by viewModel.ignoredDevices.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        if (ignoredDevices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Черный список пуст")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text(
                        text = "Игнорируемые устройства",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(ignoredDevices) { device ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = device.customName.ifBlank { device.title }, style = MaterialTheme.typography.bodyLarge)
                            Text(text = "IP: ${device.lastIp}", style = MaterialTheme.typography.bodyMedium)
                            Button(
                                onClick = { viewModel.setDeviceIgnored(device.deviceKey, false) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Разрешить сканирование")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditDeviceNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить имя устройства") },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Новое имя") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSave(nameInput) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun JournalScreen(
    modifier: Modifier = Modifier,
    viewModel: NetworkScannerViewModel = viewModel()
) {
    val history by viewModel.scanHistory.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("История сканирований пуста")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text(
                        text = "История сканирований",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(history) { scan ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Сканирование #${scan.id}")
                            Text(text = "Найдено устройств: ${scan.deviceCount}")
                            Text(text = "Длительность: ${scan.durationMs} мс")
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    data object Scanner : Screen("scanner", "Сканер")
    data object Devices : Screen("devices", "Устройства")
    data object Journal : Screen("journal", "Журнал")
    data object Blacklist : Screen("blacklist", "Черный список")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Scanner) }
    val viewModel: NetworkScannerViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentScreen.title) },
                actions = {
                    IconButton(onClick = { currentScreen = Screen.Blacklist }) {
                        Icon(Icons.Default.Block, contentDescription = "Черный список")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf(Screen.Scanner, Screen.Devices, Screen.Journal).forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {},
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (currentScreen) {
            Screen.Scanner -> ScannerScreen(modifier, viewModel)
            Screen.Devices -> DevicesScreen(modifier, viewModel)
            Screen.Journal -> JournalScreen(modifier, viewModel)
            Screen.Blacklist -> BlacklistScreen(modifier, viewModel)
        }
    }
}