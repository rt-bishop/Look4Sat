package com.rtbishop.look4sat.presentation.entriesScreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.presentation.onClick

@Composable
fun EntriesScreenView() {
    val viewModel = hiltViewModel<EntriesViewModel>()
    val dataState = viewModel.satData.observeAsState(DataState.Loading)
    Column(modifier = Modifier.padding(6.dp)) {
        Card { SearchBar(setQuery = { newQuery -> viewModel.setQuery(newQuery) }) }
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LoadingOrList(state = dataState.value) { list, value ->
                viewModel.updateSelection(list, value)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        EntryTypeBar(entryType = "All")

    }
}

@Composable
fun EntryTypeBar(entryType: String, modifier: Modifier = Modifier) {
    val icon = Icons.Outlined.ArrowForward
    val text = "Selected type: $entryType"
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
//                Icon(painter = painterResource(id = R.drawable.ic_next), contentDescription = null)
            Icon(imageVector = icon, contentDescription = null, modifier = modifier.size(48.dp))
            Text(text = text, maxLines = 1, modifier = modifier.fillMaxWidth())
        }
    }
}

@Composable
fun LoadingOrList(state: DataState<List<SatItem>>, onSelected: (List<Int>, Boolean) -> Unit) {
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
fun LoadingProgress() {
    Row(
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) { CircularProgressIndicator(modifier = Modifier.size(64.dp)) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntriesList(entries: List<SatItem>, onSelected: (List<Int>, Boolean) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Adaptive(200.dp)) {
        items(items = entries, key = { item -> item.catnum }) { entry ->
            EntryItem(entry, onSelected, Modifier.animateItemPlacement())
        }
    }
}

@Composable
fun EntryItem(entry: SatItem, onSelected: (List<Int>, Boolean) -> Unit, modifier: Modifier) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
        Surface(modifier = modifier.padding(bottom = 1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .onClick { onSelected(listOf(entry.catnum), entry.isSelected.not()) }) {
                Checkbox(
                    checked = entry.isSelected, onCheckedChange = null, Modifier.padding(6.dp)
                )
                Text(text = entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(setQuery: (String) -> Unit) {
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
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
        },
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
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    )
}
