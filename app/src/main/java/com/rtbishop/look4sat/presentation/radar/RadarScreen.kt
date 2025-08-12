package com.rtbishop.look4sat.presentation.radar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.SatRadio
import com.rtbishop.look4sat.domain.utility.toDegrees
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.Screen
import com.rtbishop.look4sat.presentation.common.IconCard
import com.rtbishop.look4sat.presentation.common.NextPassRow
import com.rtbishop.look4sat.presentation.common.TimerRow
import com.rtbishop.look4sat.presentation.common.TopBar
import com.rtbishop.look4sat.presentation.common.getDefaultPass
import com.rtbishop.look4sat.presentation.common.infiniteMarquee
import com.rtbishop.look4sat.presentation.common.isVerticalLayout
import com.rtbishop.look4sat.presentation.common.layoutPadding

fun NavGraphBuilder.radarDestination(navigateUp: () -> Unit) {
    val radarRoute = "${Screen.Radar.route}?catNum={catNum}&aosTime={aosTime}"
    val radarArgs = listOf(
        navArgument("catNum") { defaultValue = 0 },
        navArgument("aosTime") { defaultValue = 0L }
    )
    composable(radarRoute, radarArgs) {
        val viewModel = viewModel(RadarViewModel::class.java, factory = RadarViewModel.Factory)
        val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
        RadarScreen(uiState, navigateUp)
    }
}

