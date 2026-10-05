package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AlphaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    animated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val pulse by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        rememberInfiniteTransition(label = "static").animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000)),
            label = "static"
        )
    }

    Box(
        modifier = modifier
            .size(size * pulse)
            .testTag("alpha_vpn_logo"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val radius = (w / 2f) * 0.92f

            // 1. Soft ambient shadow behind emblem
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x220090B8),
                        Color(0x10E05326),
                        Color.Transparent
                    ),
                    center = center,
                    radius = w * 0.5f
                ),
                radius = w * 0.5f,
                center = center
            )

            // 2. Main circle background with radial gradient
            val circlePath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center.x - radius,
                        center.y - radius,
                        center.x + radius,
                        center.y + radius
                    )
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFFFFFFFF),
                    0.55f to Color(0xFFF3FAFC),
                    0.85f to Color(0xFFD6EFF6),
                    1.0f to Color(0xFFBFDFEB),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // 3. Dual-tone outer perimeter ring (Left: Cyan/Blue, Right: Orange/Coral)
            val ringStrokeWidth = w * 0.016f

            // Left side: Cyan to Blue arc (from 90° down through 180° to 270° up)
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF38BDF8),
                        Color(0xFF0090B8),
                        Color(0xFF0284C7)
                    ),
                    startY = center.y - radius,
                    endY = center.y + radius
                ),
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = ringStrokeWidth, cap = StrokeCap.Round)
            )

            // Right side: Orange to Amber arc (from 270° down through 0° to 90°)
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF7A45),
                        Color(0xFFE05326),
                        Color(0xFFD34015)
                    ),
                    startY = center.y - radius,
                    endY = center.y + radius
                ),
                startAngle = 270f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = ringStrokeWidth, cap = StrokeCap.Round)
            )

            // Subtle inner glow rim
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = radius - ringStrokeWidth,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 4. Stylized 3D Alpha Emblem 'A'
            clipPath(circlePath) {
                drawAlphaRibbonGlyph(w, h, center)
            }
        }
    }
}

