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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.core.domain.model.SatRadio
import com.rtbishop.look4sat.core.presentation.CardButton
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.infiniteMarquee
import java.util.Locale

@Composable
fun TransceiversPage(
    transceivers: List<SatRadio>,
    selectedUuid: String?,
    radioControl: RadioControlSubState,
    onAction: (RadarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (transceivers.isEmpty()) {
        EmptyTransceiversContent(modifier)
    } else {
        val listState = rememberLazyListState()
        // Snap the expanded item to the top of the visible area
        LaunchedEffect(selectedUuid) {
            if (selectedUuid != null) {
                val index = transceivers.indexOfFirst { it.uuid == selectedUuid }
                if (index >= 0) {
                    kotlinx.coroutines.delay(300)
                    listState.animateScrollToItem(index)
                }
            }
        }
        LazyColumn(modifier = modifier.fillMaxSize(), state = listState) {
            itemsIndexed(items = transceivers, key = { _, radio -> radio.uuid }) { _, radio ->
                val isExpanded = radio.uuid == selectedUuid
                TransceiverItem(
                    radio = radio,
                    isExpanded = isExpanded,
                    radioControl = radioControl,
                    onAction = onAction,
                    onToggle = { onAction(RadarAction.SelectTransmitter(radio.uuid)) }
                )
            }
        }
    }
}

@Composable
private fun EmptyTransceiversContent(modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
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
private fun TransceiverItem(
    radio: SatRadio,
    isExpanded: Boolean,
    radioControl: RadioControlSubState,
    onAction: (RadarAction) -> Unit,
    onToggle: () -> Unit
) {
    val bgColor = if (isExpanded) MaterialTheme.colorScheme.surfaceContainerHighest
    else MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle() }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Header: [arrow slot] [name - (mode)] [icon slot]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left icon slot — always reserved
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(if (isExpanded) 270f else 90f)
                    )
                }
                // Title with mode
                val title = if (radio.isInverted) "INV: ${radio.info}" else radio.info
                val mode = "${radio.downlinkMode ?: "--"}/${radio.uplinkMode ?: "--"}"
                val fullTitle = "$title ($mode)"
                Text(
                    text = fullTitle,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .infiniteMarquee()
                )
                // Right arrow slot — always reserved
                val iconTint = if (isExpanded) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_radios),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Frequency rows
            UnifiedFrequencyRow(
                label = "TX",
                frequencyLow = radio.uplinkLow,
                frequencyHigh = radio.uplinkHigh,
                isConnected = if (isExpanded) radioControl.txPanel.isConnected else null
            )
            UnifiedFrequencyRow(
                label = "RX",
                frequencyLow = radio.downlinkLow,
                frequencyHigh = radio.downlinkHigh,
                isConnected = if (isExpanded) radioControl.rxPanel.isConnected else null
            )
        }

        // Expanded CAT control area
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            ExpandedRadioControl(
                radio = radio,
                radioControl = radioControl,
                onAction = onAction
            )
        }

        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.background)
    }
}

@Composable
private fun UnifiedFrequencyRow(
    label: String,
    frequencyLow: Long?,
    frequencyHigh: Long?,
    isConnected: Boolean?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // TX:/RX: label
        Text(
            text = "$label:",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )
        // Low frequency
        FrequencyText(
            frequency = frequencyLow,
            modifier = Modifier.weight(1f)
        )
        // Dash — always centered
        Text(
            text = "–",
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(16.dp)
        )
        // High frequency
        FrequencyText(
            frequency = frequencyHigh,
            modifier = Modifier.weight(1f)
        )
        // Connection dot on the right
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (isConnected) {
                            true -> Color(0xFF4CAF50)
                            false -> Color(0xFFE57373)
                            null -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
            )
        }
    }
}

@Composable
private fun ExpandedRadioControl(
    radio: SatRadio,
    radioControl: RadioControlSubState,
    onAction: (RadarAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        // Frequency tuner
        if (radioControl.txBaseFrequencyHz != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
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
                        text = "${RadarViewModel.formatFrequency(radioControl.txBaseFrequencyHz)} MHz",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val upLow = radio.uplinkLow
                val upHigh = radio.uplinkHigh
                if (upLow != null && upHigh != null && upLow != upHigh) {
                    Text(
                        text = "(${RadarViewModel.formatFrequency(upLow)} – ${RadarViewModel.formatFrequency(upHigh)})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FREQ_ADJUSTMENTS.forEach { (delta, label) ->
                        CardButton(
                            onClick = { onAction(RadarAction.AdjustTxFrequency(delta)) },
                            text = label,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // CTCSS (only for FM uplink)
        if (radio.uplinkMode?.uppercase() == "FM") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "CTCSS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val chipModifier = Modifier.width(64.dp)
                    FilterChip(
                        selected = radioControl.ctcssTone == null,
                        onClick = { onAction(RadarAction.SetCtcssTone(null)) },
                        label = {
                            Text(
                                text = "Off",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = chipModifier
                    )
                    RadarViewModel.CTCSS_TONES.forEach { tone ->
                        FilterChip(
                            selected = radioControl.ctcssTone == tone,
                            onClick = { onAction(RadarAction.SetCtcssTone(tone)) },
                            label = {
                                Text(
                                    text = String.format(Locale.ENGLISH, "%.1f", tone),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            modifier = chipModifier
                        )
                    }
                }
            }
        }

        // Control buttons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!radioControl.txPanel.isConnected && !radioControl.rxPanel.isConnected) {
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
                text = if (radioControl.isTracking) "Stop" else "Track",
                modifier = Modifier.weight(1f)
            )
        }

        // Error
        radioControl.errorMessage?.let { msg ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(text = msg, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }
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
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

private val FREQ_ADJUSTMENTS =
    listOf(-10_000L to "-10k", -1_000L to "-1k", -100L to "-100", 100L to "+100", 1_000L to "+1k", 10_000L to "+10k")
