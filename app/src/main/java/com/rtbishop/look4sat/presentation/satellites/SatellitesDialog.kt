package com.rtbishop.look4sat.presentation.satellites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.common.SharedDialog

@Preview(showBackground = true)
@Composable
private fun MultiTypesDialogPreview() {
    val types = listOf("Amateur", "Geostationary", "Military", "Weather")
    MainTheme { MultiTypesDialog(types, emptyList(), {}) {} }
}

@Composable
fun MultiTypesDialog(
    allTypes: List<String>, types: List<String>, cancel: () -> Unit, accept: (List<String>) -> Unit
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(types) } }
    val select = { type: String ->
        if (selected.contains(type)) selected.remove(type) else selected.add(type)
    }
    val onAccept = { accept(selected.toList()) }
    SharedDialog(title = "Select category", onCancel = cancel, onAccept = onAccept) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(240.dp),
            modifier = Modifier
                .fillMaxHeight(0.69f)
                .background(MaterialTheme.colorScheme.background),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            itemsIndexed(allTypes) { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { select(item) }
                ) {
                    Text(
                        text = "${index + 1}).",
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Checkbox(
                        checked = selected.contains(item),
                        onCheckedChange = null,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
