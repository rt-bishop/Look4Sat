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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rtbishop.look4sat.domain.model.DataState
import com.rtbishop.look4sat.domain.predict.NearEarthSat
import com.rtbishop.look4sat.domain.predict.OrbitalData
import com.rtbishop.look4sat.domain.predict.SatPass
import com.rtbishop.look4sat.presentation.MainTheme

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
                        text = pass.name,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Id:${pass.catNum}", color = MaterialTheme.colorScheme.secondary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "AOS - 90")
                    Text(text = "Altitude: 1800 km")
                    Text(text = "180 - LOS")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "16:02:28 - Sun")
                    Text(text = "Elevation: 75")
                    Text(text = "Sun - 16:02:28")
                }
                LinearProgressIndicator(progress = pass.progress, modifier = modifier.fillMaxWidth())
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
                    items(items = state.data, key = { item -> item.catNum + item.aosTime }) { pass ->
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
