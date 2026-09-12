/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.core.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Long-press drag-to-reorder for rows inside a `LazyColumn`, with built-in edge auto-scroll and
 * a settle-back animation when the finger is released.
 *
 * Reordering only ever swaps an item with its neighbor *inside the `items` list* passed to
 * [rememberDragRowState] — so multiple independent groups of rows (e.g. two sections separated
 * by header items) can share one [DragReorderState]/`LazyColumn` simply by giving each section
 * its own backing list and calling [rememberDragRowState] with that section's list. Keys must be
 * globally unique across the whole `LazyColumn` and match exactly what is passed as the
 * `key = { ... }` lambda of `items`/`itemsIndexed`.
 *
 * Usage:
 * ```
 * val dragState = rememberDragReorderState(listState)
 * itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
 *     val row = rememberDragRowState(dragState, entries, entry, key = { it.id }) { from, to ->
 *         entries.add(to, entries.removeAt(from))
 *     }
 *     Row(
 *         modifier = Modifier
 *             .dragLift(row.isLifted, row.translationY)
 *             .animateItem(placementSpec = if (row.isDragging) tween(0) else spring())
 *     ) {
 *         Icon(modifier = Modifier.dragHandle(row), painter = ..., contentDescription = null)
 *         Text(entry.label)
 *     }
 * }
 * ```
 */
@Stable
class DragReorderState internal constructor(internal val listState: LazyListState) {
    internal var draggedKey by mutableStateOf<Any?>(null)
}

@Composable
fun rememberDragReorderState(listState: LazyListState): DragReorderState =
    remember(listState) { DragReorderState(listState) }

/** Used for the settle-back animation once the finger is released: quick and snappy. */
private val reorderSettleSpec = tween<Float>(durationMillis = 150, easing = FastOutSlowInEasing)

/**
 * Per-row drag bookkeeping, obtained via [rememberDragRowState].
 *
 * [dragStartOffset] is this row's viewport offset captured once at drag start, [fingerOffset] is
 * the raw accumulated finger travel (used for edge auto-scroll and swap detection). [translationY]
 * is derived every frame from those two plus the row's *current* live layout offset, so it
 * automatically stays correct across neighbor swaps and list auto-scroll without any manual
 * offset-compensation math.
 */
@Stable
class DragRowState<T> internal constructor(
    private val dragState: DragReorderState,
    private val itemsState: State<List<T>>,
    private val itemKey: Any,
    private val key: (T) -> Any,
    private val onMoveState: State<(Int, Int) -> Unit>,
    private val scope: CoroutineScope
) {
    internal val fingerOffset: MutableFloatState = mutableFloatStateOf(0f)
    internal val dragStartOffset: MutableFloatState = mutableFloatStateOf(0f)
    internal val startCenterY: MutableFloatState = mutableFloatStateOf(0f)
    internal val settleAnim = Animatable(0f)

    var isSettling: Boolean by mutableStateOf(false)
        private set

    val isDragging: Boolean get() = dragState.draggedKey == itemKey
    val isLifted: Boolean get() = isDragging || isSettling

    val translationY: Float
        get() = when {
            isSettling -> settleAnim.value
            isDragging -> currentTranslation()
            else -> 0f
        }

    /**
     * How far this row must be pushed away from its *current* layout slot so it stays glued to
     * the finger. Because [dragStartOffset] is fixed at drag start while the row's live layout
     * offset moves as neighbors swap places or the list auto-scrolls, this difference naturally
     * absorbs both effects with no extra bookkeeping.
     */
    private fun currentTranslation(): Float {
        val liveOffset = dragState.listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == itemKey }?.offset?.toFloat()
            ?: dragStartOffset.floatValue
        return dragStartOffset.floatValue + fingerOffset.floatValue - liveOffset
    }

    internal fun onDragStart() {
        isSettling = false
        val layout = dragState.listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == itemKey }
        dragStartOffset.floatValue = (layout?.offset ?: 0).toFloat()
        startCenterY.floatValue = (layout?.offset ?: 0) + (layout?.size ?: 0) / 2f
        fingerOffset.floatValue = 0f
        dragState.draggedKey = itemKey
    }

    internal fun onDrag(deltaY: Float) {
        if (dragState.draggedKey != itemKey) return
        fingerOffset.floatValue += deltaY
        reorderLive()
    }

    private fun reorderLive() {
        val items = itemsState.value
        val myIndex = items.indexOfFirst { key(it) == itemKey }
        if (myIndex !in items.indices) return
        val myCenter = startCenterY.floatValue + fingerOffset.floatValue
        val visible = dragState.listState.layoutInfo.visibleItemsInfo
        val onMove = onMoveState.value
        // Dragging down: swap when the dragged center passes the next row's midpoint.
        if (myIndex < items.lastIndex) {
            val nextKey = key(items[myIndex + 1])
            val next = visible.firstOrNull { it.key == nextKey }
            if (next != null && myCenter > next.offset + next.size / 2f) {
                onMove(myIndex, myIndex + 1)
                return
            }
        }
        // Dragging up: swap when the dragged center passes the previous row's midpoint.
        if (myIndex > 0) {
            val prevKey = key(items[myIndex - 1])
            val prev = visible.firstOrNull { it.key == prevKey }
            if (prev != null && myCenter < prev.offset + prev.size / 2f) {
                onMove(myIndex, myIndex - 1)
            }
        }
    }

    // Reset the drag bookkeeping and fly the lifted row back into its slot.
    // All state resets happen inside the launched block so the settle animation takes over from
    // the current visual position without a one-frame jump: isSettling is flipped to true
    // (switching rendering to settleAnim, already snapped to the last offset) before the drag
    // flags are cleared.
    internal fun onDragEnd() {
        val lastOffset = currentTranslation()
        if (kotlin.math.abs(lastOffset) < 1f) {
            fingerOffset.floatValue = 0f
            dragState.draggedKey = null
            return
        }
        scope.launch {
            settleAnim.snapTo(lastOffset)
            isSettling = true
            fingerOffset.floatValue = 0f
            dragState.draggedKey = null
            settleAnim.animateTo(0f, reorderSettleSpec)
            isSettling = false
        }
    }
}

