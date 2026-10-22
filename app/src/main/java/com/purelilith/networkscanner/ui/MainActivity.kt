package com.purelilith.networkscanner.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ScannerScreen(modifier = Modifier.padding(innerPadding))
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
            onRefresh = { viewModel.startScan(context) }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Найдено ${devices.size} устройств")
                Text(text = formatLastScanTime(lastScanTime))
                Button(
                    onClick = { viewModel.startScan(context) },
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
}