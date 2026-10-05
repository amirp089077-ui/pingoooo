package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAlphaAppColors

@Composable
fun CircularHeaderButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isSpinning: Boolean = false,
    spinAngle: Float = 0f,
    onClick: () -> Unit = {}
) {
    val colors = LocalAlphaAppColors.current
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(colors.cardBg)
            .border(1.dp, colors.cardBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier
                .size(20.dp)
                .rotate(if (isSpinning) spinAngle else 0f)
        )
    }
}
