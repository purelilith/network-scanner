package com.purelilith.networkscanner.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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