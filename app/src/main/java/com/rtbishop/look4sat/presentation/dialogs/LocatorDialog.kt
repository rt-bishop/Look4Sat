package com.rtbishop.look4sat.presentation.dialogs

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
    MainTheme { LocatorDialog("IO91vl", {}) { } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocatorDialog(qthLocator: String, hide: () -> Unit, save: (String) -> Unit) {
    val locator = rememberSaveable { mutableStateOf(qthLocator) }
    Dialog(onDismissRequest = { hide() }) {
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
                OutlinedTextField(value = locator.value, onValueChange = { locator.value = it })
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CardButton(
                        onClick = { hide() }, text = stringResource(id = R.string.btn_cancel)
                    )
                    CardButton(
                        onClick = {
                            save(locator.value)
                            hide()
                        }, text = stringResource(id = R.string.btn_accept)
                    )
                }
            }
        }
    }
}
