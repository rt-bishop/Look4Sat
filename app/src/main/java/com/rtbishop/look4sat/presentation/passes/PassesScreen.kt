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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.predict.NearEarthObject
import com.rtbishop.look4sat.domain.predict.OrbitalData
import com.rtbishop.look4sat.domain.predict.OrbitalPass
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.components.PullRefreshIndicator
import com.rtbishop.look4sat.presentation.components.PullRefreshState
import com.rtbishop.look4sat.presentation.components.pullRefresh
import com.rtbishop.look4sat.presentation.components.rememberPullRefreshState
import java.text.SimpleDateFormat
import java.util.*

private val sdf = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

@Composable
fun PassesScreen(uiState: PassesState, navToRadar: (Int, Long) -> Unit) {
    val refreshState = rememberPullRefreshState(refreshing = uiState.isRefreshing, onRefresh = {
        uiState.takeAction(PassesAction.RefreshPasses)
    })
    val toggleDialog = { uiState.takeAction(PassesAction.ToggleFilterDialog) }
    if (uiState.isDialogShown) {
        FilterDialog(uiState.hours, uiState.elevation, uiState.modes, toggleDialog) { hours, elevation, modes ->
            uiState.takeAction(PassesAction.ApplyFilter(hours, elevation, modes))
        }
    }
    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TopBar(nextId = uiState.nextId, nextName = uiState.nextName, nextTime = uiState.nextTime, toggleDialog)
        PassesCard(refreshState, uiState.isRefreshing, uiState.itemsList, navToRadar)
    }
}

@Composable
@Preview(showBackground = true)
private fun TopBarPreview() {
    MainTheme { TopBar(nextId = 45555, nextName = "Stuff", nextTime = "88:88:88") {} }
}

@Composable
private fun TopBar(nextId: Int, nextName: String, nextTime: String, toggleDialog: () -> Unit) {
    ElevatedCard(modifier = Modifier.height(48.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_sputnik),
                contentDescription = null,
                modifier = Modifier.padding(12.dp)
            )
            Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.weight(1f)) {
                Text(
                    text = "AOS - Id:$nextId",
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = nextName, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = nextTime,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
            IconButton(onClick = { toggleDialog() }) {
                Icon(painter = painterResource(id = R.drawable.ic_filter), contentDescription = null)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PassPreview() {
    val data = OrbitalData(
        "Satellite", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45000, 0.0
    )
    val satellite = NearEarthObject(data)
    val pass = OrbitalPass(1L, 0.0, 10L, 180.0, 850, 45.0, satellite, 0.5f)
    MainTheme { Pass(pass = pass, { _, _ -> }) }
}

@Composable
private fun Pass(pass: OrbitalPass, navToRadar: (Int, Long) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
        Surface(modifier = Modifier
            .padding(bottom = 2.dp)
            .clickable { navToRadar(pass.catNum, pass.aosTime) }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Id:${pass.catNum} - ",
                        modifier = Modifier.width(82.dp),
                        textAlign = TextAlign.End,
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
                        text = stringResource(id = R.string.pass_aosAz, pass.aosAzimuth),
                        textAlign = TextAlign.Start,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(id = R.string.pass_altitude, pass.altitude),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = stringResource(id = R.string.pass_losAz, pass.losAzimuth),
                        textAlign = TextAlign.End,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val deepTime = stringResource(id = R.string.pass_placeholder)
                    Text(text = if (pass.isDeepSpace) deepTime else sdf.format(Date(pass.aosTime)), fontSize = 15.sp)
                    LinearProgressIndicator(
                        progress = if (pass.isDeepSpace) 1f else pass.progress,
                        modifier = modifier
                            .fillMaxWidth(0.75f)
                            .padding(top = 3.dp),
                        trackColor = MaterialTheme.colorScheme.inverseSurface
                    )
                    Text(text = if (pass.isDeepSpace) deepTime else sdf.format(Date(pass.losTime)), fontSize = 15.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PassesCard(
    refreshState: PullRefreshState, isRefreshing: Boolean, passes: List<OrbitalPass>, navToRadar: (Int, Long) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.pullRefresh(refreshState)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = passes, key = { item -> item.catNum + item.aosTime }) { pass ->
                    Pass(pass, navToRadar, Modifier.animateItemPlacement())
                }
            }
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = refreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}
