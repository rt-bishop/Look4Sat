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
package com.rtbishop.look4sat.feature.radar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
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
import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.domain.predict.OrbitalPos
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.core.domain.utility.toDegrees
import com.rtbishop.look4sat.core.presentation.EmptyListCard
import com.rtbishop.look4sat.core.presentation.IconCard
import com.rtbishop.look4sat.core.presentation.MainTheme
import com.rtbishop.look4sat.core.presentation.NextPassRow
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.TimerRow
import com.rtbishop.look4sat.core.presentation.TopBar
import com.rtbishop.look4sat.core.presentation.getDefaultPass
import com.rtbishop.look4sat.core.presentation.infiniteMarquee
import com.rtbishop.look4sat.core.presentation.isVerticalLayout
import com.rtbishop.look4sat.core.presentation.layoutPadding

@Composable
fun RadarDestination(
    catNum: Int = 0,
    aosTime: Long = 0L,
    navigateUp: () -> Unit,
    navigateToRadioControl: (Int, Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val container = (context.applicationContext as IContainerProvider).getMainContainer()
    val viewModel = viewModel(
        modelClass = RadarViewModel::class.java,
        key = "$catNum-$aosTime",
        factory = RadarViewModel.factory(catNum, aosTime, container)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RadarScreen(uiState, viewModel::onAction, navigateUp, navigateToRadioControl)
}

@Composable
private fun RadarScreen(
    uiState: RadarState,
    onAction: (RadarAction) -> Unit,
    navigateUp: () -> Unit,
    navigateToRadioControl: (Int, Long) -> Unit
) {
    val upcomingPass = uiState.currentPass ?: getDefaultPass()
    LaunchedEffect(uiState.isLos) { if (uiState.isLos) navigateUp() }

    val addToCalendar: () -> Unit = {
        uiState.currentPass?.let { pass ->
            onAction(RadarAction.AddToCalendar(pass.name, pass.aosTime, pass.losTime))
        }
    }
    val openRadioControl: () -> Unit = {
        uiState.currentPass?.let { pass ->
            navigateToRadioControl(pass.catNum, pass.aosTime)
        }
    }
    Column(
        modifier = Modifier
            .layoutPadding()
            .keepScreenOn(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val isVertical = isVerticalLayout()
        if (isVertical) {
            TopBar {
                IconCard(action = addToCalendar, resId = R.drawable.ic_calendar)
                TimerRow(timeString = uiState.currentTime, isTimeAos = uiState.isTimeAos)
                IconCard(action = openRadioControl, resId = R.drawable.ic_radios)
            }
            TopBar { NextPassRow(pass = upcomingPass, isUtc = uiState.isUtc) }
        } else {
            TopBar {
                IconCard(action = addToCalendar, resId = R.drawable.ic_calendar)
                TimerRow(timeString = uiState.currentTime, isTimeAos = uiState.isTimeAos)
                NextPassRow(pass = upcomingPass, modifier = Modifier.weight(1f), isUtc = uiState.isUtc)
                IconCard(action = openRadioControl, resId = R.drawable.ic_radios)
            }
        }
        if (isVertical) {
            RadarCard(uiState, Modifier.weight(1f))
            TransmittersCard(uiState.transmitters, uiState.selectedTransmitterUuid, onAction, Modifier.weight(1f))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RadarCard(uiState, Modifier.weight(1f))
                TransmittersCard(uiState.transmitters, uiState.selectedTransmitterUuid, onAction, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RadarCard(uiState: RadarState, modifier: Modifier = Modifier) {
    val satellitePos = uiState.orbitalPos
    val borderModifier = if (satellitePos?.aboveHorizon == true && satellitePos.eclipsed) {
        val infiniteTransition = rememberInfiniteTransition(label = "eclipsedBorder")
        val borderAlpha by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, delayMillis = 25, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eclipsedBorderAlpha"
        )
        Modifier.border(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
            shape = MaterialTheme.shapes.medium
        )
    } else {
        Modifier
    }
    ElevatedCard(modifier = modifier.then(borderModifier)) {
        Box(contentAlignment = Alignment.Center) {
            val position = uiState.orbitalPos
            if (position == null) {
                ElevatedCard(modifier = Modifier.fillMaxSize()) {
                    EmptyListCard(message = "")
                }
            } else {
                RadarViewCompose(
                    item = position,
                    items = uiState.satTrack,
                    azimElev = uiState.orientationValues,
                    shouldShowSweep = uiState.shouldShowSweep,
                    shouldUseCompass = uiState.shouldUseCompass,
                    modifier = Modifier.align(Alignment.Center),
                    sunPosition = uiState.sunPosition,
                    moonPosition = uiState.moonPosition,
                )
                PositionOverlay(position)
            }
        }
    }
}

@Composable
private fun PositionOverlay(position: OrbitalPos) {
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
            RadarLabel(
                value = stringResource(R.string.radar_az_value, position.azimuth.toDegrees()),
                label = stringResource(R.string.radar_az_text),
                alignment = Alignment.Start,
                labelFirst = false
            )
            RadarLabel(
                value = stringResource(R.string.radar_az_value, position.elevation.toDegrees()),
                label = stringResource(R.string.radar_el_text),
                alignment = Alignment.End,
                labelFirst = false
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadarLabel(
                value = stringResource(R.string.radar_alt_value, position.altitude),
                label = stringResource(R.string.radar_alt_text),
                alignment = Alignment.Start,
                labelFirst = true
            )
            RadarLabel(
                value = stringResource(R.string.radar_alt_value, position.distance),
                label = stringResource(R.string.radar_dist_text),
                alignment = Alignment.End,
                labelFirst = true
            )
        }
    }
}

@Composable
private fun RadarLabel(
    value: String,
    label: String,
    alignment: Alignment.Horizontal,
    labelFirst: Boolean
) {
    Column(horizontalAlignment = alignment) {
        if (labelFirst) {
            Text(text = label, fontSize = 15.sp)
            Text(text = value, fontSize = 18.sp)
        } else {
            Text(text = value, fontSize = 18.sp)
            Text(text = label, fontSize = 15.sp)
        }
    }
}

@Composable
private fun TransmittersCard(
    transmitters: List<SatRadio>,
    selectedUuid: String?,
    onAction: (RadarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        if (transmitters.isEmpty()) {
            EmptyTransmittersContent()
        } else {
            TransmittersList(
                transmitters = transmitters,
                selectedUuid = selectedUuid,
                onSelect = { uuid ->
                    if (selectedUuid != null) {
                        onAction(RadarAction.SelectTransmitter(uuid))
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyTransmittersContent() {
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
}

@Composable
private fun TransmittersList(
    transmitters: List<SatRadio>,
    selectedUuid: String?,
    onSelect: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = transmitters, key = { it.uuid }) { radio ->
            TransmitterItem(
                radio = radio,
                isClickable = selectedUuid != null,
                isSelected = radio.uuid == selectedUuid,
                onClick = { onSelect(radio.uuid) }
            )
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
    MainTheme { TransmitterItem(transmitter, isClickable = true, isSelected = true, onClick = {}) }
}

@Composable
private fun TransmitterItem(
    radio: SatRadio,
    isClickable: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val title = if (radio.isInverted) "INVERTED: ${radio.info}" else radio.info
    val fullTitle = "$title - (${radio.downlinkMode ?: "--"}/${radio.uplinkMode ?: "--"})"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isClickable) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Box {
                Text(
                    text = fullTitle,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .infiniteMarquee()
                )
                if (isSelected) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_radios),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)
                    )
                }
            }
            FrequencyRow(radio = radio, isDownlink = true)
            FrequencyRow(radio = radio, isDownlink = false)
        }
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.background)
    }
}

@Composable
private fun FrequencyRow(radio: SatRadio, isDownlink: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        val desc = if (isDownlink) stringResource(R.string.radar_downlink)
        else stringResource(R.string.radar_uplink)
        Text(
            text = if (isDownlink) "D:" else "U:",
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(24.dp)
                .semantics { contentDescription = desc }
        )
        FrequencyText(
            frequency = if (isDownlink) radio.downlinkLow else radio.uplinkLow,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "-",
            textAlign = TextAlign.Center,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        FrequencyText(
            frequency = if (isDownlink) radio.downlinkHigh else radio.uplinkHigh,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow),
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = null,
            modifier = Modifier
                .rotate(if (isDownlink) 90f else -90f)
                .size(24.dp)
        )
    }
}

@Composable
private fun FrequencyText(frequency: Long?, modifier: Modifier = Modifier) {
    val text = frequency?.let {
        stringResource(id = R.string.radar_link_low, it / 1000000f)
    } ?: stringResource(R.string.radar_no_link)
    Text(
        text = text,
        textAlign = TextAlign.Center,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}
