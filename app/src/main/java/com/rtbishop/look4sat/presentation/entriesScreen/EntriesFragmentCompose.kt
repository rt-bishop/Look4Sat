package com.rtbishop.look4sat.presentation.entriesScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.presentation.theme.Look4SatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EntriesFragmentCompose : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { Look4SatTheme { EntriesScreenView() } }
        }
    }

    @Composable
    fun EntriesScreenView(viewModel: EntriesViewModel = viewModel()) {
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
        val icon = Icons.Outlined.ArrowRight
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
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
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
        Surface(color = MaterialTheme.colors.background, modifier = modifier) {
            Surface(modifier = modifier.padding(bottom = 1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                        .background(MaterialTheme.colors.surface)
                        .onClick { onSelected(listOf(entry.catnum), entry.isSelected.not()) }) {
                    Checkbox(
                        checked = entry.isSelected, onCheckedChange = null, Modifier.padding(6.dp)
                    )
                    Text(text = entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    @Composable
    fun SearchBar(setQuery: (String) -> Unit) {
        val currentQuery = rememberSaveable { mutableStateOf("") }
        OutlinedTextField(value = currentQuery.value, onValueChange = { newValue ->
            currentQuery.value = newValue
            setQuery(newValue)
        }, maxLines = 1,
//            label = { Text(text = stringResource(id = R.string.entries_search_hint)) },
            placeholder = { Text(text = stringResource(id = R.string.entries_search_hint)) },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
            }, trailingIcon = {
                Icon(imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.onClick {
                        currentQuery.value = ""
                        setQuery("")
                    })
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.onBackground,
                unfocusedBorderColor = MaterialTheme.colors.onBackground),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
        )
    }

    private fun Modifier.onClick(onClick: () -> Unit): Modifier = composed {
        clickable(remember { MutableInteractionSource() }, null) { onClick() }
    }
}