private fun DrawScope.drawAlphaRibbonGlyph(w: Float, h: Float, center: Offset) {
    val scale = w / 200f
    val cx = center.x
    val cy = center.y

    // Deep drop shadow under the 3D ribbons
    val shadowPath = Path().apply {
        moveTo(cx - 45f * scale, cy + 25f * scale)
        quadraticTo(cx, cy + 38f * scale, cx + 45f * scale, cy + 25f * scale)
        quadraticTo(cx + 25f * scale, cy + 42f * scale, cx, cy + 44f * scale)
        quadraticTo(cx - 25f * scale, cy + 42f * scale, cx - 45f * scale, cy + 25f * scale)
        close()
    }
    drawPath(shadowPath, color = Color(0x30004866))

    // -------------------------------------------------------------
    // LAYER A: Cyan ribbon back folds & loops
    // -------------------------------------------------------------
    // Left Leg Cyan ribbon
    val leftLegCyan = Path().apply {
        moveTo(cx - 7f * scale, cy - 38f * scale)
        cubicTo(
            cx - 20f * scale, cy - 15f * scale,
            cx - 40f * scale, cy + 5f * scale,
            cx - 52f * scale, cy + 28f * scale
        )
        cubicTo(
            cx - 55f * scale, cy + 34f * scale,
            cx - 48f * scale, cy + 39f * scale,
            cx - 38f * scale, cy + 36f * scale
        )
        cubicTo(
            cx - 26f * scale, cy + 32f * scale,
            cx - 16f * scale, cy + 18f * scale,
            cx - 8f * scale, cy - 5f * scale
        )
        close()
    }

    drawPath(
        path = leftLegCyan,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF38BDF8),
                Color(0xFF0090B8),
                Color(0xFF006688)
            ),
            start = Offset(cx - 7f * scale, cy - 38f * scale),
            end = Offset(cx - 52f * scale, cy + 38f * scale)
        )
    )

    // Right Leg Cyan ribbon
    val rightLegCyan = Path().apply {
        moveTo(cx + 7f * scale, cy - 38f * scale)
        cubicTo(
            cx + 20f * scale, cy - 15f * scale,
            cx + 40f * scale, cy + 5f * scale,
            cx + 52f * scale, cy + 28f * scale
        )
        cubicTo(
            cx + 55f * scale, cy + 34f * scale,
            cx + 48f * scale, cy + 39f * scale,
            cx + 38f * scale, cy + 36f * scale
        )
        cubicTo(
            cx + 26f * scale, cy + 32f * scale,
            cx + 16f * scale, cy + 18f * scale,
            cx + 8f * scale, cy - 5f * scale
        )
        close()
    }

    drawPath(
        path = rightLegCyan,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF7DD3FC),
                Color(0xFF0284C7),
                Color(0xFF084F6E)
            ),
            start = Offset(cx + 7f * scale, cy - 38f * scale),
            end = Offset(cx + 52f * scale, cy + 38f * scale)
        )
    )

    // Cyan Apex Cap & Inner Arch
    val cyanApex = Path().apply {
        moveTo(cx, cy - 42f * scale)
        cubicTo(
            cx - 12f * scale, cy - 42f * scale,
            cx - 18f * scale, cy - 25f * scale,
            cx - 9f * scale, cy - 8f * scale
        )
        cubicTo(
            cx - 3f * scale, cy - 14f * scale,
            cx + 3f * scale, cy - 14f * scale,
            cx + 9f * scale, cy - 8f * scale
        )
        cubicTo(
            cx + 18f * scale, cy - 25f * scale,
            cx + 12f * scale, cy - 42f * scale,
            cx, cy - 42f * scale
        )
        close()
    }

    drawPath(
        path = cyanApex,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFBAE6FD),
                Color(0xFF0090B8),
                Color(0xFF005575)
            ),
            startY = cy - 44f * scale,
            endY = cy - 8f * scale
        )
    )

    // Inner fold highlights of Cyan arch
    val cyanInnerFoldLeft = Path().apply {
        moveTo(cx - 38f * scale, cy + 36f * scale)
        cubicTo(
            cx - 28f * scale, cy + 32f * scale,
            cx - 18f * scale, cy + 15f * scale,
            cx - 8f * scale, cy - 6f * scale
        )
        cubicTo(
            cx - 14f * scale, cy + 8f * scale,
            cx - 26f * scale, cy + 22f * scale,
            cx - 34f * scale, cy + 28f * scale
        )
        close()
    }
    drawPath(
        path = cyanInnerFoldLeft,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFE0F2FE), Color(0xFF00B4D8)),
            start = Offset(cx - 8f * scale, cy - 6f * scale),
            end = Offset(cx - 38f * scale, cy + 36f * scale)
        )
    )

    val cyanInnerFoldRight = Path().apply {
        moveTo(cx + 38f * scale, cy + 36f * scale)
        cubicTo(
            cx + 28f * scale, cy + 32f * scale,
            cx + 18f * scale, cy + 15f * scale,
            cx + 8f * scale, cy - 6f * scale
        )
        cubicTo(
            cx + 14f * scale, cy + 8f * scale,
            cx + 26f * scale, cy + 22f * scale,
            cx + 34f * scale, cy + 28f * scale
        )
        close()
    }
    drawPath(
        path = cyanInnerFoldRight,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFBAE6FD), Color(0xFF0284C7)),
            start = Offset(cx + 8f * scale, cy - 6f * scale),
            end = Offset(cx + 38f * scale, cy + 36f * scale)
        )
    )

    // -------------------------------------------------------------
    // LAYER B: Vibrant Orange Winged Crossing Ribbon
    // -------------------------------------------------------------
    // The Orange wings form an inverted chevron crossing the waist of the 'A'
    // with aerodynamic flair and tips curving upward!
    
    // Left Wing (Orange Ribbon)
    val leftWingOrange = Path().apply {
        moveTo(cx, cy + 16f * scale)
        cubicTo(
            cx - 15f * scale, cy + 12f * scale,
            cx - 38f * scale, cy - 4f * scale,
            cx - 48f * scale, cy - 18f * scale
        )
        cubicTo(
            cx - 46f * scale, cy - 22f * scale,
            cx - 42f * scale, cy - 20f * scale,
            cx - 32f * scale, cy - 8f * scale
        )
        cubicTo(
            cx - 20f * scale, cy + 2f * scale,
            cx - 10f * scale, cy + 6f * scale,
            cx, cy + 5f * scale
        )
        close()
    }

    drawPath(
        path = leftWingOrange,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF7A45),
                Color(0xFFE05326),
                Color(0xFF9A2C08)
            ),
            start = Offset(cx - 48f * scale, cy - 18f * scale),
            end = Offset(cx, cy + 16f * scale)
        )
    )

    // Right Wing (Orange Ribbon)
    val rightWingOrange = Path().apply {
        moveTo(cx, cy + 16f * scale)
        cubicTo(
            cx + 15f * scale, cy + 12f * scale,
            cx + 38f * scale, cy - 4f * scale,
            cx + 48f * scale, cy - 18f * scale
        )
        cubicTo(
            cx + 46f * scale, cy - 22f * scale,
            cx + 42f * scale, cy - 20f * scale,
            cx + 32f * scale, cy - 8f * scale
        )
        cubicTo(
            cx + 20f * scale, cy + 2f * scale,
            cx + 10f * scale, cy + 6f * scale,
            cx, cy + 5f * scale
        )
        close()
    }

    drawPath(
        path = rightWingOrange,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF9E66),
                Color(0xFFE05326),
                Color(0xFFB5340B)
            ),
            start = Offset(cx + 48f * scale, cy - 18f * scale),
            end = Offset(cx, cy + 16f * scale)
        )
    )

    // Central crossing knot of the orange ribbon
    val orangeCenterKnot = Path().apply {
        moveTo(cx - 14f * scale, cy + 8f * scale)
        cubicTo(
            cx - 8f * scale, cy + 18f * scale,
            cx + 8f * scale, cy + 18f * scale,
            cx + 14f * scale, cy + 8f * scale
        )
        cubicTo(
            cx + 10f * scale, cy + 2f * scale,
            cx - 10f * scale, cy + 2f * scale,
            cx - 14f * scale, cy + 8f * scale
        )
        close()
    }

    drawPath(
        path = orangeCenterKnot,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFF9559),
                Color(0xFFE05326),
                Color(0xFFC2410C)
            ),
            startY = cy + 2f * scale,
            endY = cy + 18f * scale
        )
    )

    // Specular highlight ridges on the orange wings
    val orangeHighlight = Path().apply {
        moveTo(cx - 45f * scale, cy - 16f * scale)
        cubicTo(
            cx - 30f * scale, cy - 4f * scale,
            cx - 15f * scale, cy + 7f * scale,
            cx, cy + 9f * scale
        )
        cubicTo(
            cx + 15f * scale, cy + 7f * scale,
            cx + 30f * scale, cy - 4f * scale,
            cx + 45f * scale, cy - 16f * scale
        )
    }

    drawPath(
        path = orangeHighlight,
        color = Color(0x99FFFFFF),
        style = Stroke(width = 1.8f * scale, cap = StrokeCap.Round)
    )

    // Specular glossy reflection on the top Cyan arch
    val cyanApexGloss = Path().apply {
        moveTo(cx - 8f * scale, cy - 36f * scale)
        cubicTo(
            cx - 4f * scale, cy - 40f * scale,
            cx + 4f * scale, cy - 40f * scale,
            cx + 8f * scale, cy - 36f * scale
        )
    }
    drawPath(
        path = cyanApexGloss,
        color = Color(0xCCFFFFFF),
        style = Stroke(width = 2.2f * scale, cap = StrokeCap.Round)
    )
}
