package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AlphaBrandTitle
import com.example.ui.components.AlphaLogo
import com.example.ui.components.DarkGlassCard
import com.example.ui.components.DarkThemeBackground
import com.example.ui.components.NeonOrbitalDial
import com.example.ui.theme.LocalAlphaAppColors
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import com.example.ui.viewmodel.AlphaVpnViewModel
import com.example.ui.viewmodel.VpnStatus

@Composable
fun HomeScreen(
    viewModel: AlphaVpnViewModel,
    onNavigateToServers: () -> Unit,
    onNavigateToSubscription: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalAlphaAppColors.current
    val lang = viewModel.appLanguage

    val isRtl = lang == AppLanguage.PERSIAN
    val context = androidx.compose.ui.platform.LocalContext.current
    val vpnPrepareLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.startRealVpn(context)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        DarkThemeBackground(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quota Exceeded Alert Dialog
                if (viewModel.isQuotaExceeded) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { viewModel.dismissQuotaExceeded() },
                        title = {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "اتمام حجم اشتراک" else "Subscription Quota Exceeded",
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        },
                        text = {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN)
                                    "حجم یا زمان اشتراک شما به پایان رسیده است. برای برقراری مجدد اتصال VPN، لطفاً اشتراک خود را تمدید فرمایید."
                                else
                                    "Your data quota or validity period has expired. Please renew your subscription to reconnect.",
                                fontFamily = VazirmatnFontFamily,
                                color = colors.textMuted
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    viewModel.dismissQuotaExceeded()
                                    onNavigateToSubscription()
                                }
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "تمدید اشتراک" else "Renew Now",
                                    fontFamily = VazirmatnFontFamily,
                                    color = colors.accentTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.dismissQuotaExceeded() }
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "بستن" else "Close",
                                    fontFamily = VazirmatnFontFamily,
                                    color = colors.textMuted
                                )
                            }
                        },
                        containerColor = colors.cardBg,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                // Top Header: Centered Logo + Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlphaLogo(size = 36.dp, animated = false)
                    Spacer(modifier = Modifier.width(10.dp))
                    AlphaBrandTitle(fontSize = 18.sp, letterSpacing = 1.sp)
                }

                if (viewModel.announcementText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.accentTeal.copy(alpha = 0.12f))
                            .border(1.dp, colors.accentTeal.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📢", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = viewModel.announcementText,
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Center Orbital Dial with emerald green & dynamic state animation
                NeonOrbitalDial(
                    vpnStatus = viewModel.vpnStatus,
                    onToggleConnection = {
                        if (viewModel.vpnStatus == VpnStatus.CONNECTED) {
                            viewModel.stopRealVpn(context)
                        } else {
                            val prepareIntent = android.net.VpnService.prepare(context)
                            if (prepareIntent != null) {
                                vpnPrepareLauncher.launch(prepareIntent)
                            } else {
                                viewModel.startRealVpn(context)
                            }
                        }
                    },
                    size = 230.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status Information Row with glowing indicator dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Pulsing Status Dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (viewModel.vpnStatus) {
                                    VpnStatus.CONNECTED -> Color(0xFF10B981) // Emerald Green!
                                    VpnStatus.CONNECTING -> Color(0xFFF59E0B) // Amber
                                    VpnStatus.DISCONNECTED -> Color(0xFF94A3B8) // Muted slate
                                }
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = when (viewModel.vpnStatus) {
                            VpnStatus.DISCONNECTED -> AppStrings.tapToConnect(lang)
                            VpnStatus.CONNECTING -> AppStrings.connecting(lang)
                            VpnStatus.CONNECTED -> AppStrings.connected(lang)
                        },
                        color = when (viewModel.vpnStatus) {
                            VpnStatus.DISCONNECTED -> colors.textMuted
                            VpnStatus.CONNECTING -> Color(0xFFF59E0B)
                            VpnStatus.CONNECTED -> Color(0xFF10B981) // Emerald Green!
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirmatnFontFamily,
                        modifier = Modifier.testTag("home_connection_status")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Timer
                Text(
                    text = formatTimer(
                        if (viewModel.vpnStatus == VpnStatus.CONNECTED) viewModel.connectionDurationSeconds else 0,
                        lang
                    ),
                    color = if (viewModel.vpnStatus == VpnStatus.CONNECTED) colors.textPrimary else colors.textMuted,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily,
                    letterSpacing = 1.sp,
                    modifier = Modifier.testTag("home_timer")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Live Speed telemetry banner when connected
                if (viewModel.vpnStatus == VpnStatus.CONNECTED) {
                    val dlSpeedStr = String.format("%.1f", viewModel.downloadSpeed)
                    val ulSpeedStr = String.format("%.1f", viewModel.uploadSpeed)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN)
                                "↓ $dlSpeedStr مگابایت/ث   ↑ $ulSpeedStr مگابایت/ث"
                            else
                                "↓ $dlSpeedStr MB/s   ↑ $ulSpeedStr MB/s",
                            color = Color(0xFF10B981),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = VazirmatnFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Server detail with [B] badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${viewModel.selectedServer.country} ${viewModel.selectedServer.city} ",
                        color = colors.textSecondary,
                        fontSize = 13.5.sp,
                        fontFamily = VazirmatnFontFamily
                    )
                    viewModel.selectedServer.badge?.let { badge ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDC2626))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "  •  ${AppStrings.ping(lang)} ${viewModel.selectedServer.ping}ms",
                        color = colors.textSecondary,
                        fontSize = 13.5.sp,
                        fontFamily = VazirmatnFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Data Usage
                val sessionUsageFormatted = String.format("%.1f", viewModel.sessionUsedMb)
                Text(
                    text = "${AppStrings.sessionUsage(lang)} $sessionUsageFormatted ${AppStrings.megabyte(lang)}",
                    color = colors.textMuted,
                    fontSize = 12.5.sp,
                    fontFamily = VazirmatnFontFamily
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Mini Quota Summary
                val remainingPercent = (viewModel.totalRemainingGb / viewModel.totalQuotaGb).coerceIn(0f, 1f)
                val remainingGbFormatted = String.format("%.1f", viewModel.totalRemainingGb)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Remaining Data
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { remainingPercent },
                                modifier = Modifier.size(34.dp),
                                color = colors.accentTeal,
                                strokeWidth = 3.5.dp,
                                trackColor = colors.dialTrack,
                                strokeCap = StrokeCap.Round
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "$remainingGbFormatted ${AppStrings.gigabyte(lang)}",
                                color = colors.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = VazirmatnFontFamily
                            )
                            Text(
                                text = AppStrings.remainingData(lang),
                                color = colors.textMuted,
                                fontSize = 11.5.sp,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }

                    // Separator
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(colors.cardBorder)
                    )

                    // Remaining Days
                    val daysPercent = (viewModel.remainingDays.toFloat() / viewModel.totalPurchasedDays).coerceIn(0f, 1f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { daysPercent },
                                modifier = Modifier.size(34.dp),
                                color = colors.accentOrange,
                                strokeWidth = 3.5.dp,
                                trackColor = colors.dialTrack,
                                strokeCap = StrokeCap.Round
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "${viewModel.remainingDays} ${AppStrings.days(lang)}",
                                color = colors.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = VazirmatnFontFamily
                            )
                            Text(
                                text = AppStrings.remainingTime(lang),
                                color = colors.textMuted,
                                fontSize = 11.5.sp,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Server Quick Selector Bar
                DarkGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable { onNavigateToServers() }
                        .testTag("server_quick_selector_bar"),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularFlagIcon(flag = viewModel.selectedServer.flag)

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = "${viewModel.selectedServer.country} ${viewModel.selectedServer.city} ",
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = VazirmatnFontFamily
                            )

                            viewModel.selectedServer.badge?.let { badge ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFDC2626))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badge,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Select Server",
                            tint = colors.textMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CircularFlagIcon(
    flag: String,
    modifier: Modifier = Modifier,
    size: Int = 36
) {
    val colors = LocalAlphaAppColors.current
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (colors.isDark) Color(0xFF14353D) else Color(0xFFE2E8F0)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = flag,
            fontSize = (size * 0.58).sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatTimer(totalSeconds: Int, lang: AppLanguage): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    val formatted = String.format("%02d:%02d:%02d", hrs, mins, secs)
    if (lang == AppLanguage.ENGLISH) {
        return formatted
    }
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val sb = StringBuilder()
    for (ch in formatted) {
        if (ch in '0'..'9') {
            sb.append(persianDigits[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}
