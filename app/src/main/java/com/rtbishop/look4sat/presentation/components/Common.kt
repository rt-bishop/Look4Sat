package com.rtbishop.look4sat.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.predict.NearEarthObject
import com.rtbishop.look4sat.domain.predict.OrbitalData
import com.rtbishop.look4sat.domain.predict.OrbitalPass
import com.rtbishop.look4sat.presentation.MainTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

@Composable
@Preview(showBackground = true)
private fun TimerRowPreview() = MainTheme {
    TimerRow {
        CardIcon(onClick = {}, iconId = R.drawable.ic_filter)
        TimerBar(timeString = "88:88:88", isTimeAos = true)
        CardIcon(onClick = {}, iconId = R.drawable.ic_satellite)
    }
}

@Composable
fun TimerRow(content: @Composable (RowScope.() -> Unit)) {
    Row(
        modifier = Modifier.height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

@Composable
fun RowScope.TimerBar(timeString: String, isTimeAos: Boolean) {
    val aosColor = if (isTimeAos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val losColor = if (!isTimeAos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    ElevatedCard(modifier = Modifier.weight(1f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = "AOS", fontSize = 16.sp, color = aosColor)
            Text(
                text = timeString,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = "LOS", fontSize = 16.sp, color = losColor)
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun NextPassRowPreview() = MainTheme {
    NextPassRow(pass = getDefaultPass())
}

@Composable
fun NextPassRow(pass: OrbitalPass) {
    ElevatedCard(modifier = Modifier.height(48.dp)) {
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
                        .padding(end = 6.dp),
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
                Text(
                    text = " ${pass.maxElevation}°",
                    textAlign = TextAlign.End,
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
                    textAlign = TextAlign.Start,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(id = R.string.pass_aosLos, pass.aosAzimuth.toInt(), pass.losAzimuth.toInt()),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1.5f)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_altitude),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " ${pass.altitude} km",
                        textAlign = TextAlign.Start,
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
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        ), shape = MaterialTheme.shapes.small, modifier = modifier
    ) { Text(text = text, fontSize = 17.sp) }
}

@Composable
fun CardIcon(onClick: () -> Unit, iconId: Int, description: String? = null) {
    val clickableModifier = Modifier.clickable { onClick() }
    ElevatedCard(modifier = Modifier.size(48.dp)) {
        Box(modifier = clickableModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = ImageVector.vectorResource(iconId), contentDescription = description)
        }
    }
}

@Composable
fun StatusIcon(iconResId: Int, isEnabled: Boolean = false, description: String? = null, onClick: (() -> Unit)? = null) {
    val cardColors = if (isEnabled) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    }
    val clickableModifier = if (onClick != null) Modifier.clickable { onClick.invoke() } else Modifier
    val iconTint = if (isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    ElevatedCard(modifier = Modifier.size(48.dp), colors = cardColors) {
        Box(modifier = clickableModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = ImageVector.vectorResource(iconResId), tint = iconTint, contentDescription = description)
        }
    }
}

@Composable
fun CardLoadingIndicator() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.size(80.dp))
    }
}

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun gotoUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

fun getDefaultPass(): OrbitalPass {
    val orbitalData = OrbitalData("Next Satellite", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0)
    val satellite = NearEarthObject(orbitalData)
    return OrbitalPass(0L, 0.0, 0L, 0.0, 0, 0.0, satellite, 0f)
}

//fun Modifier.onClick(onClick: () -> Unit): Modifier = composed {
//    clickable(remember { MutableInteractionSource() }, null) { onClick() }
//}
