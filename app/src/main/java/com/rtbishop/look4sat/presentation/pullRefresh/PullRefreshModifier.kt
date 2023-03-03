/*
 * Copyright 2022 The Android Open Source Project
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

package com.rtbishop.look4sat.presentation.pullRefresh

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.Drag
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.unit.Velocity

/**
 * A modifier for translating the position and scaling the size of a pull-to-refresh indicator
 * based on the given [PullRefreshState].
 * @param state The [PullRefreshState] which determines the position of the indicator.
 * @param scale A boolean controlling whether the indicator's size scales with pull progress or not.
 */
fun Modifier.pullRefreshIndicatorTransform(
    state: PullRefreshState,
    scale: Boolean = false,
) = composed(inspectorInfo = debugInspectorInfo {
    name = "pullRefreshIndicatorTransform"
    properties["state"] = state
    properties["scale"] = scale
}) {
    var height by remember { mutableStateOf(0) }

    Modifier
        .onSizeChanged { height = it.height }
        .graphicsLayer {
            translationY = state.position - height

            if (scale && !state.refreshing) {
                val scaleFraction = LinearOutSlowInEasing
                    .transform(state.position / state.threshold)
                    .coerceIn(0f, 1f)
                scaleX = scaleFraction
                scaleY = scaleFraction
            }
        }
}

/**
 * PullRefresh modifier to be used in conjunction with [PullRefreshState]. Provides a connection
 * to the nested scroll system. Based on Android's SwipeRefreshLayout.
 * @param state The [PullRefreshState] associated with this pull-to-refresh component.
 * The state will be updated by this modifier.
 * @param enabled If not enabled, all scroll delta and fling velocity will be ignored.
 */
fun Modifier.pullRefresh(
    state: PullRefreshState, enabled: Boolean = true
) = inspectable(inspectorInfo = debugInspectorInfo {
    name = "pullRefresh"
    properties["state"] = state
    properties["enabled"] = enabled
}) {
    Modifier.pullRefresh(state::onPull, { state.onRelease() }, enabled)
}

/**
 * A modifier for building pull-to-refresh components. Provides a connection to the nested scroll
 * system.
 * @param onPull Callback for dispatching vertical scroll delta, takes float pullDelta as argument.
 * Positive delta (pulling down) is dispatched only if the child does not consume it (i.e. pulling
 * down despite being at the top of a scrollable component), whereas negative delta (swiping up) is
 * dispatched first (in case it is needed to push the indicator back up), and then whatever is not
 * consumed is passed on to the child.
 * @param onRelease Callback for when drag is released, takes float flingVelocity as argument.
 * @param enabled If not enabled, all scroll delta and fling velocity will be ignored and neither
 * [onPull] nor [onRelease] will be invoked.
 */
fun Modifier.pullRefresh(
    onPull: (pullDelta: Float) -> Float,
    onRelease: suspend (flingVelocity: Float) -> Unit,
    enabled: Boolean = true
) = inspectable(inspectorInfo = debugInspectorInfo {
    name = "pullRefresh"
    properties["onPull"] = onPull
    properties["onRelease"] = onRelease
    properties["enabled"] = enabled
}) {
    Modifier.nestedScroll(PullRefreshNestedScrollConnection(onPull, onRelease, enabled))
}

private class PullRefreshNestedScrollConnection(
    private val onPull: (pullDelta: Float) -> Float,
    private val onRelease: suspend (flingVelocity: Float) -> Unit,
    private val enabled: Boolean
) : NestedScrollConnection {

    override fun onPreScroll(
        available: Offset, source: NestedScrollSource
    ): Offset = when {
        !enabled -> Offset.Zero
        source == Drag && available.y < 0 -> Offset(0f, onPull(available.y)) // Swiping up
        else -> Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset, available: Offset, source: NestedScrollSource
    ): Offset = when {
        !enabled -> Offset.Zero
        source == Drag && available.y > 0 -> Offset(0f, onPull(available.y)) // Pulling down
        else -> Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        onRelease(available.y)
        return Velocity.Zero
    }
}
