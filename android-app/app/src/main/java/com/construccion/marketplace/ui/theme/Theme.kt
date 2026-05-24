package com.construccion.marketplace.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ---- Esquema claro ----
private val LightColorScheme = lightColorScheme(
    primary = OrangeConstruction,
    onPrimary = Color.White,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,

    secondary = SteelGray,
    onSecondary = Color.White,
    secondaryContainer = SteelGrayContainer,
    onSecondaryContainer = OnSteelGrayContainer,

    tertiary = SafetyYellow,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFF8E1),
    onTertiaryContainer = Color(0xFF3E2700),

    error = ErrorRed,
    errorContainer = ErrorRedContainer,
    onError = Color.White,
    onErrorContainer = OnErrorRedContainer,

    background = BackgroundLight,
    onBackground = Color(0xFF1C1B1F),

    surface = SurfaceLight,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF534341),

    outline = OutlineLight,
    outlineVariant = Color(0xFFD8C2BE),

    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = OrangeConstructionLight
)

// ---- Esquema oscuro ----
private val DarkColorScheme = darkColorScheme(
    primary = OrangeConstructionLight,
    onPrimary = Color(0xFF5C1700),
    primaryContainer = OrangeConstructionDark,
    onPrimaryContainer = OrangeContainer,

    secondary = SteelGrayContainer,
    onSecondary = Color(0xFF0D1D23),
    secondaryContainer = SteelGray,
    onSecondaryContainer = SteelGrayContainer,

    tertiary = SafetyYellowLight,
    onTertiary = Color(0xFF3E2700),
    tertiaryContainer = SafetyYellowDark,
    onTertiaryContainer = Color(0xFFFFEFBE),

    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),

    background = BackgroundDark,
    onBackground = Color(0xFFE6E1E5),

    surface = SurfaceDark,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFD8C2BE),

    outline = OutlineDark,
    outlineVariant = Color(0xFF534341),

    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = OrangeConstruction
)

/**
 * Tema principal de ConstruApp.
 *
 * @param darkTheme usar esquema oscuro (por defecto sigue la configuración del sistema)
 * @param dynamicColor usar colores dinámicos de Android 12+ (desactivado por defecto para
 *                     mantener la identidad visual de la marca)
 */
@Composable
fun ConstruAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
