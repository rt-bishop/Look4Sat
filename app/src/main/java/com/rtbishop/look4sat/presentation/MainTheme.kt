package com.rtbishop.look4sat.presentation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccentYellow = Color(0xFFFFE082)
private val TextWhite = Color(0xCCFFFFFF)
private val TextGrey = Color(0x66FFFFFF)
private val TextBlack = Color(0xFF000000)
private val SurfaceInverse = Color(0x40FFFFFF)
private val SurfaceBtn = Color(0xFF303030)
private val SurfaceCard = Color(0xFF1C1C1C)
private val SurfaceBg = Color(0xFF121212)
private val Transparent = Color(0x00000000)

private val MainColors = lightColorScheme(
    primary = AccentYellow,
    onPrimary = TextBlack,
//    primaryContainer = primaryContainer,
//    onPrimaryContainer = onPrimaryContainer,
//    inversePrimary = inversePrimary,
    secondary = AccentYellow,
    onSecondary = TextBlack,
    secondaryContainer = AccentYellow, // navBar indicator
    onSecondaryContainer = TextBlack, // navBar active icon
    tertiary = SurfaceBtn,
    onTertiary = TextWhite,
//    tertiaryContainer = tertiaryContainer,
//    onTertiaryContainer = onTertiaryContainer,
    background = SurfaceBg,
    onBackground = TextWhite,
    surface = SurfaceCard,
    onSurface = TextWhite,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextGrey, // navBar inactive icon
    surfaceTint = Transparent,
    inverseSurface = SurfaceInverse,
//    inverseOnSurface = inverseOnSurface,
//    error = error,
//    onError = onError,
//    errorContainer = errorContainer,
//    onErrorContainer = onErrorContainer,
//    outline = outline,
//    outlineVariant = outlineVariant,
//    scrim = scrim,
)

private val Typography = Typography(
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

private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp), // buttons corners
    medium = RoundedCornerShape(8.dp), // cards corners
    large = RoundedCornerShape(12.dp), extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun MainTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }
    val colors = MainColors
//    val view = LocalView.current
//    if (!view.isInEditMode) {
//        SideEffect {
//            (view.context as Activity).window.statusBarColor = colorScheme.primary.toArgb()
//            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = darkTheme
//        }
//    }
    MaterialTheme(colorScheme = colors, typography = Typography, content = content, shapes = Shapes)
}
