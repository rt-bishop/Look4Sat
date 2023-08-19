package com.rtbishop.look4sat.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.components.CardButton

@Preview(showBackground = true)
@Composable
private fun PositionDialogPreview() {
    MainTheme { PositionDialog(0.0, 0.0, {}) { _, _ -> } }
}

@Composable
fun PositionDialog(lat: Double, lon: Double, dismiss: () -> Unit, save: (Double, Double) -> Unit) {
    val latValue = rememberSaveable { mutableStateOf(lat.toString()) }
    val lonValue = rememberSaveable { mutableStateOf(lon.toString()) }
    val maxWidthModifier = Modifier.fillMaxWidth(1f)
    Dialog(onDismissRequest = { dismiss() }) {
        ElevatedCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = maxWidthModifier.padding(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.position_title), color = MaterialTheme.colorScheme.primary
                )
                Text(text = stringResource(id = R.string.position_lat_text))
                OutlinedTextField(value = latValue.value, onValueChange = { latValue.value = it })
                Text(text = stringResource(id = R.string.position_lon_text))
                OutlinedTextField(value = lonValue.value, onValueChange = { lonValue.value = it })
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    CardButton(onClick = { dismiss() }, text = stringResource(id = R.string.btn_cancel))
                    CardButton(
                        onClick = { saveValues(latValue.value, lonValue.value, dismiss, save) },
                        text = stringResource(id = R.string.btn_accept)
                    )
                }
            }
        }
    }
}

private fun saveValues(latValue: String, lonValue: String, dismiss: () -> Unit, save: (Double, Double) -> Unit) {
    val latitude = latValue.toDoubleOrNull() ?: 0.0
    val longitude = lonValue.toDoubleOrNull() ?: 0.0
    val newLatitude = if (latitude > 90) 90.0 else if (latitude < -90) -90.0 else latitude
    val newLongitude = if (longitude > 180) 180.0 else if (longitude < -180) -180.0 else longitude
    save(newLatitude, newLongitude)
    dismiss()
}

@Preview(showBackground = true)
@Composable
private fun LocatorDialogPreview() {
    MainTheme { LocatorDialog("IO91vl", {}) { } }
}

@Composable
fun LocatorDialog(qthLocator: String, dismiss: () -> Unit, save: (String) -> Unit) {
    val locator = rememberSaveable { mutableStateOf(qthLocator) }
    val maxWidthModifier = Modifier.fillMaxWidth(1f)
    Dialog(onDismissRequest = { dismiss() }) {
        ElevatedCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = maxWidthModifier.padding(12.dp)
            ) {
                Text(text = stringResource(id = R.string.locator_title), color = MaterialTheme.colorScheme.primary)
                Text(text = stringResource(id = R.string.locator_text))
                OutlinedTextField(value = locator.value, onValueChange = { locator.value = it })
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    CardButton(onClick = { dismiss() }, text = stringResource(id = R.string.btn_cancel))
                    CardButton(
                        onClick = {
                            save(locator.value)
                            dismiss()
                        }, text = stringResource(id = R.string.btn_accept)
                    )
                }
            }
        }
    }
}
