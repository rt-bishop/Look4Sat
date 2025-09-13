package com.rtbishop.look4sat.presentation.common

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.window.core.layout.WindowWidthSizeClass
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.predict.NearEarthObject
import com.rtbishop.look4sat.domain.predict.OrbitalData
import com.rtbishop.look4sat.domain.predict.OrbitalPass
import com.rtbishop.look4sat.presentation.LocalSpacing
import com.rtbishop.look4sat.presentation.MainTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

@Composable
@Preview(showBackground = true)
private fun TopBarPreview() = MainTheme {
    TopBar {
        IconCard(onClick = {}, iconId = R.drawable.ic_filter)
        TimerRow(timeString = "88:88:88", isTimeAos = true)
        IconCard(onClick = {}, iconId = R.drawable.ic_satellite)
    }
}

@Composable
fun TopBar(content: @Composable (RowScope.() -> Unit)) {
    Row(
        modifier = Modifier.height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
fun RowScope.TimerRow(timeString: String, isTimeAos: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    val aosColor = if (isTimeAos) colorScheme.primary else colorScheme.onSurface
    val losColor = if (!isTimeAos) colorScheme.primary else colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = "LOS", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = losColor)
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun NextPassRowPreview() = MainTheme {
    NextPassRow(pass = getDefaultPass())
}

@Composable
fun NextPassRow(pass: OrbitalPass, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.height(48.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy((-2).dp),
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(start = 6.dp, top = 1.dp, end = 6.dp, bottom = 0.dp)
        ) {
            val passSatId = stringResource(id = R.string.pass_satId, pass.catNum)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$passSatId - ",
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
                    painter = painterResource(id = R.drawable.ic_elevation),
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
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sdfTime.format(Date(pass.aosTime)),
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_altitude),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${pass.altitude} km",
                        fontSize = 15.sp
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.pass_aosLos,
                            pass.aosAzimuth.toInt(),
                            pass.losAzimuth.toInt()
                        ),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CardButton(onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
    ElevatedButton(
        onClick = { onClick() }, colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ), shape = MaterialTheme.shapes.small, modifier = modifier
    ) { Text(text = text, fontSize = 17.sp) }
}

@Composable
fun IconCard(onClick: () -> Unit, iconId: Int, modifier: Modifier = Modifier) {
    val clickableModifier = Modifier.clickable { onClick() }
    val iconRes = ImageVector.vectorResource(iconId)
    ElevatedCard(modifier = Modifier.size(48.dp)) {
        Box(modifier = clickableModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = iconRes, contentDescription = null, modifier = modifier)
        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = stringResource(R.string.empty_list_title), fontSize = 21.sp)
            Text(text = message, fontSize = 18.sp, textAlign = TextAlign.Center)
        }
    }
}

fun getDefaultPass(): OrbitalPass {
    val orbitalData = OrbitalData("||=<☉>=||", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0)
    val satellite = NearEarthObject(orbitalData)
    return OrbitalPass(0L, 0.0, Long.MAX_VALUE, 0.0, 0, 0.0, satellite, 0f)
}

@Composable
fun SharedDialog(
    title: String, onCancel: () -> Unit, onAccept: () -> Unit, content: @Composable () -> Unit
) {
    val padding = LocalSpacing.current.extraLarge
    Dialog(onDismissRequest = { onCancel() }) {
        ElevatedCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(padding)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = padding, top = padding, end = padding)
                )
                content()
                Row(modifier = Modifier.padding(start = padding, bottom = padding, end = padding)) {
                    CardButton(onClick = onCancel, text = stringResource(id = R.string.btn_cancel))
                    Spacer(modifier = Modifier.weight(1f))
                    CardButton(onClick = onAccept, text = stringResource(id = R.string.btn_accept))
                }
            }
        }
    }
}

@Composable
fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    val padding = LocalSpacing.current.extraLarge
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(padding)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = padding, top = padding, end = padding)
                )
                Text(
                    text = text,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = padding)
                )
                Row(modifier = Modifier.padding(start = padding, bottom = padding, end = padding)) {
                    Spacer(modifier = Modifier.weight(1f))
                    CardButton(onClick = onDismiss, text = stringResource(id = R.string.btn_understand))
                }
            }
        }
    }
}

@Composable
fun isVerticalLayout(): Boolean {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
}

@Composable
fun Modifier.infiniteMarquee(): Modifier {
    return basicMarquee(iterations = Int.MAX_VALUE, spacing = MarqueeSpacing(16.dp))
}

@Composable
fun Modifier.layoutPadding(): Modifier {
    val statusBarMod = this.statusBarsPadding()
    val spacing = LocalSpacing.current.small
    return when {
        isVerticalLayout() -> statusBarMod.padding(horizontal = spacing)
        else -> statusBarMod.padding(start = 0.dp, top = 0.dp, end = spacing, bottom = spacing)
    }
}
