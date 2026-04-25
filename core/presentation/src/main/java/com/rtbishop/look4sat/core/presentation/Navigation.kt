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
package com.rtbishop.look4sat.core.presentation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen(val iconResId: Int, val titleResId: Int) : NavKey {

    @Serializable
    data object Satellites : Screen(R.drawable.ic_satellites, R.string.nav_sat)

    @Serializable
    data object Passes : Screen(R.drawable.ic_passes, R.string.nav_pass)

    @Serializable
    data class Radar(val catNum: Int = 0, val aosTime: Long = 0L) : Screen(R.drawable.ic_radar, R.string.nav_radar)

    @Serializable
    data class RadioControl(val catNum: Int = 0, val aosTime: Long = 0L) : Screen(0, 0)

    @Serializable
    data object Map : Screen(R.drawable.ic_map, R.string.nav_map)

    @Serializable
    data object Settings : Screen(R.drawable.ic_settings, R.string.nav_prefs)
}