/**
 * Remembers a [DragRowState] for one row of a drag-reorderable list.
 *
 * [items] must be the exact (optionally section-scoped) list backing the enclosing
 * `items`/`itemsIndexed` call, and [key] must return the same value used as that call's
 * `key = { ... }` lambda. Reordering only ever swaps neighbors within [items], so passing a
 * section-local list is what confines dragging to one section of a multi-section `LazyColumn`.
 */
@Composable
fun <T> rememberDragRowState(
    dragState: DragReorderState,
    items: List<T>,
    item: T,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit
): DragRowState<T> {
    val scope = rememberCoroutineScope()
    val itemsState = rememberUpdatedState(items)
    val onMoveState = rememberUpdatedState(onMove)
    val itemKey = key(item)
    val rowState = remember(dragState, itemKey) {
        DragRowState(dragState, itemsState, itemKey, key, onMoveState, scope)
    }
    LaunchedEffect(rowState.isDragging) {
        if (!rowState.isDragging) return@LaunchedEffect
        autoScrollWhileDragging(dragState.listState, rowState.startCenterY, rowState.fingerOffset)
    }
    return rowState
}

/** Drag-handle gesture: attach to a small handle icon to start/drive/end reordering of [rowState]'s row. */
fun Modifier.dragHandle(rowState: DragRowState<*>): Modifier = pointerInput(rowState) {
    detectDragGesturesAfterLongPress(
        onDragStart = { rowState.onDragStart() },
        onDragEnd = { rowState.onDragEnd() },
        onDragCancel = { rowState.onDragEnd() }
    ) { change, dragAmount ->
        change.consume()
        rowState.onDrag(dragAmount.y)
    }
}

/**
 * Visual treatment for a draggable row: while [isLifted], translates the row by [translationY],
 * scales it up slightly and raises it above its neighbors with a shadow and a solid background so
 * it fully covers the row beneath instead of showing a translucent overlap of both rows.
 */
@Composable
fun Modifier.dragLift(isLifted: Boolean, translationY: Float): Modifier {
    val shape = MaterialTheme.shapes.small
    val scale by animateFloatAsState(
        targetValue = if (isLifted) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dragScale"
    )
    return this
        .graphicsLayer {
            if (isLifted) {
                this.translationY = translationY
                scaleX = scale
                scaleY = scale
            }
        }
        .then(
            if (isLifted) {
                Modifier
                    .zIndex(1f)
                    .shadow(8.dp, shape, clip = false)
                    .background(MaterialTheme.colorScheme.surface, shape)
            } else {
                Modifier
            }
        )
}

/**
 * Scrolls the list while dragging so the entry follows the finger past the viewport edges.
 * The visual center is tracked independently of the entry's layout slot (which can scroll out of
 * `LazyListState.layoutInfo.visibleItemsInfo` during a long drag); [startCenterY] is the entry's
 * viewport center captured at drag start and [fingerOffset] is the raw finger delta. The
 * resulting scroll is picked up automatically by [DragRowState.translationY] on the next frame,
 * so no separate scroll-compensation bookkeeping is required here.
 */
private suspend fun autoScrollWhileDragging(
    listState: LazyListState,
    startCenterY: MutableFloatState,
    fingerOffset: MutableFloatState
) {
    val threshold = 48f
    val maxSpeed = 24f
    while (true) {
        val info = listState.layoutInfo
        val center = startCenterY.floatValue + fingerOffset.floatValue
        val top = info.viewportStartOffset + threshold
        val bottom = info.viewportEndOffset - threshold
        val delta = when {
            center < top -> -(top - center).coerceAtMost(maxSpeed)
            center > bottom -> (center - bottom).coerceAtMost(maxSpeed)
            else -> 0f
        }
        if (delta != 0f) listState.scrollBy(delta)
        delay(16L.milliseconds)
    }
}
