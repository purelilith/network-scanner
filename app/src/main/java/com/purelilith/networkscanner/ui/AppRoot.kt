package com.purelilith.networkscanner.ui

import androidx.activity.compose.BackHandler
import androidx.benchmark.traceprocessor.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purelilith.networkscanner.ui.screens.BlacklistScreen
import com.purelilith.networkscanner.ui.screens.DevicesScreen
import com.purelilith.networkscanner.ui.screens.JournalScreen
import com.purelilith.networkscanner.ui.screens.ScannerScreen
import com.purelilith.networkscanner.ui.theme.appBackgroundBrush
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.purelilith.networkscanner.ui.theme.Sea

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
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {}
        ) { innerPadding ->
            val contentModifier = Modifier.padding(
                top = innerPadding.calculateTopPadding(),
                bottom = 120.dp
            )

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

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .navigationBarsPadding()
        ) {
            val panelShape = RoundedCornerShape(16.dp)
            val strokeColor = if (MaterialTheme.colorScheme.primary == Sea) {
                Color.Black.copy(alpha = 0.08f)
            } else {
                Color.White.copy(alpha = 0.12f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = panelShape,
                        clip = false
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = panelShape
                    )
                    .border(
                        width = 1.dp,
                        color = strokeColor,
                        shape = panelShape
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    val selected = currentScreen == item.screen ||
                            (item.screen == Screen.Devices && currentScreen == Screen.Blacklist)

                    val contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }

                    val segmentShape = when (index) {
                        0 -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        navItems.lastIndex -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                        else -> RoundedCornerShape(0.dp)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(segmentShape)
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = segmentShape
                            )
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                currentScreen = item.screen
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.screen.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}