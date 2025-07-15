package com.rtbishop.look4sat.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.rtbishop.look4sat.presentation.map.mapDestination
import com.rtbishop.look4sat.presentation.passes.passesDestination
import com.rtbishop.look4sat.presentation.radar.radarDestination
import com.rtbishop.look4sat.presentation.satellites.satellitesDestination
import com.rtbishop.look4sat.presentation.settings.settingsDestination

@Composable
fun MainScreen() {
    val navController: NavHostController = rememberNavController()
    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "passes",
            modifier = Modifier.padding(innerPadding)
        ) {
            satellitesDestination(navController)
            passesDestination(navController)
            radarDestination(navController)
            mapDestination(navController)
            settingsDestination(navController)
        }
    }
}
