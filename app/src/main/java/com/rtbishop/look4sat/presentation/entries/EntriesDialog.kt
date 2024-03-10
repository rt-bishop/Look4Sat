package com.rtbishop.look4sat.presentation.entries

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rtbishop.look4sat.presentation.MainTheme

@Preview(showBackground = true)
@Composable
private fun TypeDialogPreview() {
    val types = listOf("All", "Amateur", "Geostationary", "Military", "Weather")
    MainTheme { TypesDialog(types, "All", {}) {} }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypesDialog(items: List<String>, selected: String, dismiss: () -> Unit, select: (String) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { dismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.80f)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            item { HorizontalDivider(thickness = 0.dp, color = MaterialTheme.colorScheme.surface) }
            itemsIndexed(items) { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { select(item) }) {
                    Text(
                        text = "$index).",
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp),
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = item,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    RadioButton(
                        selected = item == selected,
                        onClick = null,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 12.dp, bottom = 8.dp)
                    )
                }
            }
            item { HorizontalDivider(thickness = 24.dp, color = MaterialTheme.colorScheme.surface) }
        }
    }
}
