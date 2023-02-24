package com.rtbishop.look4sat.presentation.passesScreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.predict.NearEarthSat
import com.rtbishop.look4sat.domain.predict.OrbitalData
import com.rtbishop.look4sat.domain.predict.SatPass
import com.rtbishop.look4sat.presentation.MainTheme
import java.text.SimpleDateFormat
import java.util.*

private val sdf = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)

@Composable
fun PassesScreen(viewModel: PassesViewModel = hiltViewModel()) {
    val state = viewModel.passes.observeAsState(DataState.Loading)
    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PassesCard(state = state.value)
    }
}

@Preview(showBackground = true)
@Composable
private fun PassPreview() {
    val data = OrbitalData(
        "Satellite", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45000, 0.0
    )
    val satellite = NearEarthSat(data)
    val pass = SatPass(1L, 25.0, 10L, 75.0, 850, 45.0, satellite, 0.5f)
    MainTheme { Pass(pass = pass) }
}

@Composable
private fun Pass(pass: SatPass, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier) {
        Surface(modifier = Modifier.padding(bottom = 1.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Id:${pass.catNum}  -  ",
                        modifier = Modifier.width(90.dp),
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
                    Text(text = stringResource(id = R.string.pass_aosAz, pass.aosAzimuth))
                    Text(
                        text = stringResource(id = R.string.pass_altitude, pass.altitude),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = stringResource(id = R.string.pass_losAz, pass.losAzimuth))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val deepTime = stringResource(id = R.string.pass_placeholder)
                    Text(text = if (pass.isDeepSpace) deepTime else sdf.format(Date(pass.aosTime)))
                    LinearProgressIndicator(
                        progress = pass.progress,
                        modifier = modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.inverseSurface
                    )
                    Text(text = if (pass.isDeepSpace) deepTime else sdf.format(Date(pass.losTime)))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PassesCard(state: DataState<List<SatPass>>) {
    ElevatedCard(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is DataState.Success -> {
                LazyColumn {
                    items(
                        items = state.data,
                        key = { item -> item.catNum + item.aosTime }) { pass ->
                        Pass(pass, Modifier.animateItemPlacement())
                    }
                }
            }
            else -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.size(80.dp))
                }
            }
        }
    }
}
