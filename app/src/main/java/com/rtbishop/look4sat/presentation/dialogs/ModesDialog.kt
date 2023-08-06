package com.rtbishop.look4sat.presentation.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rtbishop.look4sat.presentation.MainTheme

private val allModes = listOf(
    "AFSK", "AFSK S-Net", "AFSK SALSAT", "AHRPT", "AM", "APT", "BPSK", "BPSK PMT-A3",
    "CERTO", "CW", "DQPSK", "DSTAR", "DUV", "FFSK", "FM", "FMN", "FSK", "FSK AX.100 Mode 5",
    "FSK AX.100 Mode 6", "FSK AX.25 G3RUH", "GFSK", "GFSK Rktr", "GMSK", "HRPT", "LoRa",
    "LRPT", "LSB", "MFSK", "MSK", "MSK AX.100 Mode 5", "MSK AX.100 Mode 6", "OFDM", "OQPSK",
    "PSK", "PSK31", "PSK63", "QPSK", "QPSK31", "QPSK63", "SSTV", "USB", "WSJT"
)

@Preview(showBackground = true)
@Composable
private fun ModesDialogPreview() {
    MainTheme { ModesDialog(allModes, listOf("AFSK", "AFSK S-Net"), {}) {} }
}

@Composable
fun ModesDialog(list: List<String>, selected: List<String>, toggle: () -> Unit, click: (String) -> Unit) {
    Dialog(onDismissRequest = { toggle() }) {
        ElevatedCard(modifier = Modifier.fillMaxHeight(0.9f)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(list) { mode ->
                    Surface {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .fillMaxWidth()
                                .clickable {
                                    click(mode)
                                    toggle()
                                }) {
                            Text(
                                text = mode,
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 6.dp)
                                    .weight(1f),
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Checkbox(
                                checked = selected.contains(mode),
                                onCheckedChange = null,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
