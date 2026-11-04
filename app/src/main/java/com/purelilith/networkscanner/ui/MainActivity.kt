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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = isScanning,
        onRefresh = { viewModel.startScan() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Найдено ${devices.size} устройств")
            Text(text = formatLastScanTime(lastScanTime))
            Button(
                onClick = { viewModel.startScan() },
                enabled = !isScanning
            ) {
                Text(if (isScanning) "Сканирую..." else "Сканировать")
            }
            LazyColumn {
                items(devices) { device ->
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
                    var showEditDialog by remember { mutableStateOf(false) }

                    DeviceCardContainer(
                        onClick = { showEditDialog = true }
                    ) {
                        Text(
                            text = device.customName.ifBlank { device.title },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(text = "IP: ${device.lastIp}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Ключ: ${device.deviceKey}", style = MaterialTheme.typography.bodySmall)
                    }

                    if (showEditDialog) {
                        EditDeviceNameDialog(
                            initialName = device.customName.ifBlank { device.title },
                            onDismiss = { showEditDialog = false },
                            onSave = { newName ->
                                viewModel.updateCustomName(device.deviceKey, newName)
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

enum class Tab(val title: String) {
    SCANNER("Сканер"),
    DEVICES("Устройства"),
    JOURNAL("Журнал")
}

@Composable
fun AppRoot() {
    var currentTab by remember { mutableStateOf(Tab.SCANNER) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {},
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (currentTab) {
            Tab.SCANNER -> ScannerScreen(modifier)
            Tab.DEVICES -> DevicesScreen(modifier)
            Tab.JOURNAL -> JournalScreen(modifier)
        }
    }
}