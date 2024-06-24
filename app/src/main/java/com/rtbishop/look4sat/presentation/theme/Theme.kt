package com.rtbishop.look4sat.presentation.theme
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.rtbishop.look4sat.MainApplication

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (view.isInEditMode) MaterialTheme(
        colorScheme = lightScheme,
        typography = typography,
        shapes = shapes
    ) {
        CompositionLocalProvider(LocalOverscrollConfiguration.provides(null), content = content)
    }
    else {
        val otherSettings by (LocalContext.current.applicationContext as MainApplication).container.settingsRepo.otherSettings.collectAsState()
        val isOldScheme = otherSettings.stateOfOldScheme
        val colorScheme = when {
            isOldScheme -> oldScheme

            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> darkScheme
            else -> lightScheme
        }
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                if (isOldScheme) false else !darkTheme
        }
        MaterialTheme(colorScheme = colorScheme, typography = typography, shapes = shapes) {
            CompositionLocalProvider(LocalOverscrollConfiguration.provides(null), content = content)
        }
    }
}

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

private val accentYellow = Color(0xFFFFE082)
private val textWhite = Color(0xCCFFFFFF)
private val textGrey = Color(0x66FFFFFF)
private val textBlack = Color(0xFF000000)
private val surfaceInverse = Color(0x40FFFFFF)
private val surfaceBtn = Color(0xFF303030)
private val surfaceCard = Color(0xFF1C1C1C)
private val surfaceBg = Color(0xFF121212)
private val transparent = Color(0x00000000)
private val oldScheme = lightColorScheme(
    primary = accentYellow,
    onPrimary = textBlack,
//    primaryContainer = primaryContainer,
//    onPrimaryContainer = onPrimaryContainer,
//    inversePrimary = inversePrimary,
    secondary = accentYellow,
    onSecondary = textBlack,
    secondaryContainer = surfaceInverse, // navBar indicator
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

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)
