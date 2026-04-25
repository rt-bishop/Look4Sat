/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat

import android.content.Context
import android.content.res.Configuration
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.core.presentation.MainTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val config = Configuration(newBase?.resources?.configuration)
        applyOverrideConfiguration(config.apply { fontScale = 1.0f })
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        observeNightFilterState()
        setContent {
            MainTheme(isDarkTheme = true) { MainScreen() }
        }
    }

    private fun observeNightFilterState() {
        val mainContainer = (applicationContext as IContainerProvider).getMainContainer()
        lifecycleScope.launch {
            mainContainer.settingsRepo.otherSettings
                .map { it.stateOfNightMode }
                .distinctUntilChanged()
                .collect { nightMode -> applyNightFilter(nightMode) }
        }
    }

    private fun applyNightFilter(enabled: Boolean) {
        if (enabled) {
            val nightMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,   // R → R
                    0f, 0f, 0f, 0f, 0f,   // G → 0
                    0f, 0f, 0f, 0f, 0f,   // B → 0
                    0f, 0f, 0f, 1f, 0f    // A → A
                )
            )
            window.decorView.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply {
                colorFilter = ColorMatrixColorFilter(nightMatrix)
            })
        } else {
            window.decorView.setLayerType(View.LAYER_TYPE_NONE, null)
        }
    }
}
