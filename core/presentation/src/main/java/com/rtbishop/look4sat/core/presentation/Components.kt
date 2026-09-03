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

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.core.domain.predict.NearEarthObject
import com.rtbishop.look4sat.core.domain.predict.OrbitalData
import com.rtbishop.look4sat.core.domain.predict.OrbitalPass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
@Preview(showBackground = true)
private fun TopBarPreview() = MainTheme {
    TopBar {
        IconCard(action = {}, resId = R.drawable.ic_filter)
        TimerRow(timeString = "88:88:88", isTimeAos = true)
        IconCard(action = {}, resId = R.drawable.ic_radios)
    }
}

@Composable
fun TopBar(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun RowScope.TimerRow(timeString: String, isTimeAos: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    val (aosColor, losColor) = if (isTimeAos) {
        colorScheme.primary to colorScheme.onSurface
    } else {
        colorScheme.onSurface to colorScheme.primary
    }
    ElevatedCard(modifier = Modifier.weight(1f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = "AOS", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = aosColor)
            Text(
                text = timeString,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
            Text(text = "LOS", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = losColor)
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun NextPassRowPreview() = MainTheme {
    TopBar { NextPassRow(pass = getDefaultPass()) }
}

@Composable
fun RowScope.NextPassRow(pass: OrbitalPass, modifier: Modifier = Modifier, isUtc: Boolean = false) {
    val timeZone = remember(isUtc) {
        if (isUtc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
    }
    val sdfTime = remember(isUtc) {
        SimpleDateFormat("HH:mm:ss", displayLocale()).also { it.timeZone = timeZone }
    }
    ElevatedCard(
        modifier = modifier
            .height(48.dp)
            .weight(1f)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy((-2).dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, top = 1.dp, end = 6.dp, bottom = 0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${stringResource(R.string.pass_satId, pass.catNum)} - ",
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = pass.name,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                        .infiniteMarquee(),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val elevColor = elevationColor(pass.maxElevation)
                Icon(
                    painter = painterResource(R.drawable.ic_elevation),
                    contentDescription = null,
                    tint = elevColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${pass.maxElevation}°",
                    color = elevColor
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = sdfTime.format(Date(pass.aosTime)),
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "${pass.altitude} km", fontSize = 15.sp)
                }
                Text(
                    text = stringResource(R.string.pass_aosLos, pass.aosAzimuth.toInt(), pass.losAzimuth.toInt()),
                    fontSize = 15.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun displayLocale(): Locale {
    val locale = Locale.getDefault()
    return if (locale.language == Locale.CHINESE.language) locale else Locale.ENGLISH
}

@Composable
fun CardButton(onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
    ElevatedButton(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) { Text(text = text, fontSize = 16.sp, textAlign = TextAlign.Center) }
}

@Composable
fun IconCard(
    action: () -> Unit, resId: Int, modifier: Modifier = Modifier,
    enabled: Boolean = true, containerColor: Color = Color.Unspecified
) {
    val colors = if (containerColor == Color.Unspecified) CardDefaults.elevatedCardColors()
    else CardDefaults.elevatedCardColors(containerColor = containerColor)
    ElevatedCard(modifier = Modifier.size(48.dp), enabled = enabled, onClick = action, colors = colors) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(painter = painterResource(resId), contentDescription = null, modifier = modifier)
        }
    }
}

@Composable
fun PrimaryIconCard(modifier: Modifier = Modifier, resId: Int, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.size(102.dp, 48.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Box(
            modifier = modifier
                .clickable(onClick = onClick)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { Icon(painter = painterResource(resId), contentDescription = null) }
    }
}

@Composable
fun CardLoadingIndicator() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.size(80.dp))
    }
}

@Composable
fun EmptyListCard(message: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = """¯\_(ツ)_/¯""", fontSize = 32.sp)
            Text(text = stringResource(R.string.empty_list_message), fontSize = 21.sp, textAlign = TextAlign.Center)
            Text(text = message, fontSize = 18.sp, textAlign = TextAlign.Center)
        }
    }
}

private val defaultOrbitalData = OrbitalData(
    name = """ ¯\_(ツ)_/¯ ⚠️""",
    epoch = 0.0, meanmo = 0.0, eccn = 0.0, incl = 0.0,
    raan = 0.0, argper = 0.0, meanan = 0.0, catnum = 0, bstar = 0.0
)

fun getDefaultPass(): OrbitalPass = OrbitalPass(
    aosTime = 0L, aosAzimuth = 0.0, losTime = Long.MAX_VALUE, losAzimuth = 0.0,
    altitude = 0, maxElevation = 0.0, orbitalObject = NearEarthObject(defaultOrbitalData), progress = 0f
)

@Composable
fun InfoDialog(
    title: String,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    extraAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    DialogShell(onDismissRequest = onDismiss) { padding ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = padding, top = padding, end = padding)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            extraAction?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }
            CardButton(onClick = onAccept, text = stringResource(R.string.btn_accept))
        }
        content()
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    onCancel: () -> Unit,
    onAccept: () -> Unit,
    content: @Composable () -> Unit
) {
    DialogShell(onDismissRequest = onCancel) { padding ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = padding, top = padding, end = padding)
        ) {
            CardButton(onClick = onCancel, text = stringResource(R.string.btn_cancel))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = padding)
            )
            CardButton(onClick = onAccept, text = stringResource(R.string.btn_accept))
        }
        content()
    }
}

