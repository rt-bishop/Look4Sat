package com.rtbishop.look4sat.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

@Composable
fun Look4SatTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colors = colors, typography = Typography, shapes = Shapes, content = content)
}

private val LightColors = lightColors(
    primary = SurfaceGrey,
    primaryVariant = ButtonGrey,
    secondary = Yellow,
    background = Background,
    surface = SurfaceGrey,
    onPrimary = TextWhite,
    onSecondary = TextBlack,
    onBackground = TextGrey,
    onSurface = TextWhite
)

private val DarkColors = darkColors(
    primary = SurfaceGrey,
    primaryVariant = ButtonGrey,
    secondary = Yellow,
    background = Background,
    surface = SurfaceGrey,
    onPrimary = TextWhite,
    onSecondary = TextBlack,
    onBackground = TextGrey,
    onSurface = TextWhite
)
