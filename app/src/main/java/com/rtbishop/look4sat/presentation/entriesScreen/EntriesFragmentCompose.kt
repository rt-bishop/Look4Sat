package com.rtbishop.look4sat.presentation.entriesScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
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
            setContent {
                val entriesViewModel: EntriesViewModel = viewModel()
                EntriesScreen(entriesViewModel)
            }
        }
    }

    @Preview(showBackground = true, widthDp = 400, heightDp = 720, showSystemUi = true)
    @Composable
    private fun EntriesScreen(entriesViewModel: EntriesViewModel = viewModel()) {
        Look4SatTheme {
            val dataState = entriesViewModel.satData.observeAsState(DataState.Loading)
            val onSelected: (List<Int>, Boolean) -> Unit = { list, value ->
                entriesViewModel.updateSelection(list, value)
            }
            Card(modifier = Modifier.padding(6.dp)) {
                when (val state = dataState.value) {
                    is DataState.Success -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SearchBar()
                            EntriesList(dataState = state.data, onSelected)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    @Composable
    fun SearchBar() {
        Card {

        }
    }

    @Composable
    fun EntriesList(dataState: List<SatItem>, onSelected: (List<Int>, Boolean) -> Unit) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) { items(dataState) { EntryItem(item = it, onSelected) } }
    }

    @Composable
    fun EntryItem(item: SatItem, onSelected: (List<Int>, Boolean) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .noRippleClickable { onSelected(listOf(item.catnum), item.isSelected.not()) }) {
            Checkbox(checked = item.isSelected, onCheckedChange = null, Modifier.padding(6.dp))
            Text(text = item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
        clickable(remember { MutableInteractionSource() }, null) { onClick() }
    }
}
