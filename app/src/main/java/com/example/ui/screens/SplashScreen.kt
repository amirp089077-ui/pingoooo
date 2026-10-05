package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AlphaBrandTitle
import com.example.ui.components.AlphaLogo
import com.example.ui.components.DarkThemeBackground
import com.example.ui.theme.AlphaDotBlue
import com.example.ui.theme.AlphaDotCyan
import com.example.ui.theme.AlphaDotOrange
import com.example.ui.theme.LocalAlphaAppColors
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    language: AppLanguage,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    autoNavigateDelayMs: Long = 2600L
) {
    val colors = LocalAlphaAppColors.current

    LaunchedEffect(Unit) {
        if (autoNavigateDelayMs > 0) {
            delay(autoNavigateDelayMs)
            onNavigateToLogin()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    val isRtl = language == AppLanguage.PERSIAN
    CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        DarkThemeBackground(
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNavigateToLogin
                )
                .testTag("splash_screen_root")
        ) {
            // Main Centered Content: Logo + Brand + Tagline
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Circular 3D Alpha Logo Emblem (~185dp)
                Box(modifier = Modifier.scale(pulseScale)) {
                    AlphaLogo(
                        size = 185.dp,
                        animated = true,
                        modifier = Modifier.testTag("splash_logo")
                    )
                }

                Spacer(modifier = Modifier.height(34.dp))

                // Brand Title: "ALPHA VPN"
                AlphaBrandTitle(
                    fontSize = 28.sp,
                    letterSpacing = 1.8.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Slogan (Dynamic per language)
                Text(
                    text = AppStrings.slogan(language),
                    color = colors.textSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("splash_slogan")
                )
            }

            // Bottom Section: 3-dot indicator (Orange, Cyan, Blue)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .testTag("splash_dot_indicators")
                            .clickable { onNavigateToLogin() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(AlphaDotOrange)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(AlphaDotCyan)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(AlphaDotBlue)
                        )
                    }
                }
            }
        }
    }
}
