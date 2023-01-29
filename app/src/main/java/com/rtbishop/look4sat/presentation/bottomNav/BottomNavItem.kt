package com.rtbishop.look4sat.presentation.bottomNav

import com.rtbishop.look4sat.R

sealed class BottomNavItem(var title: String, var icon: Int, var screen_route: String) {
    object Satellites : BottomNavItem("Satellites", R.drawable.ic_satellite, "satellites")
    object Passes : BottomNavItem("Passes", R.drawable.ic_list, "passes")
    object WorldMap : BottomNavItem("World Map", R.drawable.ic_map, "world_map")
    object Settings : BottomNavItem("Settings", R.drawable.ic_settings, "settings")
    object About : BottomNavItem("About", R.drawable.ic_about, "about")
}
