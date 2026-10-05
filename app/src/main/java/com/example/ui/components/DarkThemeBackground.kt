package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LocalAlphaAppColors

@Composable
fun DarkThemeBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalAlphaAppColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgEnd)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Overall base vertical gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.bgStart,
                        colors.bgMiddle,
                        colors.bgEnd
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // 2. Ambient top-left glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (colors.isDark) Color(0x282DD4BF) else Color(0x180090B8),
                        if (colors.isDark) Color(0x10081D22) else Color(0x08F0F8FB),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.15f, h * 0.25f),
                    radius = w * 0.75f
                ),
                center = Offset(w * 0.15f, h * 0.25f),
                radius = w * 0.75f
            )

            // 3. Ambient right-side glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (colors.isDark) Color(0x18FF6B35) else Color(0x10E05326),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.95f, h * 0.45f),
                    radius = w * 0.6f
                ),
                center = Offset(w * 0.95f, h * 0.45f),
                radius = w * 0.6f
            )
        }

        content()
    }
}
