package com.rtbishop.look4sat.presentation.radarScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rtbishop.look4sat.R

@Composable
fun RadarScreen(viewModel: RadarViewModel = hiltViewModel()) {

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
        ElevatedCard(modifier = Modifier.fillMaxSize().weight(1f)) {

        }
        ElevatedCard(modifier = Modifier.fillMaxSize().weight(1f)) {

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
//            radarCard.addView(radarView)
//            viewModel.radarData.observe(viewLifecycleOwner) { passData ->
//                radarView?.setPosition(passData.satPos)
//                radarView?.setPositions(passData.satTrack)
//                setPassText(pass, passData.satPos)
//            }
//            viewModel.transmitters.observe(viewLifecycleOwner) { list ->
//                if (list.isNotEmpty()) {
//                    radioAdapter.submitList(list)
//                    radarProgress.visibility = View.INVISIBLE
//                } else {
//                    radarProgress.visibility = View.INVISIBLE
//                    radarEmptyLayout.visibility = View.VISIBLE
//                }
//                radarView?.invalidate()
//            }
//            viewModel.orientation.observe(viewLifecycleOwner) { value ->
//                radarView?.setOrientation(value.first, value.second, value.third)
//            }
//                radarBtnBack.clickWithDebounce { findNavController().navigateUp() }
//                radarBtnMap.clickWithDebounce {
//                    val direction = RadarFragmentDirections.globalToMap(pass.catNum)
//                    findNavController().navigate(direction)
//                }
//                radarBtnNotify.isEnabled = false
//                radarBtnSettings.clickWithDebounce {
//                    val direction = RadarFragmentDirections.globalToSettings()
//                    findNavController().navigate(direction)
//                }
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
