package com.rtbishop.look4sat.core.presentation

sealed class Screen(val route: String, val iconResId: Int, val titleResId: Int) {
    data object Satellites : Screen("satellites", R.drawable.ic_satellites, R.string.nav_sat)
    data object Passes : Screen("passes", R.drawable.ic_passes, R.string.nav_pass)
    data object Radar : Screen("radar", R.drawable.ic_radar, R.string.nav_radar)
    data object Map : Screen("map", R.drawable.ic_map, R.string.nav_map)
    data object Settings : Screen("settings", R.drawable.ic_settings, R.string.nav_prefs)
    data object RadioControl : Screen("radiocontrol", R.drawable.ic_radios, R.string.nav_radiocontrol)
}
