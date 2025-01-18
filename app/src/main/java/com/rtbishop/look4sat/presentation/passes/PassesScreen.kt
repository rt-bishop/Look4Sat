package com.rtbishop.look4sat.presentation.passes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.rtbishop.look4sat.domain.predict.DeepSpaceObject
import com.rtbishop.look4sat.domain.predict.NearEarthObject
import com.rtbishop.look4sat.domain.predict.OrbitalData
import com.rtbishop.look4sat.domain.predict.OrbitalPass
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.Screen
import com.rtbishop.look4sat.presentation.components.CardIcon
import com.rtbishop.look4sat.presentation.components.NextPassRow
import com.rtbishop.look4sat.presentation.components.TimerBar
import com.rtbishop.look4sat.presentation.components.TimerRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sdfDate = SimpleDateFormat("EEE dd MMM", Locale.ENGLISH)
private val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

fun NavGraphBuilder.passesDestination(navigateToRadar: (Int, Long) -> Unit) {
    composable(Screen.Passes.route) {
        val viewModel = viewModel(
            modelClass = PassesViewModel::class.java,
            factory = PassesViewModel.Factory
        )
        val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
        PassesScreen(uiState, navigateToRadar)
    }
}

@Composable
private fun PassesScreen(uiState: PassesState, navigateToRadar: (Int, Long) -> Unit) {
    val refreshPasses = { uiState.takeAction(PassesAction.RefreshPasses) }
    val showPassesDialog = { uiState.takeAction(PassesAction.TogglePassesDialog) }
    val showRadiosDialog = { uiState.takeAction(PassesAction.ToggleRadiosDialog) }
    if (uiState.isPassesDialogShown) {
        PassesDialog(hours = uiState.hours, elevation = uiState.elevation, cancel = showPassesDialog) { hours, elevation ->
            uiState.takeAction(PassesAction.FilterPasses(hours, elevation))
        }
    }
    if (uiState.isRadiosDialogShown) {
        RadiosDialog(modes = uiState.modes, cancel = showRadiosDialog) { modes ->
            uiState.takeAction(PassesAction.FilterRadios(modes))
        }
    }
    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TimerRow {
            CardIcon(onClick = { showPassesDialog() }, iconId = R.drawable.ic_filter)
            TimerBar(timeString = uiState.nextTime, isTimeAos = uiState.isNextTimeAos)
            CardIcon(onClick = { showRadiosDialog() }, iconId = R.drawable.ic_satellite)
        }
        NextPassRow(pass = uiState.nextPass)
        PassesList(uiState.isRefreshing, uiState.itemsList, navigateToRadar, refreshPasses)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassesList(
    isRefreshing: Boolean,
    passes: List<OrbitalPass>,
    navigateToRadar: (Int, Long) -> Unit,
    refreshPasses: () -> Unit
) {
    val refreshState = rememberPullToRefreshState()
    ElevatedCard(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = refreshState,
            onRefresh = refreshPasses,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = refreshState,
                    isRefreshing = isRefreshing,
                    color = MaterialTheme.colorScheme.background,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = passes, key = { item -> item.catNum + item.aosTime }) { pass ->
                    if (pass.isDeepSpace) {
                        DeepSpacePass(
                            pass = pass,
                            navigateToRadar = navigateToRadar,
                            modifier = Modifier.animateItem()
                        )
                    } else {
                        NearEarthPass(
                            pass = pass,
                            navigateToRadar = navigateToRadar,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeepSpacePassPreview() {
    val data = OrbitalData(
        "Satellite", 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45000, 0.0
    )
    val satellite = DeepSpaceObject(data)
    val pass = OrbitalPass(1L, 180.0, 10L, 360.0, 36650, 45.0, satellite, 0.5f)
    MainTheme { DeepSpacePass(pass = pass, { _, _ -> }) }
}

@Composable
private fun DeepSpacePass(
    pass: OrbitalPass,
    navigateToRadar: (Int, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val passSatId = stringResource(id = R.string.pass_satId, pass.catNum)
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
        Surface(modifier = Modifier
            .padding(bottom = 2.dp)
            .clickable { navigateToRadar(pass.catNum, pass.aosTime) }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$passSatId - ",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = pass.name,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_elevation),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${pass.maxElevation}°",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DeepSpace",
                            fontSize = 15.sp
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_altitude),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pass.altitude} km",
                            fontSize = 15.sp
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_direction),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                id = R.string.pass_aosLos,
                                pass.aosAzimuth.toInt(),
                                pass.losAzimuth.toInt()
                            ),
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NearEarthPassPreview() {
    val data = OrbitalData(
        "Satellite", 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45000, 0.0
    )
    val satellite = NearEarthObject(data)
    val pass = OrbitalPass(1L, 180.0, 10L, 360.0, 36650, 45.0, satellite, 0.5f)
    MainTheme { NearEarthPass(pass = pass, { _, _ -> }) }
}

@Composable
private fun NearEarthPass(
    pass: OrbitalPass,
    navigateToRadar: (Int, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val passSatId = stringResource(id = R.string.pass_satId, pass.catNum)
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
        Surface(modifier = Modifier
            .padding(bottom = 2.dp)
            .clickable { navigateToRadar(pass.catNum, pass.aosTime) }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$passSatId - ",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = pass.name,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_elevation),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${pass.maxElevation}°",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_calendar),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = sdfDate.format(Date(pass.aosTime)),
                            fontSize = 15.sp
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_altitude),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pass.altitude} km",
                            fontSize = 15.sp
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_direction),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                id = R.string.pass_aosLos,
                                pass.aosAzimuth.toInt(),
                                pass.losAzimuth.toInt()
                            ),
                            fontSize = 15.sp
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val defaultTime = "   - - : - -   "
                    Text(
                        text = if (pass.isDeepSpace) defaultTime else sdfTime.format(Date(pass.aosTime)),
                        fontSize = 15.sp
                    )
                    LinearProgressIndicator(
                        progress = { if (pass.isDeepSpace) 100f else pass.progress },
                        drawStopIndicator = {},
                        modifier = modifier.fillMaxWidth(0.75f)
                    )
                    Text(
                        text = if (pass.isDeepSpace) defaultTime else sdfTime.format(Date(pass.losTime)),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
