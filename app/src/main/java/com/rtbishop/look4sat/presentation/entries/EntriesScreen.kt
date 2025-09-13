package com.rtbishop.look4sat.presentation.entries

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.presentation.CardIcon
import com.rtbishop.look4sat.presentation.CardLoadingIndicator
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.dialogs.TypesDialog

@Composable
fun EntriesScreen(uiState: EntriesUiState, navToPasses: () -> Unit) {
    val showDialog = rememberSaveable { mutableStateOf(false) }
    val toggleDialog = { showDialog.value = showDialog.value.not() }
    if (showDialog.value) {
        TypesDialog(list = uiState.typesList, selected = uiState.currentType, toggleDialog) {
            uiState.takeAction(EntriesUiAction.SelectType(it))
        }
    }

    val unselectAll = { uiState.takeAction(EntriesUiAction.UnselectAll) }
    val selectAll = { uiState.takeAction(EntriesUiAction.SelectAll) }
    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TopBar(setQuery = { newQuery: String -> uiState.takeAction(EntriesUiAction.SearchFor(newQuery)) },
            saveSelection = {
                uiState.takeAction(EntriesUiAction.SaveSelection)
                navToPasses()
            })
        MiddleBar(uiState.currentType, { toggleDialog() }, { unselectAll() }, { selectAll() })
        ElevatedCard(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                CardLoadingIndicator()
            } else {
                EntriesCard(uiState.itemsList) { id, isTicked ->
                    uiState.takeAction(EntriesUiAction.SelectSingle(id, isTicked))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TopBarPreview() = MainTheme { TopBar({}, {}) }

@Composable
private fun TopBar(setQuery: (String) -> Unit, saveSelection: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(48.dp)) {
        SearchBar(setQuery = { setQuery(it) }, modifier = Modifier.weight(1f))
        SaveButton(saveSelection = { saveSelection() }, modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun SearchBar(setQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    val currentQuery = rememberSaveable { mutableStateOf("") }
    val setNewQuery = { newValue: String ->
        currentQuery.value = newValue
        setQuery(newValue)
    }
    ElevatedCard(modifier = modifier.height(48.dp)) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_search), contentDescription = null)
            BasicTextField(
                value = currentQuery.value,
                onValueChange = { setNewQuery(it) },
                singleLine = true,
                modifier = modifier.padding(start = 12.dp),
                textStyle = TextStyle(
                    fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    if (currentQuery.value.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.entries_search_hint),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface)
            )
            IconButton(onClick = { setNewQuery("") }) {
                Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = null)
            }
        }
    }
}

@Composable
private fun SaveButton(saveSelection: () -> Unit, modifier: Modifier = Modifier) {
    val clickableModifier = modifier.clickable { saveSelection() }
    ElevatedCard(
        modifier = Modifier.width(102.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Box(modifier = clickableModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(painter = painterResource(id = R.drawable.ic_done), contentDescription = null)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MiddleBarPreview() = MainTheme { MiddleBar("Amateur", {}, {}, {}) }

@Composable
private fun MiddleBar(type: String, navigate: () -> Unit, uncheck: () -> Unit, check: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(48.dp)) {
        EntryTypeCard(type = type, { navigate() }, modifier = Modifier.weight(1f))
        CardIcon(onClick = { uncheck() }, iconId = R.drawable.ic_checkbox_off)
        CardIcon(onClick = { check() }, iconId = R.drawable.ic_checkbox_on)
    }
}

@Composable
private fun EntryTypeCard(type: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(48.dp)
                .clickable { onClick() }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .padding(start = 6.dp)
            )
            Text(
                text = "Type: $type",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = modifier.padding(start = 12.dp, end = 6.dp, bottom = 2.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryPreview() {
    val satItem = SatItem(44444, "Ultra Super Mega long satellite name", true)
    MainTheme { Entry(item = satItem, onSelected = { _, _ -> run {} }, modifier = Modifier) }
}

@Composable
private fun Entry(item: SatItem, onSelected: (Int, Boolean) -> Unit, modifier: Modifier) {
    Surface(color = MaterialTheme.colorScheme.background,
        modifier = modifier.clickable { onSelected(item.catnum, item.isSelected) }) {
        Surface(modifier = Modifier.padding(bottom = 1.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 0.dp, top = 8.dp, end = 12.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Id:${item.catnum}  -  ",
                    modifier = Modifier.width(104.dp),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = item.name,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Checkbox(checked = item.isSelected, onCheckedChange = null, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EntriesPreview() {
    val entries = listOf(
        SatItem(8888, "Meteor", false),
        SatItem(44444, "ISS", true),
        SatItem(88888, "Starlink", false)
    )
    MainTheme { EntriesCard(entries) { _, _ -> run {} } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntriesCard(items: List<SatItem>, onSelected: (Int, Boolean) -> Unit) {
    LazyColumn {
        items(items = items, key = { item -> item.catnum }) { entry ->
            Entry(entry, onSelected, Modifier.animateItemPlacement())
        }
    }
}
