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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
        SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).also { it.timeZone = timeZone }
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
                Icon(
                    painter = painterResource(R.drawable.ic_elevation),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${pass.maxElevation}°",
                    color = MaterialTheme.colorScheme.primary
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
                    Icon(
                        painter = painterResource(R.drawable.ic_altitude),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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
fun IconCard(action: () -> Unit, resId: Int, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = Modifier.size(48.dp)) {
        Box(
            modifier = Modifier
                .clickable(onClick = action)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { Icon(painter = painterResource(resId), contentDescription = null, modifier = modifier) }
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
fun SharedDialog(
    title: String, onCancel: () -> Unit, onAccept: () -> Unit, content: @Composable () -> Unit
) {
    DialogShell(title = title, titleFontSize = 16, onDismissRequest = onCancel) {
        content()
        Row(modifier = Modifier.padding(start = it, bottom = it, end = it)) {
            CardButton(onClick = onCancel, text = stringResource(R.string.btn_cancel))
            Spacer(modifier = Modifier.weight(1f))
            CardButton(onClick = onAccept, text = stringResource(R.string.btn_accept))
        }
    }
}

@Composable
fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    DialogShell(title = title, titleFontSize = 18, onDismissRequest = {}) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = it)
        )
        Row(modifier = Modifier.padding(start = it, bottom = it, end = it)) {
            Spacer(modifier = Modifier.weight(1f))
            CardButton(onClick = onDismiss, text = stringResource(R.string.btn_accept))
        }
    }
}

@Composable
private fun DialogShell(
    title: String,
    titleFontSize: Int,
    onDismissRequest: () -> Unit,
    content: @Composable (padding: androidx.compose.ui.unit.Dp) -> Unit
) {
    val padding = LocalSpacing.current.large
    Dialog(onDismissRequest = onDismissRequest) {
        ElevatedCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(padding)
            ) {
                Text(
                    text = title,
                    fontSize = titleFontSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = padding, top = padding, end = padding)
                )
                content(padding)
            }
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
