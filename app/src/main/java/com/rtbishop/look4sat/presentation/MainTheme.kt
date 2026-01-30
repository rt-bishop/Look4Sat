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
package com.rtbishop.look4sat.presentation

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val LocalSpacing = compositionLocalOf { Spacing }

data object Spacing {
    val extraExtraSmall = 4.dp
    val extraSmall = 6.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
}

@Composable
fun MainTheme(isDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val view = LocalView.current
    if (view.isInEditMode) {
        MaterialTheme(darkScheme, shapes, typography, content)
    } else {
        CompositionLocalProvider(LocalSpacing provides Spacing) {
            val colorScheme = if (isDarkTheme) darkScheme else lightScheme
            SideEffect {
                val window = (view.context as ComponentActivity).window
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }
            MaterialTheme(colorScheme, shapes, typography, content)
        }
    }
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF715C0C),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF685E40),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1E1BB),
    onSecondaryContainer = Color(0xFF221B04),
    background = Color(0xFFFFF8F0),
    onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFFFF8F0),
    onSurface = Color(0xFF1E1B13),
    surfaceTint = Color(0x00000000),
    surfaceVariant = Color(0xFFEBE2CF),
    onSurfaceVariant = Color(0xFF4C4639),
    error = Color(0xFFDC0000),
    outline = Color(0xFF7D7667),
    scrim = Color(0xFF000000)
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFFFE082), // main accent, switch background, active progress
    onPrimary = Color(0xFF000000), // on main accent, switch knob
//    primaryContainer = Color(0xFF121212),
//    onPrimaryContainer = Color(0xFF121212),
//    inversePrimary = Color(0xFF121212),
    secondary = Color(0xFFE0E0E0),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF404040), // navBar indicator,
    onSecondaryContainer = Color(0xFFE0E0E0), // navBar active icon
//    tertiary = Color(0xFF121212),
//    onTertiary = Color(0xFF121212),
//    tertiaryContainer = Color(0xFF121212),
//    onTertiaryContainer = Color(0xFF121212),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF202020), // card background
    onSurface = Color(0xFFE0E0E0), // on card background
    surfaceVariant = Color(0xFF404040), // buttons background
    onSurfaceVariant = Color(0xFFE0E0E0), // navBar inactive icon
    surfaceTint = Color(0x00000000),
//    inverseSurface = Color(0xFF121212),
//    inverseOnSurface = Color(0xFF121212),
    error = Color(0xFFDC0000),
//    onError = Color(0xFFFFFFFF),
//    errorContainer = Color(0xFF121212),
//    onErrorContainer = Color(0xFF121212),
    outline = Color(0xA3E0E0E0),
//    outlineVariant = Color(0xFF121212),
    scrim = Color(0xFF000000),
//    surfaceBright = Color(0xFF121212),
    surfaceContainer = Color(0xFF121212), // navBar background
//    surfaceContainerHigh = Color(0xFF121212),
    surfaceContainerHighest = Color(0xFF242424), // filled card background
    surfaceContainerLow = Color(0xFF202020), // elevated card background
    surfaceContainerLowest = Color(0xCC121212),
//    surfaceDim = Color(0xFF121212),
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), // menus, snackbars, text fields
    small = RoundedCornerShape(8.dp), // buttons, chips
    medium = RoundedCornerShape(12.dp), // cards, small FABs
    large = RoundedCornerShape(24.dp), // extended FABs, FABs, nav drawers
    extraLarge = RoundedCornerShape(32.dp) // large FABs
)

private val typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ), titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ), labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
