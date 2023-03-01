package com.rtbishop.look4sat.presentation.settingsScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
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
private fun LocatorDialogPreview() {
    MainTheme { LocatorDialog(8, 16.0, {}) { _, _ -> } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocatorDialog(
    hours: Int, elevation: Double, toggle: () -> Unit, save: (Int, Double) -> Unit
) {
    val hoursValue = rememberSaveable { mutableStateOf(hours) }
    val elevValue = rememberSaveable { mutableStateOf(elevation) }
    Dialog(onDismissRequest = { toggle() }) {
        ElevatedCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(1f)
            ) {
                Text(
                    text = stringResource(id = R.string.locator_title),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = stringResource(id = R.string.locator_text))
                OutlinedTextField(value = hoursValue.value.toString(), onValueChange = { newValue ->
                    val hoursAhead = try {
                        newValue.toInt()
                    } catch (exception: Exception) {
                        12
                    }
                    hoursValue.value = hoursAhead
                })
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CardButton(onClick = { toggle() }, text = "Cancel")
                    CardButton(
                        onClick = {
                            save(hoursValue.value, elevValue.value)
                            toggle()
                        }, text = "Accept"
                    )
                }
            }
        }
    }
}
