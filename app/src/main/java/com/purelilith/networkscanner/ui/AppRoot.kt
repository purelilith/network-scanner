package com.purelilith.networkscanner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purelilith.networkscanner.ui.screens.BlacklistScreen
import com.purelilith.networkscanner.ui.screens.DevicesScreen
import com.purelilith.networkscanner.ui.screens.JournalScreen
import com.purelilith.networkscanner.ui.screens.ScannerScreen
import com.purelilith.networkscanner.ui.theme.appBackgroundBrush

sealed class Screen(val route: String, val title: String) {
    data object Scanner : Screen("scanner", "Сканер")
    data object Devices : Screen("devices", "Устройства")
    data object Journal : Screen("journal", "Журнал")
    data object Blacklist : Screen("blacklist", "Чёрный список")
}

private data class NavItem(val screen: Screen, val icon: ImageVector)

private val navItems = listOf(
    NavItem(Screen.Scanner, Icons.Rounded.Wifi),
    NavItem(Screen.Devices, Icons.Rounded.Devices),
    NavItem(Screen.Journal, Icons.Rounded.History)
)

@Composable
fun AppRoot() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Scanner) }
    val viewModel: NetworkScannerViewModel = viewModel()

    BackHandler(enabled = currentScreen == Screen.Blacklist) {
        currentScreen = Screen.Devices
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackgroundBrush())
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp
                ) {
                    navItems.forEach { item ->
                        val selected = currentScreen == item.screen ||
                                (item.screen == Screen.Devices && currentScreen == Screen.Blacklist)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentScreen = item.screen },
                            icon = { Icon(imageVector = item.icon, contentDescription = null) },
                            label = { Text(item.screen.title) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)
            when (currentScreen) {
                Screen.Scanner -> ScannerScreen(viewModel = viewModel, modifier = contentModifier)
                Screen.Devices -> DevicesScreen(
                    viewModel = viewModel,
                    onOpenBlacklist = { currentScreen = Screen.Blacklist },
                    modifier = contentModifier
                )
                Screen.Journal -> JournalScreen(viewModel = viewModel, modifier = contentModifier)
                Screen.Blacklist -> BlacklistScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.Devices },
                    modifier = contentModifier
                )
            }
        }
    }
}