package com.purelilith.networkscanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.purelilith.networkscanner.ui.theme.appBackgroundBrush

@Composable
fun Modifier.blockTouches(): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {}
        )
        .pointerInput(Unit) {
            detectHorizontalDragGestures { change, _ -> change.consume() }
        }
}

@Composable
fun <T : Any> rememberLastNonNull(value: T?): T? {
    val last = remember { mutableStateOf<T?>(null) }
    SideEffect {
        if (value != null) last.value = value
    }
    return value ?: last.value
}

@Composable
fun DetailOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInHorizontally(animationSpec = tween(300)) { it / 2 } +
                fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally(animationSpec = tween(250)) { it / 2 } +
                fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush())
                .blockTouches()
        ) {
            content()
        }
    }
}