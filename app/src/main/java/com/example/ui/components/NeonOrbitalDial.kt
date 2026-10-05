package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAlphaAppColors
import com.example.ui.viewmodel.VpnStatus
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NeonOrbitalDial(
    vpnStatus: VpnStatus,
    onToggleConnection: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp
) {
    val colors = LocalAlphaAppColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "dial_anim")

    // Rotation speed: fast when connected/connecting, calm when disconnected
    val orbitDuration = when (vpnStatus) {
        VpnStatus.CONNECTED -> 7000
        VpnStatus.CONNECTING -> 2500
        VpnStatus.DISCONNECTED -> 24000
    }

    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(orbitDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    // Pulse glow animation
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = if (vpnStatus == VpnStatus.DISCONNECTED) 0.6f else 0.85f,
        targetValue = if (vpnStatus == VpnStatus.DISCONNECTED) 0.85f else 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (vpnStatus == VpnStatus.CONNECTING) 600 else 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    // Sonar Ripple wave for Connecting/Connected states
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_progress"
    )

    // Animated Accent Colors based on state:
    // Connected: Emerald Green (#10B981) + Neon Cyan (#00E5FF)
    // Connecting: Radiant Amber (#F59E0B) + Orange (#FF6B35)
    // Disconnected: Slate / Dim Steel (#64748B / #94A3B8)
    val primaryAuraColor by animateColorAsState(
        targetValue = when (vpnStatus) {
            VpnStatus.CONNECTED -> Color(0xFF10B981) // Radiant Emerald Green!
            VpnStatus.CONNECTING -> Color(0xFFF59E0B) // Amber
            VpnStatus.DISCONNECTED -> if (colors.isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
        },
        animationSpec = tween(600),
        label = "primary_color"
    )

    val secondaryAuraColor by animateColorAsState(
        targetValue = when (vpnStatus) {
            VpnStatus.CONNECTED -> Color(0xFF00E5FF) // Electric Cyan
            VpnStatus.CONNECTING -> Color(0xFFFF6B35) // Neon Orange
            VpnStatus.DISCONNECTED -> if (colors.isDark) Color(0xFF475569) else Color(0xFF94A3B8)
        },
        animationSpec = tween(600),
        label = "secondary_color"
    )

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleConnection
            )
            .testTag("neon_orbital_dial"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val sphereRadius = w * 0.36f

            // 0. Sonar Expanding Ripple Waves (Connecting or Connected state)
            if (vpnStatus != VpnStatus.DISCONNECTED) {
                val rippleRadius = sphereRadius + (w * 0.16f * rippleProgress)
                val rippleAlpha = (1f - rippleProgress) * (if (vpnStatus == VpnStatus.CONNECTING) 0.5f else 0.35f)
                drawCircle(
                    color = primaryAuraColor.copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Secondary offset ripple
                val ripple2Progress = (rippleProgress + 0.5f) % 1f
                val ripple2Radius = sphereRadius + (w * 0.16f * ripple2Progress)
                val ripple2Alpha = (1f - ripple2Progress) * (if (vpnStatus == VpnStatus.CONNECTING) 0.4f else 0.25f)
                drawCircle(
                    color = secondaryAuraColor.copy(alpha = ripple2Alpha),
                    radius = ripple2Radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 1. Orbital Ring 1
            val orbit1Radius = w * 0.47f
            drawCircle(
                color = if (vpnStatus == VpnStatus.CONNECTED) Color(0x3310B981) else colors.dialTrack,
                radius = orbit1Radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            val angle1Rad = Math.toRadians(orbitAngle.toDouble())
            val dot1X = center.x + orbit1Radius * cos(angle1Rad).toFloat()
            val dot1Y = center.y + orbit1Radius * sin(angle1Rad).toFloat()
            drawCircle(
                color = if (vpnStatus == VpnStatus.CONNECTED) Color(0xFF6EE7B7) else colors.textMuted,
                radius = 3.8.dp.toPx(),
                center = Offset(dot1X, dot1Y)
            )
            drawCircle(
                color = primaryAuraColor.copy(alpha = 0.5f),
                radius = 7.dp.toPx(),
                center = Offset(dot1X, dot1Y)
            )

            // 2. Orbital Ring 2
            val orbit2Radius = w * 0.42f
            drawCircle(
                color = if (vpnStatus == VpnStatus.CONNECTED) Color(0x3300E5FF) else colors.dialTrack.copy(alpha = 0.6f),
                radius = orbit2Radius,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
            val angle2Rad = Math.toRadians((orbitAngle * 1.6 + 180).toDouble())
            val dot2X = center.x + orbit2Radius * cos(angle2Rad).toFloat()
            val dot2Y = center.y + orbit2Radius * sin(angle2Rad).toFloat()
            drawCircle(
                color = secondaryAuraColor,
                radius = 3.dp.toPx(),
                center = Offset(dot2X, dot2Y)
            )

            // 3. Ambient Glow Halo behind the Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryAuraColor.copy(alpha = (if (vpnStatus == VpnStatus.CONNECTED) 0.45f else 0.15f) * pulseGlow),
                        secondaryAuraColor.copy(alpha = (if (vpnStatus == VpnStatus.CONNECTED) 0.25f else 0.08f) * pulseGlow),
                        Color.Transparent
                    ),
                    center = center,
                    radius = sphereRadius * 1.4f
                ),
                radius = sphereRadius * 1.4f,
                center = center
            )

            // 4. Glass Sphere Body
            val sphereBg = if (colors.isDark) {
                when (vpnStatus) {
                    VpnStatus.CONNECTED -> Brush.radialGradient(
                        0.0f to Color(0x55062C24),
                        0.7f to Color(0x99051E19),
                        1.0f to Color(0xDD02100E),
                        center = center,
                        radius = sphereRadius
                    )
                    VpnStatus.CONNECTING -> Brush.radialGradient(
                        0.0f to Color(0x55301808),
                        0.7f to Color(0x99201005),
                        1.0f to Color(0xDD140A03),
                        center = center,
                        radius = sphereRadius
                    )
                    VpnStatus.DISCONNECTED -> Brush.radialGradient(
                        0.0f to Color(0x331E293B),
                        0.7f to Color(0x660F172A),
                        1.0f to Color(0xCC020617),
                        center = center,
                        radius = sphereRadius
                    )
                }
            } else {
                when (vpnStatus) {
                    VpnStatus.CONNECTED -> Brush.radialGradient(
                        0.0f to Color(0xFFFFFFFF),
                        0.65f to Color(0xFFE6FBF5),
                        1.0f to Color(0xFFC7F7E8),
                        center = center,
                        radius = sphereRadius
                    )
                    VpnStatus.CONNECTING -> Brush.radialGradient(
                        0.0f to Color(0xFFFFFFFF),
                        0.65f to Color(0xFFFFF4EC),
                        1.0f to Color(0xFFFFDEC9),
                        center = center,
                        radius = sphereRadius
                    )
                    VpnStatus.DISCONNECTED -> Brush.radialGradient(
                        0.0f to Color(0xFFFFFFFF),
                        0.65f to Color(0xFFF1F5F9),
                        1.0f to Color(0xFFE2E8F0),
                        center = center,
                        radius = sphereRadius
                    )
                }
            }

            drawCircle(
                brush = sphereBg,
                radius = sphereRadius,
                center = center
            )

            // 5. Dual-Color Outer Rim
            // Left rim: Primary (Emerald Green when connected, Amber when connecting, Slate when disconnected)
            drawArc(
                brush = Brush.verticalGradient(
                    listOf(primaryAuraColor, primaryAuraColor.copy(alpha = 0.7f)),
                    startY = center.y - sphereRadius,
                    endY = center.y + sphereRadius
                ),
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - sphereRadius, center.y - sphereRadius),
                size = androidx.compose.ui.geometry.Size(sphereRadius * 2f, sphereRadius * 2f),
                style = Stroke(
                    width = (if (vpnStatus == VpnStatus.CONNECTED) 3.dp else 2.2.dp).toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Right rim: Secondary (Electric Cyan when connected, Orange when connecting)
            drawArc(
                brush = Brush.verticalGradient(
                    listOf(secondaryAuraColor, secondaryAuraColor.copy(alpha = 0.7f)),
                    startY = center.y - sphereRadius,
                    endY = center.y + sphereRadius
                ),
                startAngle = 270f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - sphereRadius, center.y - sphereRadius),
                size = androidx.compose.ui.geometry.Size(sphereRadius * 2f, sphereRadius * 2f),
                style = Stroke(
                    width = (if (vpnStatus == VpnStatus.CONNECTED) 3.dp else 2.2.dp).toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Specular Inner Ring
            drawCircle(
                color = if (colors.isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.7f),
                radius = sphereRadius - 2.5.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 6. Stylized Center 'A' Logo
            drawNeonALogoWithState(
                center = center,
                sphereRadius = sphereRadius,
                vpnStatus = vpnStatus,
                pulse = pulseGlow,
                primaryColor = primaryAuraColor,
                secondaryColor = secondaryAuraColor,
                isDark = colors.isDark
            )
        }
    }
}

private fun DrawScope.drawNeonALogoWithState(
    center: Offset,
    sphereRadius: Float,
    vpnStatus: VpnStatus,
    pulse: Float,
    primaryColor: Color,
    secondaryColor: Color,
    isDark: Boolean
) {
    val cx = center.x
    val cy = center.y
    val scale = sphereRadius / 100f

    val isConnected = vpnStatus == VpnStatus.CONNECTED
    val isConnecting = vpnStatus == VpnStatus.CONNECTING

    val archColor = if (isConnected) primaryColor else if (isConnecting) primaryColor else Color(0xFF64748B)
    val wingsColor = if (isConnected) secondaryColor else if (isConnecting) secondaryColor else Color(0xFF94A3B8)

    // Outer Bloom Glow
    val bloomCyan = Path().apply {
        moveTo(cx - 38f * scale, cy + 32f * scale)
        lineTo(cx, cy - 42f * scale)
        lineTo(cx + 38f * scale, cy + 32f * scale)
    }
    drawPath(
        path = bloomCyan,
        color = archColor.copy(alpha = (if (isConnected) 0.45f else 0.18f) * pulse),
        style = Stroke(width = 15f * scale, cap = StrokeCap.Round)
    )

    // Main Arch of 'A'
    val outerCyanArch = Path().apply {
        moveTo(cx - 36f * scale, cy + 30f * scale)
        cubicTo(
            cx - 28f * scale, cy + 6f * scale,
            cx - 18f * scale, cy - 24f * scale,
            cx, cy - 40f * scale
        )
        cubicTo(
            cx + 18f * scale, cy - 24f * scale,
            cx + 28f * scale, cy + 6f * scale,
            cx + 36f * scale, cy + 30f * scale
        )
    }

    drawPath(
        path = outerCyanArch,
        brush = Brush.verticalGradient(
            listOf(Color.White, archColor),
            startY = cy - 40f * scale,
            endY = cy + 30f * scale
        ),
        style = Stroke(width = 4.8f * scale, cap = StrokeCap.Round)
    )

    drawPath(
        path = outerCyanArch,
        color = if (isConnected) Color(0xFFE6FFFA) else Color.White,
        style = Stroke(width = 1.8f * scale, cap = StrokeCap.Round)
    )

    // Inner Wings of 'A'
    val orangeWings = Path().apply {
        moveTo(cx - 35f * scale, cy - 6f * scale)
        lineTo(cx, cy + 34f * scale)
        lineTo(cx + 35f * scale, cy - 6f * scale)
    }

    drawPath(
        path = orangeWings,
        color = wingsColor.copy(alpha = (if (isConnected) 0.45f else 0.2f) * pulse),
        style = Stroke(width = 13f * scale, cap = StrokeCap.Round)
    )

    drawPath(
        path = orangeWings,
        brush = Brush.verticalGradient(
            listOf(Color.White, wingsColor),
            startY = cy - 6f * scale,
            endY = cy + 34f * scale
        ),
        style = Stroke(width = 4.4f * scale, cap = StrokeCap.Round)
    )

    drawPath(
        path = orangeWings,
        color = Color(0xFFFFFDF5),
        style = Stroke(width = 1.6f * scale, cap = StrokeCap.Round)
    )
}
