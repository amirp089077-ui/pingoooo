package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = AlphaPrimaryBlue,
    onPrimary = AlphaCardBg,
    primaryContainer = AlphaBgStart,
    onPrimaryContainer = AlphaPrimaryBlueDark,
    secondary = AlphaAccentOrange,
    onSecondary = AlphaCardBg,
    background = AlphaBgStart,
    onBackground = AlphaTextDark,
    surface = AlphaCardBg,
    onSurface = AlphaTextDark,
    surfaceVariant = AlphaBgMiddle,
    onSurfaceVariant = AlphaTextMuted,
    outline = AlphaFieldBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = AlphaBgStart.toArgb()
                window.navigationBarColor = AlphaBgEnd.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
