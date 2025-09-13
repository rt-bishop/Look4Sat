package com.rtbishop.look4sat.presentation.satellites

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.Screen
import com.rtbishop.look4sat.presentation.common.CardLoadingIndicator
import com.rtbishop.look4sat.presentation.common.IconCard
import com.rtbishop.look4sat.presentation.common.InfoDialog
import com.rtbishop.look4sat.presentation.common.TopBar
import com.rtbishop.look4sat.presentation.common.infiniteMarquee
import com.rtbishop.look4sat.presentation.common.isVerticalLayout
import com.rtbishop.look4sat.presentation.common.layoutPadding

fun NavGraphBuilder.satellitesDestination(navigateUp: () -> Unit) {
    composable(Screen.Satellites.route) {
        val viewModel = viewModel(
            modelClass = SatellitesViewModel::class.java, factory = SatellitesViewModel.Factory
        )
        val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
        SatellitesScreen(uiState, navigateUp)
    }
}

@Composable
private fun SatellitesScreen(uiState: SatellitesState, navigateUp: () -> Unit) {
    val toggleDialog = { uiState.takeAction(SatellitesAction.ToggleTypesDialog) }
    if (uiState.isDialogShown) {
        MultiTypesDialog(allTypes = uiState.typesList, types = uiState.currentTypes, toggleDialog) {
            uiState.takeAction(SatellitesAction.SelectTypes(it))
        }
    }
    if (uiState.shouldSeeWarning) {
        InfoDialog(
            stringResource(R.string.satellites_warning_title),
            stringResource(R.string.satellites_warning_message)
        ) {
            uiState.takeAction(SatellitesAction.DismissWarning)
        }
    }
    val unselectAll = { uiState.takeAction(SatellitesAction.UnselectAll) }
    val selectAll = { uiState.takeAction(SatellitesAction.SelectAll) }
    val setQuery = { newQuery: String -> uiState.takeAction(SatellitesAction.SearchFor(newQuery)) }
    val saveSelection = { uiState.takeAction(SatellitesAction.SaveSelection).also { navigateUp() } }
    Column(modifier = Modifier.layoutPadding(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (isVerticalLayout()) {
            TopBar {
                TypeCard(types = uiState.currentTypes, toggleDialog, modifier = Modifier.weight(1f))
                SaveButton(saveSelection = saveSelection, modifier = Modifier.height(48.dp))
            }
            TopBar {
                SearchBar(setQuery = { setQuery(it) }, modifier = Modifier.weight(1f))
                IconCard(onClick = unselectAll, iconId = R.drawable.ic_check_off)
                IconCard(onClick = selectAll, iconId = R.drawable.ic_check_on)
            }
        } else {
            TopBar {
                SaveButton(saveSelection = saveSelection, modifier = Modifier.height(48.dp))
                TypeCard(types = uiState.currentTypes, toggleDialog, modifier = Modifier.weight(1f))
                SearchBar(setQuery = { setQuery(it) }, modifier = Modifier.weight(1f))
                IconCard(onClick = unselectAll, iconId = R.drawable.ic_check_off)
                IconCard(onClick = selectAll, iconId = R.drawable.ic_check_on)
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                CardLoadingIndicator()
            } else {
                SatellitesCard(uiState.itemsList) { id, isTicked ->
                    uiState.takeAction(SatellitesAction.SelectSingle(id, isTicked))
                }
            }
        }
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
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    if (currentQuery.value.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.entries_search_hint),
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
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

@Composable
private fun TypeCard(types: List<String>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val typesText = if (types.isEmpty()) "All" else types.joinToString(", ")
    ElevatedCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                text = "Types: $typesText",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
                    .infiniteMarquee()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SatellitePreview() {
    val satItem = SatItem(44444, "Ultra Super Mega long satellite name", true)
    MainTheme { Satellite(item = satItem, onSelected = { _, _ -> run {} }, modifier = Modifier) }
}

@Composable
private fun Satellite(item: SatItem, onSelected: (Int, Boolean) -> Unit, modifier: Modifier) {
    val passSatId = stringResource(id = R.string.pass_satId, item.catnum)
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.clickable { onSelected(item.catnum, item.isSelected) }) {
        Surface(modifier = Modifier.padding(bottom = 1.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 14.dp, top = 8.dp, end = 12.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "$passSatId - ", color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.name,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SatellitesPreview() {
    val entries = listOf(
        SatItem(8888, "Meteor", false),
        SatItem(44444, "ISS", true),
        SatItem(88888, "Starlink", false)
    )
    MainTheme { SatellitesCard(entries) { _, _ -> run {} } }
}

@Composable
fun SatellitesCard(items: List<SatItem>, onSelected: (Int, Boolean) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Adaptive(320.dp)) {
        items(items = items, key = { item -> item.catnum }) { entry ->
            Satellite(entry, onSelected, Modifier.animateItem())
        }
    }
}
