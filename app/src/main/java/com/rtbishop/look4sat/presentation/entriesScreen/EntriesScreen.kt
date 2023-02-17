package com.rtbishop.look4sat.presentation.entriesScreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.onClick

@Composable
fun EntriesScreen(viewModel: EntriesViewModel = hiltViewModel()) {
    val dataState = viewModel.satData.observeAsState(DataState.Loading)
    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Card { SearchBar(setQuery = { newQuery -> viewModel.setQuery(newQuery) }) }
        EntryType(entryType = "All")
        Card(modifier = Modifier.fillMaxWidth()) {
            LoadingOrList(dataState.value) { list, value -> viewModel.updateSelection(list, value) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryTypeBarPreview() {
    MainTheme { EntryType(entryType = "Amateur") }
}

@Composable
private fun EntryType(entryType: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier.height(48.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = modifier.size(32.dp)
            )
            Text(
                text = "Selected type: $entryType",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = modifier.padding(start = 6.dp, end = 6.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun LoadingOrList(
    state: DataState<List<SatItem>>, onSelected: (List<Int>, Boolean) -> Unit
) {
    when (state) {
        is DataState.Loading -> {
            LoadingProgress()
        }
        is DataState.Success -> {
            EntriesList(entries = state.data) { list, value -> onSelected(list, value) }
        }
        else -> {
            LoadingProgress()
        }
    }
}

@Composable
private fun LoadingProgress() {
    Row(
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) { CircularProgressIndicator(modifier = Modifier.size(64.dp)) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntriesList(entries: List<SatItem>, onSelected: (List<Int>, Boolean) -> Unit) {
    LazyColumn {
        items(items = entries, key = { item -> item.catnum }) { entry ->
            Entry(entry, onSelected, Modifier.animateItemPlacement())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryPreview() {
    val satItem = SatItem(45555, "SatName", emptyList(), true)
    MainTheme { Entry(item = satItem, onSelected = { _, _ -> run {} }, modifier = Modifier) }
}

@Composable
private fun Entry(item: SatItem, onSelected: (List<Int>, Boolean) -> Unit, modifier: Modifier) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
        Surface(modifier = Modifier.padding(bottom = 1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 0.dp, top = 6.dp, end = 12.dp, bottom = 6.dp)
                    .onClick { onSelected(listOf(item.catnum), item.isSelected.not()) }) {
                Text(
                    text = "Id:${item.catnum}  -  ",
                    modifier = Modifier.width(100.dp),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = item.name,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Checkbox(checked = item.isSelected, onCheckedChange = null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(setQuery: (String) -> Unit) {
    val currentQuery = rememberSaveable { mutableStateOf("") }
    OutlinedTextField(value = currentQuery.value,
        onValueChange = { newValue ->
            currentQuery.value = newValue
            setQuery(newValue)
        },
        maxLines = 1,
//            label = { Text(text = stringResource(id = R.string.entries_search_hint)) },
        placeholder = { Text(text = stringResource(id = R.string.entries_search_hint)) },
        leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            Icon(imageVector = Icons.Outlined.Close,
                contentDescription = null,
                modifier = Modifier.onClick {
                    currentQuery.value = ""
                    setQuery("")
                })
        },
//            colors = TextFieldDefaults.outlinedTextFieldColors(
//                focusedBorderColor = MaterialTheme.colorScheme.onBackground,
//                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground
//            ),
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    )
}
