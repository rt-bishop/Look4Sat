package com.rtbishop.look4sat.presentation.radar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.domain.model.SatRadio

@Composable
fun RadarScreen() {
    val viewModel = viewModel(RadarViewModel::class.java, factory = RadarViewModel.Factory)
    val currentPass = viewModel.getPass().collectAsState(null)

    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ElevatedCard(modifier = Modifier.height(52.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter),
                        contentDescription = null
                    )
                }
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Null")
                    Text(text = "Null")
                }
                Text(
                    text = "00:00:00",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 2.dp, end = 8.dp)
                )
            }
        }
        ElevatedCard(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            viewModel.radarData.value?.let { data ->
                RadarViewCompose(
                    item = data.satPos,
                    items = data.satTrack,
                    azimElev = viewModel.orientation.value
                )
            }
        }
        ElevatedCard(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            TransmittersList(transmitters = viewModel.transmitters.value)
        }
    }
}

@Composable
private fun TransmittersList(transmitters: List<SatRadio>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = transmitters, key = { item -> item.uuid }) { radio ->
            TransmitterItem(radio)
        }
    }
}

@Composable
private fun TransmitterItem(radio: SatRadio) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Surface(modifier = Modifier.padding(bottom = 2.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow),
                        contentDescription = null, modifier = Modifier.rotate(180f)
                    )
                    Text(
                        text = radio.info,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow),
                        contentDescription = null
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.radio_downlink, radio.downlink ?: 0L),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(0.5f)
                    )
                    Text(
                        text = stringResource(id = R.string.radio_uplink, radio.uplink ?: 0L),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(0.5f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = radio.mode ?: "",
                        fontSize = 15.sp
                    )
                    Text(
                        text = radio.isInverted.toString(),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

//private val divider = 1000000f
//radioDownlink.text = String.format(Locale.ENGLISH, link, downlink / divider)
//radioUplink.text = String.format(Locale.ENGLISH, link, uplink / divider)

//private fun setupObservers() {
//    viewModel.getPass(45000, System.currentTimeMillis()).observe(viewLifecycleOwner) { pass ->
//        binding?.run {
//            radarView = RadarView(requireContext()).apply {
//                setShowAim(viewModel.getUseCompass())
//                setScanning(viewModel.getShowSweep())
//            }
//            viewModel.radarData.observe(viewLifecycleOwner) { passData ->
//                setPassText(pass, passData.satPos)
//            }
//        }
//    }
//}
//
//private fun setPassText(satPass: SatPass, satPos: SatPos) {
//    binding?.run {
//        val timeNow = System.currentTimeMillis()
//        val radarAzim = getString(R.string.radar_az_value)
//        val radarElev = getString(R.string.radar_el_value)
//        val radarAlt = getString(R.string.radar_alt_value)
//        val radarDist = getString(R.string.radar_dist_value)
//        radarAzValue.text = String.format(radarAzim, satPos.azimuth.toDegrees())
//        radarElValue.text = String.format(radarElev, satPos.elevation.toDegrees())
//        radarAltValue.text = String.format(radarAlt, satPos.altitude)
//        radarDstValue.text = String.format(radarDist, satPos.distance)
//        if (satPos.eclipsed) {
//            radarVisibility.text = getText(R.string.radar_eclipsed)
//        } else {
//            radarVisibility.text = getText(R.string.radar_visible)
//        }
//        if (!satPass.isDeepSpace) {
//            if (timeNow < satPass.aosTime) {
//                val millisBeforeStart = satPass.aosTime.minus(timeNow)
//                radarTimer.text = millisBeforeStart.toTimerString()
//            } else {
//                val millisBeforeEnd = satPass.losTime.minus(timeNow)
//                radarTimer.text = millisBeforeEnd.toTimerString()
//                if (timeNow > satPass.losTime) {
//                    radarTimer.text = 0L.toTimerString()
////                        findNavController().navigateUp()
//                }
//            }
//        } else radarTimer.text = 0L.toTimerString()
//    }
//}
