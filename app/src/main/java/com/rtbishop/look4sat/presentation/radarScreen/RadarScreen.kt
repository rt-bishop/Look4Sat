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
