/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rtbishop.look4sat.core.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

//region DragDrop Region
@Composable
fun rememberDragDropState(lazyListState: LazyListState, onMove: (Int, Int) -> Unit): DragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState) {
        DragDropState(state = lazyListState, onMove = onMove, scope = scope)
    }
    LaunchedEffect(state) {
        while (true) {
            val diff = state.scrollChannel.receive()
            lazyListState.scrollBy(diff)
        }
    }
    return state
}

class DragDropState internal constructor(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    internal val scrollChannel = Channel<Float>()

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)
    internal val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
        } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

    internal var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set
    internal var previousItemOffset = Animatable(0f)
        private set

    internal fun onDragStart(offset: Offset) {
        state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            offset.y.toInt() in item.offset..(item.offset + item.size)
        }?.also {
            draggingItemIndex = it.index
            draggingItemInitialOffset = it.offset
        }
    }

    internal fun onDragInterrupted() {
        if (draggingItemIndex != null) {
            previousIndexOfDraggedItem = draggingItemIndex
            val startOffset = draggingItemOffset
            scope.launch {
                previousItemOffset.snapTo(startOffset)
                previousItemOffset.animateTo(
                    0f, spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 1f)
                )
                previousIndexOfDraggedItem = null
            }
        }
        draggingItemDraggedDelta = 0f
        draggingItemIndex = null
        draggingItemInitialOffset = 0
    }

    internal fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset.y

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = startOffset + (endOffset - startOffset) / 2f

        val targetItem = state.layoutInfo.visibleItemsInfo.find { item ->
            middleOffset.toInt() in item.offset..item.offsetEnd && draggingItem.index != item.index
        }
        if (targetItem != null) {
            if (draggingItem.index == state.firstVisibleItemIndex || targetItem.index == state.firstVisibleItemIndex) {
                state.requestScrollToItem(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
            }
            onMove.invoke(draggingItem.index, targetItem.index)
            draggingItemIndex = targetItem.index
        } else {
            val overscroll = when {
                draggingItemDraggedDelta > 0 -> (endOffset - state.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                draggingItemDraggedDelta < 0 -> (startOffset - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)
                else -> 0f
            }
            if (overscroll != 0f) {
                scrollChannel.trySend(overscroll)
            }
        }
    }

    private val LazyListItemInfo.offsetEnd: Int
        get() = this.offset + this.size
}

fun Modifier.dragContainer(dragDropState: DragDropState): Modifier {
    return pointerInput(dragDropState) {
        detectDragGesturesAfterLongPress(
            onDrag = { change, offset ->
                change.consume()
                dragDropState.onDrag(offset = offset)
            },
            onDragStart = { offset -> dragDropState.onDragStart(offset) },
            onDragEnd = { dragDropState.onDragInterrupted() },
            onDragCancel = { dragDropState.onDragInterrupted() }
        )
    }
}

@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(isDragging: Boolean) -> Unit
) {
    val zIndexMod = Modifier.zIndex(1f)
    val dragging = index == dragDropState.draggingItemIndex
    val draggingModifier = when {
        dragging -> zIndexMod.graphicsLayer { translationY = dragDropState.draggingItemOffset }
        index == dragDropState.previousIndexOfDraggedItem -> {
            zIndexMod.graphicsLayer { translationY = dragDropState.previousItemOffset.value }
        }
        else -> Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
    }
    Column(modifier = modifier.then(draggingModifier)) { content(dragging) }
}
//endregion

//region Swipe Region
@Composable
fun SwipeableItem(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val dismissThresholdPx = with(LocalDensity.current) { 120.dp.toPx() }
    val dismissState = rememberSwipeToDismissBoxState { dismissThresholdPx }
    val isVisible = remember { mutableStateOf(true) }
    val willTrigger by remember { derivedStateOf { dismissState.targetValue != SwipeToDismissBoxValue.Settled } }
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(willTrigger) {
        val feedbackType = if (willTrigger) HapticFeedbackType.LongPress else HapticFeedbackType.SegmentTick
        hapticFeedback.performHapticFeedback(feedbackType)
    }
    AnimatedVisibility(visible = isVisible.value, exit = fadeOut(spring())) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = false,
            backgroundContent = { DismissBackground(willTrigger) },
            content = { content() },
            onDismiss = {
                isVisible.value = false
                coroutineScope.launch { dismissState.reset() }
            }
        )
    }
    LaunchedEffect(isVisible.value) {
        if (!isVisible.value) { delay(250.milliseconds).also { onRemove() } }
    }
}

@Composable
fun DismissBackground(willTrigger: Boolean, bgColor: Color = Color(0xFFFF3C3A)) {
    val iconScale by animateFloatAsState(targetValue = if (willTrigger) 1f else .8f)
    val slide by animateDpAsState(targetValue = if (willTrigger) 32.dp else (12).dp)
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxSize()
            .border(width = 2.dp, shape = MaterialTheme.shapes.extraLarge, color = bgColor)
            .dropShadow(shape = MaterialTheme.shapes.extraLarge) {
                color = bgColor
                radius = 40f
                alpha = if (willTrigger) .2f else 0f
            }
            .innerShadow(shape = MaterialTheme.shapes.extraLarge) {
                color = bgColor
                radius = 40f
                alpha = if (willTrigger) 1f else .2f
            }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    translationX = slide.toPx()
                }
        )
    }
}
//endregion
