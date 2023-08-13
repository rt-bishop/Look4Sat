package com.rtbishop.look4sat.presentation.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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

@Preview(showBackground = true)
@Composable
private fun TypeDialogPreview() {
    val types = listOf("All", "Amateur", "Geostationary", "Military", "Weather")
    MainTheme { TypesDialog(types, "All", {}) {} }
}

@Composable
fun TypesDialog(list: List<String>, selected: String, toggle: () -> Unit, click: (String) -> Unit) {
    val clickAction = { type: String ->
        click(type)
        toggle()
    }
    Dialog(onDismissRequest = { toggle() }) {
        ElevatedCard(modifier = Modifier.fillMaxHeight(0.75f)) {
            LazyColumn(
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(list) { index, type ->
                    Surface {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { clickAction(type) }) {
                            Text(
                                text = "$index).",
                                modifier = Modifier.padding(start = 12.dp, end = 6.dp),
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = type,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            RadioButton(selected = type == selected, onClick = { clickAction(type) })
                        }
                    }
                }
            }
        }
    }
}