@Composable
private fun RadarScreen(uiState: RadarState, navigateUp: () -> Unit) {
//    BluetoothCIV.init(LocalContext.current)
    val addToCalendar: () -> Unit = {
        uiState.currentPass?.let { pass ->
            uiState.sendAction(RadarAction.AddToCalendar(pass.name, pass.aosTime, pass.losTime))
        }
    }
    val upcomingPass = uiState.currentPass ?: getDefaultPass()
    if (upcomingPass.losTime < System.currentTimeMillis()) navigateUp()
    Column(modifier = Modifier.layoutPadding(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (isVerticalLayout()) {
            TopBar {
                IconCard(onClick = navigateUp, iconId = R.drawable.ic_back)
                TimerRow(timeString = uiState.currentTime, isTimeAos = uiState.isCurrentTimeAos)
                IconCard(onClick = addToCalendar, iconId = R.drawable.ic_calendar)
            }
            NextPassRow(pass = upcomingPass)
        } else {
            TopBar {
                IconCard(onClick = navigateUp, iconId = R.drawable.ic_back)
                TimerRow(timeString = uiState.currentTime, isTimeAos = uiState.isCurrentTimeAos)
                NextPassRow(pass = upcomingPass, modifier = Modifier.weight(1f))
                IconCard(onClick = addToCalendar, iconId = R.drawable.ic_calendar)
            }
        }
        if(isVerticalLayout()) {
            ElevatedCard(modifier = Modifier.weight(1f)) {
                Box(contentAlignment = Alignment.Center) {
                    uiState.orbitalPos?.let { position ->
                        RadarViewCompose(
                            item = position,
                            items = uiState.satTrack,
                            azimElev = uiState.orientationValues,
                            shouldShowSweep = uiState.shouldShowSweep,
                            shouldUseCompass = uiState.shouldUseCompass,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadarTextTop(
                                    position.azimuth,
                                    stringResource(R.string.radar_az_text),
                                    true
                                )
                                RadarTextTop(
                                    position.elevation,
                                    stringResource(R.string.radar_el_text),
                                    false
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadarTextBottom(
                                    position.altitude,
                                    stringResource(R.string.radar_alt_text),
                                    true
                                )
                                RadarTextBottom(
                                    position.distance,
                                    stringResource(R.string.radar_dist_text),
                                    false
                                )
                            }
                        }
                    }
                }
            }
            ElevatedCard(modifier = Modifier.weight(1f)) {
                if (uiState.transmitters.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_satellite),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "This satellite doesn't have any known transcievers...",
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TransmittersList(transmitters = uiState.transmitters)
                        if (uiState.orbitalPos?.eclipsed == true) {
                            EclipsedIndicator()
                        }
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ElevatedCard(modifier = Modifier.weight(1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        uiState.orbitalPos?.let { position ->
                            RadarViewCompose(
                                item = position,
                                items = uiState.satTrack,
                                azimElev = uiState.orientationValues,
                                shouldShowSweep = uiState.shouldShowSweep,
                                shouldUseCompass = false,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            Column(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadarTextTop(
                                        position.azimuth,
                                        stringResource(R.string.radar_az_text),
                                        true
                                    )
                                    RadarTextTop(
                                        position.elevation,
                                        stringResource(R.string.radar_el_text),
                                        false
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadarTextBottom(
                                        position.altitude,
                                        stringResource(R.string.radar_alt_text),
                                        true
                                    )
                                    RadarTextBottom(
                                        position.distance,
                                        stringResource(R.string.radar_dist_text),
                                        false
                                    )
                                }
                            }
                        }
                    }
                }
                ElevatedCard(modifier = Modifier.weight(1f)) {
                    if (uiState.transmitters.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_satellite),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "This satellite doesn't have any known transcievers...",
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            TransmittersList(transmitters = uiState.transmitters)
                            if (uiState.orbitalPos?.eclipsed == true) {
                                EclipsedIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarTextTop(value: Double, text: String, isLeft: Boolean) {
    val alignment = if (isLeft) Alignment.Start else Alignment.End
    val degValue = stringResource(R.string.radar_az_value, value.toDegrees())
    Column(horizontalAlignment = alignment) {
        Text(text = degValue, fontSize = 18.sp)
        Text(text = text, fontSize = 15.sp)
    }
}

@Composable
private fun RadarTextBottom(value: Double, text: String, isLeft: Boolean) {
    val alignment = if (isLeft) Alignment.Start else Alignment.End
    val degValue = stringResource(R.string.radar_alt_value, value)
    Column(horizontalAlignment = alignment) {
        Text(text = text, fontSize = 15.sp)
        Text(text = degValue, fontSize = 18.sp)
    }
}

@Composable
private fun TransmittersList(transmitters: List<SatRadio>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = transmitters, key = { item -> item.uuid }) { radio ->
            TransmitterItem(radio)
        }
    }
}

@Preview
@Composable
private fun TransmitterItemPreview() {
    val transmitter = SatRadio(
        "", "Extremely powerful transmitter", true, 10000000000L, 10000000000L,
        null, 10000000000L, 10000000000L, "FSK AX.100 Mode 5", true, 0
    )
    MainTheme { TransmitterItem(transmitter) }
}

@Composable
private fun EclipsedIndicator() {
    val bgColor = MaterialTheme.colorScheme.primary
    val bgShape = MaterialTheme.shapes.medium
    val infiniteTransition = rememberInfiniteTransition()
    val textAlpha = infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                delayMillis = 25,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(textAlpha.value)
            .border(width = 2.dp, color = bgColor, shape = bgShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Eclipsed!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .background(color = bgColor, shape = bgShape)
                .padding(12.dp)
        )
    }
}

@Composable
private fun TransmitterItem(radio: SatRadio) {
//    LaunchedEffect(radio) {
//        BluetoothCIV.updateOnce(radio)
//    }

    Surface(color = MaterialTheme.colorScheme.background,
//        modifier = Modifier.clickable(onClick = {
//            Log.d("BluetoothCivManager", radio.toString())
//            if(radio.uuid == BluetoothCIV.selected) BluetoothCIV.selected = "NONE" else
//            {
//                BluetoothCIV.connect(radio)
//                BluetoothCIV.updateOnce(radio)
//            }
//        })
        ) {
        Surface(modifier = Modifier.padding(bottom = 2.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow),
                        tint = Color.Green,
                        contentDescription = null, modifier = Modifier
                            .rotate(90f)
                            .weight(0.15f)
                    )
                    Text(
                        text = if (radio.isInverted) "INVERTED: ${radio.info} " else "${radio.info} ",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .infiniteMarquee()
                            .weight(0.7f)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow),
                        tint = Color.Red,
                        contentDescription = null, modifier = Modifier
                            .rotate(-90f)
                            .weight(0.15f)
                    )
                }
                FrequencyRow(satRadio = radio)
            }
        }
    }
}

@Composable
private fun FrequencyRow(satRadio: SatRadio) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        ModeText(satRadio.downlinkMode)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(0.35f)
        ) {
            FrequencyText(satRadio.downlinkHigh)
            FrequencyText(satRadio.downlinkLow)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(0.35f)
        ) {
            FrequencyText(satRadio.uplinkHigh)
            FrequencyText(satRadio.uplinkLow)
        }
        ModeText(satRadio.uplinkMode)
    }
}

@Composable
private fun RowScope.ModeText(mode: String?) {
    Text(
        text = mode ?: "- - : - -",
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(0.15f)
    )
}

@Composable
private fun FrequencyText(frequency: Long?) {
    val noLinkText = stringResource(R.string.radio_no_link)
    val freqValue = frequency?.let { it / 1000000f }
    val freqText = freqValue?.let { stringResource(id = R.string.radio_link_low, it) } ?: noLinkText
    Text(
        text = freqText,
        textAlign = TextAlign.Center,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}