@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    InfoDialog(
        title = stringResource(R.string.pass_whatsnew_title),
        onDismiss = onDismiss,
        onAccept = onDismiss
    ) {
        Text(
            text = stringResource(R.string.pass_whatsnew_message),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = LocalSpacing.current.large)
        )
        Spacer(modifier = Modifier.height(0.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogShell(
    onDismissRequest: () -> Unit,
    content: @Composable (padding: Dp) -> Unit
) {
    val padding = LocalSpacing.current.large
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val containerHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val maxSheetHeight = containerHeight - statusBarHeight
    val stopSheetFling = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return available
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset.Zero
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        scrimColor = Color.Black.copy(alpha = 0.64f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(padding),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .nestedScroll(stopSheetFling)
        ) {
            content(padding)
        }
    }
}

@Composable
fun hasEnoughHeight(): Boolean =
    currentWindowAdaptiveInfo().windowSizeClass.isHeightAtLeastBreakpoint(480)

@Composable
fun hasEnoughWidth(): Boolean =
    currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(600)

@Composable
fun isVerticalLayout(): Boolean = !hasEnoughWidth()

@Composable
fun Modifier.infiniteMarquee(): Modifier =
    basicMarquee(iterations = Int.MAX_VALUE, spacing = MarqueeSpacing(16.dp))

@Composable
fun Modifier.layoutPadding(): Modifier {
    val spacing = LocalSpacing.current.extraSmall
    return statusBarsPadding().padding(start = spacing, top = 0.dp, end = spacing, bottom = spacing)
}

@Composable
fun ScreenColumn(
    topBar: @Composable (Boolean) -> Unit = {},
    floatingBar: @Composable () -> Unit = {},
    content: @Composable (Boolean) -> Unit = {}
) {
    val isVertical = isVerticalLayout()
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.layoutPadding(), contentAlignment = Alignment.BottomCenter) {
            Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.extraSmall)) {
                topBar(isVertical)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) { content(isVertical) }
            }
            floatingBar()
        }
    }
}

@Composable
fun TopBar(
    isVerticalLayout: Boolean,
    startAction: @Composable () -> Unit,
    topInfo: @Composable RowScope.() -> Unit,
    bottomInfo: @Composable RowScope.() -> Unit,
    endAction: @Composable () -> Unit
) {
    if (isVerticalLayout) {
        TopBar { startAction(); topInfo(); endAction() }
        TopBar { bottomInfo() }
    } else {
        TopBar { startAction(); topInfo(); bottomInfo(); endAction() }
    }
}

@Composable
fun elevationColor(elevation: Double): Color {
    val thresholds = LocalElevationThresholds.current
    return when {
        elevation < thresholds.low -> ElevationLowColor // soft red for low elevation
        elevation < thresholds.high -> MaterialTheme.colorScheme.primary // accent yellow for normal
        else -> ElevationHighColor // soft green for high elevation
    }
}

/** User-configurable elevation highlight thresholds (in degrees). */
data class ElevationThresholds(val low: Double = 15.0, val high: Double = 45.0)

/** Provided at the app root from settings; defaults keep the original 15°/45° behavior. */
val LocalElevationThresholds = compositionLocalOf { ElevationThresholds() }

/** Soft red used for elevations below the low threshold. */
val ElevationLowColor = Color(0xFFEF5350)

/** Soft green used for elevations above the high threshold. */
val ElevationHighColor = Color(0xFF66BB6A)

@Composable
fun OutlinedText(
    text: String,
    modifier: Modifier = Modifier,
    fillColor: Color = Color.Unspecified,
    outlineColor: Color,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    outlineDrawStyle: Stroke = Stroke(width = 8f),
) {
    Box(modifier = modifier) {
        Text(
            text = text,
            modifier = Modifier.semantics { hideFromAccessibility() },
            color = outlineColor,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = null,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = style.copy(shadow = null, drawStyle = outlineDrawStyle),
        )

        Text(
            text = text,
            color = fillColor,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = style,
        )
    }
}

// Formats a frequency in Hz as "MMM.KKK.HHH" (e.g. 145.825.000) or "---"
fun formatFrequency(frequencyHz: Long): String {
    if (frequencyHz <= 0) return "---"
    val mhz = frequencyHz / 1_000_000
    val khz = (frequencyHz % 1_000_000) / 1_000
    val hz = frequencyHz % 1_000
    return String.format(Locale.ENGLISH, "%d.%03d.%03d", mhz, khz, hz)
}
