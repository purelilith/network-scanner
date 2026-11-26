package com.purelilith.networkscanner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purelilith.networkscanner.ui.components.blockTouches
import com.purelilith.networkscanner.ui.screens.BlacklistScreen
import com.purelilith.networkscanner.ui.screens.DevicesScreen
import com.purelilith.networkscanner.ui.screens.JournalScreen
import com.purelilith.networkscanner.ui.screens.ScannerScreen
import com.purelilith.networkscanner.ui.theme.appBackgroundBrush
import kotlinx.coroutines.launch

private enum class Tab(val title: String, val icon: ImageVector) {
    Scanner("Сканер", Icons.Rounded.Wifi),
    Devices("Устройства", Icons.Rounded.Devices),
    Journal("Журнал", Icons.Rounded.History)
}

@Composable
fun AppRoot() {
    val viewModel: NetworkScannerViewModel = viewModel()
    val pagerState = rememberPagerState(pageCount = { Tab.entries.size })
    val scope = rememberCoroutineScope()
    var showBlacklist by remember { mutableStateOf(false) }

    BackHandler(enabled = showBlacklist) {
        showBlacklist = false
    }
    BackHandler(enabled = !showBlacklist && pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackgroundBrush())
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                FloatingTabBar(
                    selectedIndex = pagerState.currentPage,
                    onSelect = { index ->
                        showBlacklist = false
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (Tab.entries[page]) {
                        Tab.Scanner -> ScannerScreen(viewModel = viewModel)
                        Tab.Devices -> DevicesScreen(
                            viewModel = viewModel,
                            onOpenBlacklist = { showBlacklist = true }
                        )
                        Tab.Journal -> JournalScreen(viewModel = viewModel)
                    }
                }

                AnimatedVisibility(
                    visible = showBlacklist,
                    enter = slideInHorizontally(animationSpec = tween(300)) { it / 2 } +
                            fadeIn(animationSpec = tween(300)),
                    exit = slideOutHorizontally(animationSpec = tween(250)) { it / 2 } +
                            fadeOut(animationSpec = tween(200))
                ) {
                    BlacklistScreen(
                        viewModel = viewModel,
                        onBack = { showBlacklist = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(appBackgroundBrush())
                            .blockTouches()
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingTabBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Tab.entries.forEachIndexed { index, tab ->
                TabItem(
                    tab = tab,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: Tab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "tabBackground"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 250),
        label = "tabContent"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = tab.title,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}