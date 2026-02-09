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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.rtbishop.look4sat.presentation.common.EmptyListCard
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
    Column(modifier = Modifier.layoutPadding().keepScreenOn(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (isVerticalLayout()) {
            TopBar {
                IconCard(action = navigateUp, resId = R.drawable.ic_back)
                TimerRow(timeString = uiState.currentTime, isTimeAos = uiState.isCurrentTimeAos)
                IconCard(action = addToCalendar, resId = R.drawable.ic_calendar)
            }
            TopBar { NextPassRow(pass = upcomingPass) }
        } else {
            TopBar {
                IconCard(action = navigateUp, resId = R.drawable.ic_back)
                TimerRow(timeString = uiState.currentTime, isTimeAos = uiState.isCurrentTimeAos)
                NextPassRow(pass = upcomingPass, modifier = Modifier.weight(1f))
                IconCard(action = addToCalendar, resId = R.drawable.ic_calendar)
            }
        }
        if(isVerticalLayout()) {
            ElevatedCard(modifier = Modifier.weight(1f)) {
                Box(contentAlignment = Alignment.Center) {
                    if (uiState.orbitalPos == null) {
                        ElevatedCard(modifier = Modifier.fillMaxSize()) {
                            EmptyListCard(message = "")
                        }
                    }
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
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(text = """¯\_(ツ)_/¯""", fontSize = 32.sp)
                            Text(
                                text = stringResource(R.string.empty_list_message),
                                fontSize = 21.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.radar_no_data),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
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
                        if (uiState.orbitalPos == null) {
                            ElevatedCard(modifier = Modifier.fillMaxSize()) {
                                EmptyListCard(message = "")
                            }
                        }
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
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(text = """¯\_(ツ)_/¯""", fontSize = 32.sp)
                                Text(
                                    text = stringResource(R.string.empty_list_message),
                                    fontSize = 21.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.radar_no_data),
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
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
            text = stringResource(R.string.radar_eclipsed),
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
    val title = if (radio.isInverted) "INVERTED: ${radio.info}" else radio.info
    val fullTitle = "$title - (${radio.downlinkMode ?: "--"}/${radio.uplinkMode ?: "--"})"
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
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = fullTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .infiniteMarquee()
                )
                FrequencyRow(radio = radio, isDownlink = true)
                FrequencyRow(radio = radio, isDownlink = false)
            }
        }
    }
}

@Composable
private fun FrequencyRow(radio: SatRadio, isDownlink: Boolean, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        val rotateMod = Modifier.rotate(if (isDownlink) 90f else -90f)
        val weightMod = Modifier.weight(1f)
        val desc = if (isDownlink) stringResource(R.string.radar_downlink) else stringResource(R.string.radar_uplink)
        Text(
            text = if (isDownlink) "D:" else "U:",
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.size(width = 24.dp, height = 24.dp).semantics { contentDescription = desc }
        )
        FrequencyText(if (isDownlink) radio.downlinkLow else radio.uplinkLow, weightMod)
        Text(
            text = "-",
            textAlign = TextAlign.Center,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
        FrequencyText(if (isDownlink) radio.downlinkHigh else radio.uplinkHigh, weightMod)
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow),
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = null,
            modifier = rotateMod.size(width = 24.dp, height = 24.dp)
        )
    }
}

@Composable
private fun FrequencyText(frequency: Long?, modifier: Modifier = Modifier) {
    val noLinkText = stringResource(R.string.radar_no_link)
    val freqValue = frequency?.let { it / 1000000f }
    Text(
        text = freqValue?.let { stringResource(id = R.string.radar_link_low, it) } ?: noLinkText,
        textAlign = TextAlign.Center,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}
