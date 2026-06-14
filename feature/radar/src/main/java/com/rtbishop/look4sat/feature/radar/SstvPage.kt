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

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rtbishop.look4sat.core.presentation.IconCard
import com.rtbishop.look4sat.core.presentation.OutlinedText
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.infiniteMarquee

@Composable
internal fun SstvPage(
    sstv: SstvSubState,
    dopplerFrequency: String?,
    onAction: (RadarAction) -> Unit,
    requestMicPermission: () -> Unit
) {
    if (!sstv.hasPermission) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "🎙️", fontSize = 48.sp)
                Text(
                    text = "Microphone access needed",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(onClick = requestMicPermission) {
                    Text("Grant permission")
                }
            }
        }
        return
    }

    // Mode selection dialog
    val showModeDialog = remember { mutableStateOf(false) }

    if (showModeDialog.value) {
        val allModes = remember(sstv.supportedModes) { listOf("Auto") + sstv.supportedModes }
        Dialog(onDismissRequest = { showModeDialog.value = false }) {
            ElevatedCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "SSTV Mode",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight(0.5f)
                            .background(MaterialTheme.colorScheme.background),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(allModes) { mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        onAction(RadarAction.SstvSelectMode(mode))
                                        showModeDialog.value = false
                                    }
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 16.dp)
                                )
                                RadioButton(
                                    selected = mode == sstv.selectedMode,
                                    onClick = null,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Main SSTV view — image fills entire area, controls overlay at bottom
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Decoded image or placeholder — centered in available space
        val frame = sstv.currentFrame
        val pixels = frame?.imagePixels
        if (pixels != null && frame.imageWidth > 0 && frame.imageHeight > 0) {
            val bitmap = remember(pixels, frame.imageWidth, frame.imageHeight) {
                Bitmap.createBitmap(
                    pixels,
                    frame.imageWidth,
                    frame.imageHeight,
                    Bitmap.Config.ARGB_8888
                ).asImageBitmap()
            }
            Image(
                bitmap = bitmap,
                contentDescription = "Decoded SSTV image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )
        } else {
            Text(
                text = if (sstv.status == SstvStatus.Recording) "Listening…" else "No signal",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        // Bottom control bar — dark, blends with the black canvas
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Doppler-corrected downlink frequency hint
            OutlinedText(
                text = dopplerFrequency?.let { "RX: $it Hz" } ?: "No transceiver selected",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fillColor = MaterialTheme.colorScheme.primary,
                outlineColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Mode button — opens dialog
                ElevatedCard(
                    modifier = Modifier.weight(1f).height(48.dp),
                    onClick = { showModeDialog.value = true },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Mode: ${sstv.selectedMode}",
                            fontSize = 14.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.infiniteMarquee()
                        )
                    }
                }
                // Reset button — clears decoder state and image
                IconCard(
                    action = { onAction(RadarAction.SstvReset) },
                    resId = R.drawable.ic_delete
                )
                // Save button — always visible, enabled when there are pixels
                IconCard(
                    action = { onAction(RadarAction.SstvSaveImage) },
                    resId = R.drawable.ic_save,
                    enabled = sstv.currentFrame?.imagePixels != null && !sstv.isSaving
                )
                // Record / Stop button
                val playAction = {
                    when (sstv.status) {
                        SstvStatus.Idle -> onAction(RadarAction.SstvStartRecording)
                        SstvStatus.Recording -> onAction(RadarAction.SstvStopRecording)
                    }
                }
                val playColors = CardDefaults.elevatedCardColors(
                    containerColor = if (sstv.status == SstvStatus.Recording) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                )
                val playIcon = if (sstv.status == SstvStatus.Recording) R.drawable.ic_pause else R.drawable.ic_play
                ElevatedCard(modifier = Modifier.size(48.dp), onClick = playAction, colors = playColors) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(painter = painterResource(playIcon), contentDescription = null)
                    }
                }
            }
        }
    }
}
