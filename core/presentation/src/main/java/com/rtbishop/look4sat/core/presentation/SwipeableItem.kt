package com.rtbishop.look4sat.core.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SwipeableItem(onSwipeRight: () -> Unit, onSwipeLeft: () -> Unit, content: @Composable () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val dismissThresholdPx = with(LocalDensity.current) { 120.dp.toPx() }
    val dismissState = rememberSwipeToDismissBoxState { dismissThresholdPx }
    val willTrigger by remember { derivedStateOf { dismissState.targetValue != SwipeToDismissBoxValue.Settled } }
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(willTrigger) {
        val feedbackType = if (willTrigger) HapticFeedbackType.LongPress else HapticFeedbackType.SegmentTick
        if (willTrigger) hapticFeedback.performHapticFeedback(feedbackType)
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isSwipeRight = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            SwipeBackground(isSwipeRight, willTrigger, MaterialTheme.colorScheme.primary)
        },
        content = { content() },
        onDismiss = {
            val targetValue = dismissState.targetValue
            when (targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> onSwipeRight()
                SwipeToDismissBoxValue.EndToStart -> onSwipeLeft()
                SwipeToDismissBoxValue.Settled -> {}
            }
            if (targetValue != SwipeToDismissBoxValue.Settled) {
                coroutineScope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
            }
        }
    )
}

@Composable
fun SwipeBackground(isSwipeRight: Boolean, willTrigger: Boolean, bgColor: Color = Color(0x00000000)) {
    val shape = RectangleShape
    Box(
        contentAlignment = if (isSwipeRight) Alignment.CenterStart else Alignment.CenterEnd,
        modifier = Modifier
            .fillMaxSize()
            .border(width = 1.dp, shape = shape, color = bgColor)
            .dropShadow(shape = shape) {
                color = bgColor
                radius = 40f
                alpha = if (willTrigger) .2f else 0f
            }
            .innerShadow(shape = shape) {
                color = bgColor
                radius = 40f
                alpha = if (willTrigger) 1f else .2f
            }
    ) {
        val iconScale by animateFloatAsState(targetValue = if (willTrigger) 1f else .8f)
        val slide by animateDpAsState(targetValue = if (willTrigger) 32.dp else (12).dp)
        Icon(
            painter = painterResource(R.drawable.ic_filter),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .fillMaxHeight()
                .rotate(if (isSwipeRight) 0f else 180f)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    translationX = slide.toPx()
                }
        )
    }
}
