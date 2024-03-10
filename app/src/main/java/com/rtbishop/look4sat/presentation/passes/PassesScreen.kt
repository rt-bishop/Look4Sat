package com.rtbishop.look4sat.presentation.passes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.predict.DeepSpaceObject
import com.rtbishop.look4sat.domain.predict.NearEarthObject
import com.rtbishop.look4sat.domain.predict.OrbitalData
import com.rtbishop.look4sat.domain.predict.OrbitalPass
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.components.CardIcon
import com.rtbishop.look4sat.presentation.components.NextPassRow
import com.rtbishop.look4sat.presentation.components.PullRefreshIndicator
import com.rtbishop.look4sat.presentation.components.PullRefreshState
import com.rtbishop.look4sat.presentation.components.TimerBar
import com.rtbishop.look4sat.presentation.components.TimerRow
import com.rtbishop.look4sat.presentation.components.pullRefresh
import com.rtbishop.look4sat.presentation.components.rememberPullRefreshState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sdfDate = SimpleDateFormat("EEE, dd MMM", Locale.ENGLISH)
private val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

@Composable
fun PassesScreen(uiState: PassesState, navToRadar: (Int, Long) -> Unit) {
    val refreshPasses = { uiState.takeAction(PassesAction.RefreshPasses) }
    val refreshState = rememberPullRefreshState(refreshing = uiState.isRefreshing, onRefresh = refreshPasses)
    val showPassesDialog = { uiState.takeAction(PassesAction.TogglePassesDialog) }
    val showRadiosDialog = { uiState.takeAction(PassesAction.ToggleRadiosDialog) }
    if (uiState.isPassesDialogShown) {
        PassesDialog(hours = uiState.hours, elev = uiState.elevation, dismiss = showPassesDialog) { hours, elevation ->
            uiState.takeAction(PassesAction.FilterPasses(hours, elevation))
        }
    }
    if (uiState.isRadiosDialogShown) {
        RadiosDialog(modes = uiState.modes, dismiss = showRadiosDialog) { modes ->
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
        PassesList(refreshState, uiState.isRefreshing, uiState.itemsList, navToRadar)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PassesList(
    refreshState: PullRefreshState,
    isRefreshing: Boolean,
    passes: List<OrbitalPass>,
    navToRadar: (Int, Long) -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.primary
    ElevatedCard(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.pullRefresh(refreshState), contentAlignment = Alignment.TopCenter) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = passes, key = { item -> item.catNum + item.aosTime }) { pass ->
                    PassItem(pass = pass, navToRadar = navToRadar, modifier = Modifier.animateItemPlacement())
                }
            }
            PullRefreshIndicator(refreshing = isRefreshing, state = refreshState, backgroundColor = backgroundColor)
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
    MainTheme { PassItem(pass = pass, { _, _ -> }) }
}

@Preview(showBackground = true)
@Composable
private fun NearEarthPassPreview() {
    val data = OrbitalData(
        "Satellite", 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45000, 0.0
    )
    val satellite = NearEarthObject(data)
    val pass = OrbitalPass(1L, 180.0, 10L, 360.0, 36650, 45.0, satellite, 0.5f)
    MainTheme { PassItem(pass = pass, { _, _ -> }) }
}

@Composable
private fun PassItem(pass: OrbitalPass, navToRadar: (Int, Long) -> Unit, modifier: Modifier = Modifier) {
    val passSatId = stringResource(id = R.string.pass_satId, pass.catNum)
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
        Surface(modifier = Modifier
            .padding(bottom = 2.dp)
            .clickable { navToRadar(pass.catNum, pass.aosTime) }) {
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
                    Text(
                        text = " ${pass.maxElevation}°",
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sdfDate.format(Date(pass.aosTime)),
                        textAlign = TextAlign.Start,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(
                            id = R.string.pass_aosLos,
                            pass.aosAzimuth.toInt(),
                            pass.losAzimuth.toInt()
                        ),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1.5f)
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_altitude),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " ${pass.altitude} km",
                            textAlign = TextAlign.Start,
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
                        modifier = modifier
                            .fillMaxWidth(0.75f)
                            .padding(top = 2.dp),
                        trackColor = MaterialTheme.colorScheme.inverseSurface
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
