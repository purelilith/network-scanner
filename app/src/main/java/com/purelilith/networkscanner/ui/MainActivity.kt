package com.purelilith.networkscanner.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import com.purelilith.networkscanner.data.scanner.DeviceType
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var deviceToDelete by remember { mutableStateOf<DeviceEntity?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var deviceForHelpInfo by remember { mutableStateOf<DeviceEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (knownDevices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Список известных устройств пуст")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Всего сохранено: ${knownDevices.size}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        IconButton(
                            onClick = { showDeleteAllDialog = true },
                            enabled = knownDevices.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить все",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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

                        val deviceType = DeviceType.fromName(device.deviceType)
                        val isGuess = device.typeIsGuess && deviceType != DeviceType.UNKNOWN

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = "Тип: ${deviceType.label}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (isGuess) {
                                IconButton(
                                    onClick = { deviceForHelpInfo = device },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = "Сомнение в типе устройства",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Text(text = "Ключ: ${device.deviceKey}", style = MaterialTheme.typography.bodySmall)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { deviceToIgnore = device },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("В черный список")
                            }

                            OutlinedButton(
                                onClick = { deviceToDelete = device },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Удалить", color = MaterialTheme.colorScheme.error)
                            }
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

    deviceToDelete?.let { device ->
        DeleteDeviceDialog(
            deviceName = device.customName.ifBlank { device.title },
            onDismiss = { deviceToDelete = null },
            onConfirm = {
                viewModel.deleteDevice(device.deviceKey)
                deviceToDelete = null
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

    deviceForHelpInfo?.let { device ->
        AlertDialog(
            onDismissRequest = { deviceForHelpInfo = null },
            title = { Text("Неточное определение") },
            text = {
                Text("Тип этого устройства («${DeviceType.fromName(device.deviceType).label}») определён не на 100%, а на основе открытых портов или косвенных признаков.")
            },
            confirmButton = {
                Button(onClick = { deviceForHelpInfo = null }) {
                    Text("Понятно")
                }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Очистить всю память?") },
            text = { Text("Все сохраненные устройства будут удалены из базы данных. Они могут появиться снова при следующем сканировании.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllDevices()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить все")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun DeleteDeviceDialog(
    deviceName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удаление") },
        text = {
            Text("Устройство «$deviceName» будет удалено из памяти. Оно может снова появиться при следующем сканировании сети.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Удалить")
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
                            // formatted date "13.08.2026 10:30"
                            val formattedDate = remember(scan.startedAt) {
                                val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))
                                formatter.format(Date(scan.startedAt))
                            }

                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(text = "Найдено устройств: ${scan.deviceCount}")
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