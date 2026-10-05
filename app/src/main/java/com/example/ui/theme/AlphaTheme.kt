package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val title: String) {
    DARK("تیره"),
    LIGHT("روشن"),
    AUTO("خودکار")
}

@Immutable
data class AlphaAppColors(
    val isDark: Boolean,
    val bgStart: Color,
    val bgMiddle: Color,
    val bgEnd: Color,
    val cardBg: Color,
    val cardBgSecondary: Color,
    val cardBorder: Color,
    val cardBorderGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentTeal: Color,
    val accentCyan: Color,
    val accentOrange: Color,
    val accentOrangeDeep: Color,
    val navBg: Color,
    val navBorder: Color,
    val dialTrack: Color,
    val shadowColor: Color
)

val LightAlphaColors = AlphaAppColors(
    isDark = false,
    bgStart = Color(0xFFEBF4F6),
    bgMiddle = Color(0xFFF8FBFC),
    bgEnd = Color(0xFFFFFFFF),
    cardBg = Color(0xFFFFFFFF),
    cardBgSecondary = Color(0xFFF1F7F9),
    cardBorder = Color(0xFFE2E8F0),
    cardBorderGlow = Color(0x330090B8),
    textPrimary = Color(0xFF1E293B),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF8E9AA8),
    accentTeal = Color(0xFF0090B8),
    accentCyan = Color(0xFF0284C7),
    accentOrange = Color(0xFFE05326),
    accentOrangeDeep = Color(0xFFC2410C),
    navBg = Color(0xF2FFFFFF),
    navBorder = Color(0xFFCCE4ED),
    dialTrack = Color(0x260090B8),
    shadowColor = Color(0x1A004664)
)

val DarkAlphaColors = AlphaAppColors(
    isDark = true,
    bgStart = Color(0xFF081D22),
    bgMiddle = Color(0xFF051014),
    bgEnd = Color(0xFF020608),
    cardBg = Color(0xD90D1F25),
    cardBgSecondary = Color(0xEB12262C),
    cardBorder = Color(0xFF14353D),
    cardBorderGlow = Color(0x332DD4BF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    accentTeal = Color(0xFF2DD4BF),
    accentCyan = Color(0xFF00E5FF),
    accentOrange = Color(0xFFFF6B35),
    accentOrangeDeep = Color(0xFFFF5722),
    navBg = Color(0xF20F232A),
    navBorder = Color(0x402DD4BF),
    dialTrack = Color(0x3314353D),
    shadowColor = Color(0x4D000000)
)

val LocalAlphaAppColors = staticCompositionLocalOf { DarkAlphaColors }

@Composable
fun ProvideAlphaColors(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> isSystemDark
    }

    val colors = if (isDark) DarkAlphaColors else LightAlphaColors

    CompositionLocalProvider(LocalAlphaAppColors provides colors) {
        content()
    }
}
