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
    val colors = themeColors
//    val view = LocalView.current
//    if (!view.isInEditMode) {
//        SideEffect {
//            (view.context as Activity).window.statusBarColor = colorScheme.primary.toArgb()
//            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = darkTheme
//        }
//    }
    MaterialTheme(colorScheme = colors, typography = typography, content = content, shapes = shapes)
}

private val accentYellow = Color(0xFFFFE082)
private val textWhite = Color(0xCCFFFFFF)
private val textGrey = Color(0x66FFFFFF)
private val textBlack = Color(0xFF000000)
private val surfaceInverse = Color(0x40FFFFFF)
private val surfaceBtn = Color(0xFF303030)
private val surfaceCard = Color(0xFF1C1C1C)
private val surfaceBg = Color(0xFF121212)
private val transparent = Color(0x00000000)
private val themeColors = lightColorScheme(
    primary = accentYellow,
    onPrimary = textBlack,
//    primaryContainer = primaryContainer,
//    onPrimaryContainer = onPrimaryContainer,
//    inversePrimary = inversePrimary,
    secondary = accentYellow,
    onSecondary = textBlack,
    secondaryContainer = accentYellow, // navBar indicator
    onSecondaryContainer = textBlack, // navBar active icon
    tertiary = surfaceBtn,
    onTertiary = textWhite,
//    tertiaryContainer = tertiaryContainer,
//    onTertiaryContainer = onTertiaryContainer,
    background = surfaceBg,
    onBackground = textWhite,
    surface = surfaceCard,
    onSurface = textWhite,
    surfaceVariant = surfaceCard,
    onSurfaceVariant = textGrey, // navBar inactive icon
    surfaceTint = transparent,
    inverseSurface = surfaceInverse,
//    inverseOnSurface = inverseOnSurface,
//    error = error,
//    onError = onError,
//    errorContainer = errorContainer,
//    onErrorContainer = onErrorContainer,
//    outline = outline,
//    outlineVariant = outlineVariant,
//    scrim = scrim,
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

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp), // buttons corners
    medium = RoundedCornerShape(8.dp), // cards corners
    large = RoundedCornerShape(12.dp), extraLarge = RoundedCornerShape(16.dp)
)
