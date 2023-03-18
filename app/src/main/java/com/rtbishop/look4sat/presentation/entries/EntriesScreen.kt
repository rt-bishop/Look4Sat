package com.rtbishop.look4sat.presentation.entries

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.model.DataState
import com.rtbishop.look4sat.model.SatItem
import com.rtbishop.look4sat.presentation.CardLoadingIndicator
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.dialogs.TypesDialog
import com.rtbishop.look4sat.presentation.onClick

@Composable
fun EntriesScreen(navToPasses: () -> Unit, viewModel: EntriesViewModel = hiltViewModel()) {
    val state = viewModel.satData.collectAsState(initial = DataState.Loading)

    val showDialog = rememberSaveable { mutableStateOf(false) }
    val toggleDialog = { showDialog.value = showDialog.value.not() }
    if (showDialog.value) {
        TypesDialog(list = viewModel.satTypes, selected = viewModel.getSatType(), toggleDialog) {
            viewModel.setSatType(it)
        }
    }

    val unselectAll = { viewModel.selectCurrentItems(false) }
    val selectAll = { viewModel.selectCurrentItems(true) }
    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TopBar(setQuery = { newQuery: String -> viewModel.setQuery(newQuery) }, saveSelection = {
            viewModel.saveSelection()
            navToPasses()
        })
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(48.dp)) {
            EntryTypeCard(
                type = viewModel.getSatType(), { toggleDialog() }, modifier = Modifier.weight(1f)
            )
            SelectionCard(unselectAll = { unselectAll() }, selectAll = { selectAll() })
        }
        EntriesCard(state = state.value) { list, value -> viewModel.updateSelection(list, value) }
    }
}

@Preview(showBackground = true)
@Composable
private fun TopBarPreview() = MainTheme { TopBar({}, {}) }

@Preview(showBackground = true)
@Composable
private fun MiddleBarPreview() = MainTheme { MiddleBar({}, {}) }

@Preview(showBackground = true)
@Composable
private fun EntryPreview() {
    val satItem = SatItem(45555, "SatName", emptyList(), true)
    MainTheme { Entry(item = satItem, onSelected = { _, _ -> run {} }, modifier = Modifier) }
}

@Preview(showBackground = true)
@Composable
private fun EntriesPreview() {
    MainTheme { EntriesCard(DataState.Loading) { _, _ -> run {} } }
}

@Composable
private fun TopBar(setQuery: (String) -> Unit, saveSelection: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(56.dp)) {
        SearchBar(setQuery = { setQuery(it) }, modifier = Modifier.weight(1f))
        SaveButton(saveSelection = { saveSelection() }, modifier = Modifier.height(56.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(setQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    val currentQuery = rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = currentQuery.value,
        onValueChange = { newValue ->
            currentQuery.value = newValue
            setQuery(newValue)
        },
        maxLines = 1,
//            label = { Text(text = stringResource(id = R.string.entries_search_hint)) },
        placeholder = { Text(text = stringResource(id = R.string.entries_search_hint)) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = null
            )
        },
        trailingIcon = {
            Icon(painter = painterResource(id = R.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier.onClick {
                    currentQuery.value = ""
                    setQuery("")
                })
        },
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    )
}

@Composable
private fun SaveButton(saveSelection: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedButton(
        onClick = { saveSelection() }, colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ), shape = MaterialTheme.shapes.medium, modifier = modifier.width(96.dp)
    ) { Icon(painter = painterResource(id = R.drawable.ic_checkmark), contentDescription = null) }
}

@Composable
private fun MiddleBar(unselectAll: () -> Unit, selectAll: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(48.dp)) {
        EntryTypeCard(type = "Amateur", {}, modifier = Modifier.weight(1f))
        SelectionCard(unselectAll = { unselectAll() }, selectAll = { selectAll() })
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
                painter = painterResource(id = R.drawable.ic_next),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .padding(start = 6.dp)
            )
            Text(
                text = "Type: $type",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = modifier.padding(start = 16.dp, end = 6.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun SelectionCard(unselectAll: () -> Unit, selectAll: () -> Unit) {
    ElevatedCard {
        Row(modifier = Modifier.width(96.dp)) {
            val unselectIcon = painterResource(id = R.drawable.ic_checkbox_off)
            val selectIcon = painterResource(id = R.drawable.ic_checkbox_on)
            val iconColor = MaterialTheme.colorScheme.onSurface
            IconButton(onClick = { unselectAll() }) {
                Icon(painter = unselectIcon, contentDescription = null, tint = iconColor)
            }
            IconButton(onClick = { selectAll() }) {
                Icon(painter = selectIcon, contentDescription = null, tint = iconColor)
            }
        }
    }
}

@Composable
private fun Entry(item: SatItem, onSelected: (List<Int>, Boolean) -> Unit, modifier: Modifier) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
        Surface(modifier = Modifier.padding(bottom = 1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 0.dp, top = 8.dp, end = 12.dp, bottom = 8.dp)
                    .onClick { onSelected(listOf(item.catnum), item.isSelected.not()) }) {
                Text(
                    text = "Id:${item.catnum}  -  ",
                    modifier = Modifier.width(104.dp),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = item.name,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Checkbox(checked = item.isSelected, onCheckedChange = null)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntriesCard(state: DataState<List<SatItem>>, onSelected: (List<Int>, Boolean) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is DataState.Success -> {
                LazyColumn {
                    items(items = state.data, key = { item -> item.catnum }) { entry ->
                        Entry(entry, onSelected, Modifier.animateItemPlacement())
                    }
                }
            }
            else -> CardLoadingIndicator()
        }
    }
}
