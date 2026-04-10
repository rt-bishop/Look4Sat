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
package com.rtbishop.look4sat.feature.radiocontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.presentation.CardButton
import com.rtbishop.look4sat.core.presentation.IconCard
import com.rtbishop.look4sat.core.presentation.NextPassRow
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.Screen
import com.rtbishop.look4sat.core.presentation.TimerRow
import com.rtbishop.look4sat.core.presentation.TopBar
import com.rtbishop.look4sat.core.presentation.getDefaultPass
import com.rtbishop.look4sat.core.presentation.isVerticalLayout
import com.rtbishop.look4sat.core.presentation.layoutPadding
import java.util.Locale

fun NavGraphBuilder.radioControlDestination(navigateUp: () -> Unit) {
    val route = "${Screen.RadioControl.route}?catNum={catNum}&aosTime={aosTime}"
    val args = listOf(
        navArgument("catNum") { defaultValue = 0 },
        navArgument("aosTime") { defaultValue = 0L }
    )
    composable(route, args) {
        val viewModel = viewModel(RadioControlViewModel::class.java, factory = RadioControlViewModel.Factory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        RadioControlScreen(uiState, navigateUp)
    }
}

@Composable
private fun RadioControlScreen(uiState: RadioControlState, navigateUp: () -> Unit) {
    Column(
        modifier = Modifier
            .layoutPadding()
            .keepScreenOn(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val currentPass = uiState.currentPass ?: getDefaultPass()
        val isVertical = isVerticalLayout()
        if (isVertical) {
            TopBar {
                IconCard(action = navigateUp, resId = R.drawable.ic_back)
                TimerRow(timeString = uiState.currentTime, isTimeAos = uiState.isCurrentTimeAos)
                IconCard(action = navigateUp, resId = R.drawable.ic_back)
            }
            TopBar { NextPassRow(pass = currentPass) }
        } else {
            TopBar {
                IconCard(action = navigateUp, resId = R.drawable.ic_back)
                TimerRow(timeString = uiState.currentTime, isTimeAos = uiState.isCurrentTimeAos)
                NextPassRow(pass = currentPass, modifier = Modifier.weight(1f))
                IconCard(action = navigateUp, resId = R.drawable.ic_back)
            }
        }

        RadioPanel(panel = uiState.txPanel)
        RadioPanel(panel = uiState.rxPanel)

        PositionRow(
            azimuth = uiState.azimuth,
            elevation = uiState.elevation,
            distance = uiState.distance
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { TransponderSelector(uiState) }

            val selectedTransponder = uiState.transponders.find {
                it.uuid == uiState.selectedTransponderUuid
            }
            if (selectedTransponder?.uplinkMode?.uppercase() == "FM") {
                item { CtcssSelector(uiState) }
            }

            if (uiState.txBaseFrequencyHz != null) {
                item { FrequencyTuner(uiState) }
            }

            uiState.errorMessage?.let { msg ->
                item {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        ControlButtons(uiState)
    }
}

@Composable
private fun RadioPanel(panel: RadioPanelState) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (panel.isConnected) Color(0xFF4CAF50) else Color(0xFFE57373))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = panel.label, fontSize = 14.sp)
            }
            Text(
                text = panel.frequencyDisplay,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = panel.mode ?: "--",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PositionRow(azimuth: String, elevation: String, distance: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Az", fontSize = 12.sp)
                Text(text = "$azimuth°", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "El", fontSize = 12.sp)
                Text(text = "$elevation°", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Dist", fontSize = 12.sp)
                Text(text = "$distance km", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TransponderSelector(uiState: RadioControlState) {
    if (uiState.transponders.isEmpty()) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "No transponders with uplink+downlink available",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        uiState.transponders.forEach { radio ->
            TransponderItem(
                radio = radio,
                isSelected = radio.uuid == uiState.selectedTransponderUuid,
                onSelect = { uiState.sendAction(RadioControlAction.SelectTransponder(radio.uuid)) }
            )
        }
    }
}

@Composable
private fun TransponderItem(radio: SatRadio, isSelected: Boolean, onSelect: () -> Unit) {
    val invLabel = if (radio.isInverted) " INV" else ""
    val modeLabel = "${radio.uplinkMode ?: "--"}/${radio.downlinkMode ?: "--"}$invLabel"
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = radio.info, modifier = Modifier.weight(1f))
            Text(text = modeLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CtcssSelector(uiState: RadioControlState) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(text = "CTCSS Tone", color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = uiState.ctcssTone == null,
                    onClick = { uiState.sendAction(RadioControlAction.SetCtcssTone(null)) },
                    label = { Text("Off") }
                )
                RadioControlViewModel.CTCSS_TONES.forEach { tone ->
                    FilterChip(
                        selected = uiState.ctcssTone == tone,
                        onClick = { uiState.sendAction(RadioControlAction.SetCtcssTone(tone)) },
                        label = { Text(String.format(Locale.ENGLISH, "%.1f", tone)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyTuner(uiState: RadioControlState) {
    val freq = uiState.txBaseFrequencyHz ?: return
    val transponder = uiState.transponders.find { it.uuid == uiState.selectedTransponderUuid }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(text = "TX Base Frequency", color = MaterialTheme.colorScheme.primary)
            if (transponder != null) {
                val upLow = transponder.uplinkLow
                val upHigh = transponder.uplinkHigh
                val dnLow = transponder.downlinkLow
                val dnHigh = transponder.downlinkHigh
                if (upLow != null && upHigh != null && upLow != upHigh) {
                    Text(
                        text = "UP: ${RadioControlViewModel.formatFrequency(upLow)} - ${RadioControlViewModel.formatFrequency(upHigh)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (dnLow != null && dnHigh != null && dnLow != dnHigh) {
                    Text(
                        text = "DN: ${RadioControlViewModel.formatFrequency(dnLow)} - ${RadioControlViewModel.formatFrequency(dnHigh)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${RadioControlViewModel.formatFrequency(freq)} MHz",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CardButton(
                    onClick = { uiState.sendAction(RadioControlAction.AdjustTxFrequency(-10_000)) },
                    text = "-10k",
                    modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = { uiState.sendAction(RadioControlAction.AdjustTxFrequency(-1_000)) },
                    text = "-1k",
                    modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = { uiState.sendAction(RadioControlAction.AdjustTxFrequency(-100)) },
                    text = "-100",
                    modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = { uiState.sendAction(RadioControlAction.AdjustTxFrequency(100)) },
                    text = "+100",
                    modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = { uiState.sendAction(RadioControlAction.AdjustTxFrequency(1_000)) },
                    text = "+1k",
                    modifier = Modifier.weight(1f)
                )
                CardButton(
                    onClick = { uiState.sendAction(RadioControlAction.AdjustTxFrequency(10_000)) },
                    text = "+10k",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ControlButtons(uiState: RadioControlState) {
    val isConnected = uiState.txPanel.isConnected || uiState.rxPanel.isConnected
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!isConnected) {
            CardButton(
                onClick = { uiState.sendAction(RadioControlAction.ConnectRadios) },
                text = "Connect",
                modifier = Modifier.weight(1f)
            )
        } else {
            CardButton(
                onClick = { uiState.sendAction(RadioControlAction.DisconnectRadios) },
                text = "Disconnect",
                modifier = Modifier.weight(1f)
            )
        }
        CardButton(
            onClick = { uiState.sendAction(RadioControlAction.ToggleTracking) },
            text = if (uiState.isTracking) "Stop Tracking" else "Start Tracking",
            modifier = Modifier.weight(1f)
        )
    }
}
