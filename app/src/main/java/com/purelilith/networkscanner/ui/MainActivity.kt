package com.purelilith.networkscanner.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purelilith.networkscanner.data.scanner.FriendlyName
import com.purelilith.networkscanner.ui.theme.NetworkScannerTheme
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment


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

    @Preview(showBackground = true)

    @Composable
    fun ScannerScreen(
        modifier: Modifier = Modifier,
        viewModel: NetworkScannerViewModel = viewModel()
    ) {
        val context = LocalContext.current
        val devices by viewModel.devices.collectAsState()
        val isScanning by viewModel.isScanning.collectAsState()
        val lastScanTime by viewModel.lastScanTime.collectAsState()

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
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = device.name.title)
                            device.name.detail?.let { Text(text = it) }
                            Text(text = "${device.ip} · ${device.name.source.label}")
                            Text(text = "hostname: ${device.hostname ?: "—"}")
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
        // Подключаем список всех сохраненных (известных) устройств из базы данных
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
                        // Отображаем сохраненные сущности DeviceEntity из базы данных
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = device.hostname ?: "Неизвестное устройство", style = MaterialTheme.typography.bodyLarge)
                                Text(text = "IP: ${device.lastIp}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Ключ: ${device.deviceKey}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun JournalScreen(
        modifier: Modifier = Modifier,
        viewModel: NetworkScannerViewModel = viewModel()
    ) {
        // Подключаем историю сканирований из базы данных
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
                        // Отображаем элементы ScanEntity из базы данных
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
