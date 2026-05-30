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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.presentation.CardButton
import java.util.Locale

@Composable
fun RadioControlPage(
    radioState: RadioControlSubState,
    onAction: (RadarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTransponder = radioState.transponders.find {
        it.uuid == radioState.selectedTransponderUuid
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Transponder selector (dropdown)
        TransponderDropdown(
            transponders = radioState.transponders,
            selectedTransponder = selectedTransponder,
            onAction = onAction
        )

        // Active transponder status (TX/RX lines)
        if (selectedTransponder != null) {
            ActiveTransponderStatus(
                transponder = selectedTransponder,
                txPanel = radioState.txPanel,
                rxPanel = radioState.rxPanel
            )
        }

        // Frequency tuner + CTCSS
        if (radioState.txBaseFrequencyHz != null) {
            FrequencyTunerCard(
                txBaseFrequencyHz = radioState.txBaseFrequencyHz,
                selectedTransponder = selectedTransponder,
                ctcssTone = radioState.ctcssTone,
                showCtcss = selectedTransponder?.uplinkMode?.uppercase() == "FM",
                onAction = onAction
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Error message
        radioState.errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Control buttons
        ControlButtons(
            isConnected = radioState.txPanel.isConnected || radioState.rxPanel.isConnected,
            isTracking = radioState.isTracking,
            onAction = onAction
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransponderDropdown(
    transponders: List<SatRadio>,
    selectedTransponder: SatRadio?,
    onAction: (RadarAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedTransponder?.info ?: "No transponders available",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            transponders.forEach { radio ->
                val invLabel = if (radio.isInverted) " INV" else ""
                val modeLabel = "${radio.uplinkMode ?: "--"}/${radio.downlinkMode ?: "--"}$invLabel"
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = radio.info,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = modeLabel,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onAction(RadarAction.SelectTransponder(radio.uuid))
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ActiveTransponderStatus(
    transponder: SatRadio,
    txPanel: RadioPanelState,
    rxPanel: RadioPanelState
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // TX line (uplink — on top)
            FrequencyStatusRow(
                label = "TX",
                frequency = txPanel.frequencyHz ?: transponder.uplinkLow,
                isConnected = txPanel.isConnected,
                mode = txPanel.mode
            )
            // RX line (downlink — below)
            FrequencyStatusRow(
                label = "RX",
                frequency = rxPanel.frequencyHz ?: transponder.downlinkLow,
                isConnected = rxPanel.isConnected,
                mode = rxPanel.mode
            )
        }
    }
}

@Composable
private fun FrequencyStatusRow(
    label: String,
    frequency: Long?,
    isConnected: Boolean,
    mode: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$label:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = frequency?.let { RadarViewModel.formatFrequency(it) } ?: "—",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        if (mode != null) {
            Text(
                text = mode,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFFE57373))
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrequencyTunerCard(
    txBaseFrequencyHz: Long,
    selectedTransponder: SatRadio?,
    ctcssTone: Double?,
    showCtcss: Boolean,
    onAction: (RadarAction) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // TX Base Frequency label + value on one line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "TX Base: ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${RadarViewModel.formatFrequency(txBaseFrequencyHz)} MHz",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // Show band range hint if it's a wide transponder
            if (selectedTransponder != null) {
                val upLow = selectedTransponder.uplinkLow
                val upHigh = selectedTransponder.uplinkHigh
                if (upLow != null && upHigh != null && upLow != upHigh) {
                    Text(
                        text = "(${RadarViewModel.formatFrequency(upLow)} – ${RadarViewModel.formatFrequency(upHigh)})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Adjustment buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FREQ_ADJUSTMENTS.forEach { (delta, label) ->
                    CardButton(
                        onClick = { onAction(RadarAction.AdjustTxFrequency(delta)) },
                        text = label,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // CTCSS inline (only for FM)
            if (showCtcss) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "CTCSS",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = ctcssTone == null,
                        onClick = { onAction(RadarAction.SetCtcssTone(null)) },
                        label = { Text("Off", fontSize = 12.sp) }
                    )
                    RadarViewModel.CTCSS_TONES.forEach { tone ->
                        FilterChip(
                            selected = ctcssTone == tone,
                            onClick = { onAction(RadarAction.SetCtcssTone(tone)) },
                            label = { Text(String.format(Locale.ENGLISH, "%.1f", tone), fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ControlButtons(
    isConnected: Boolean,
    isTracking: Boolean,
    onAction: (RadarAction) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!isConnected) {
            CardButton(
                onClick = { onAction(RadarAction.ConnectRadios) },
                text = "Connect",
                modifier = Modifier.weight(1f)
            )
        } else {
            CardButton(
                onClick = { onAction(RadarAction.DisconnectRadios) },
                text = "Disconnect",
                modifier = Modifier.weight(1f)
            )
        }
        CardButton(
            onClick = { onAction(RadarAction.ToggleTracking) },
            text = if (isTracking) "Stop" else "Track",
            modifier = Modifier.weight(1f)
        )
    }
}

private val FREQ_ADJUSTMENTS =
    listOf(-10_000L to "-10k", -1_000L to "-1k", -100L to "-100", 100L to "+100", 1_000L to "+1k", 10_000L to "+10k")
