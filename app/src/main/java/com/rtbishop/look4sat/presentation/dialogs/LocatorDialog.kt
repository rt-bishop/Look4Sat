package com.rtbishop.look4sat.presentation.dialogs

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
import com.rtbishop.look4sat.presentation.CardButton
import com.rtbishop.look4sat.presentation.MainTheme

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
