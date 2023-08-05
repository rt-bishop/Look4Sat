package com.rtbishop.look4sat.presentation.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.CardButton
import com.rtbishop.look4sat.presentation.MainTheme

@Preview(showBackground = true)
@Composable
private fun PositionDialogPreview() {
    MainTheme { PositionDialog(0.0, 0.0, {}) { _, _ -> } }
}

@Composable
fun PositionDialog(lat: Double, lon: Double, hide: () -> Unit, save: (Double, Double) -> Unit) {
    val latValue = rememberSaveable { mutableStateOf(lat.toString()) }
    val lonValue = rememberSaveable { mutableStateOf(lon.toString()) }
    Dialog(onDismissRequest = { hide() }) {
        ElevatedCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(1f)) {
                Text(
                    text = stringResource(id = R.string.position_title), color = MaterialTheme.colorScheme.primary
                )
                Text(text = stringResource(id = R.string.position_lat_text))
                OutlinedTextField(value = latValue.value, onValueChange = { latValue.value = it })
                Text(text = stringResource(id = R.string.position_lon_text))
                OutlinedTextField(value = lonValue.value, onValueChange = { lonValue.value = it })
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    CardButton(onClick = { hide() }, text = stringResource(id = R.string.btn_cancel))
                    CardButton(
                        onClick = { saveValues(latValue.value, lonValue.value, hide, save) },
                        text = stringResource(id = R.string.btn_accept)
                    )
                }
            }
        }
    }
}

private fun saveValues(lat: String, lon: String, hide: () -> Unit, save: (Double, Double) -> Unit) {
    val latValue = lat.toDoubleOrNull() ?: 0.0
    val lonValue = lon.toDoubleOrNull() ?: 0.0
    val newLat = if (latValue > 90) 90.0 else if (latValue < -90) -90.0 else latValue
    val newLon = if (lonValue > 180) 180.0 else if (lonValue < -180) -180.0 else lonValue
    save(newLat, newLon)
    hide()
}
