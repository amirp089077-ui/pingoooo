package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CircularHeaderButton
import com.example.ui.components.DarkGlassCard
import com.example.ui.components.DarkThemeBackground
import com.example.ui.theme.LocalAlphaAppColors
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import com.example.ui.viewmodel.AlphaVpnViewModel

@Composable
fun SubscriptionScreen(
    viewModel: AlphaVpnViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalAlphaAppColors.current
    val lang = viewModel.appLanguage

    var isGiftExpanded by remember { mutableStateOf(false) }
    var giftCodeInput by remember { mutableStateOf("") }
    var giftStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(false) }

    val remainingGbStr = String.format("%.1f", viewModel.totalRemainingGb)
    val usedGbStr = String.format("%.1f", viewModel.totalUsedGb)
    val totalQuotaStr = String.format("%.0f", viewModel.totalQuotaGb)

    val remainingProgress = (viewModel.totalRemainingGb / viewModel.totalQuotaGb).coerceIn(0f, 1f)
    val daysProgress = (viewModel.remainingDays.toFloat() / viewModel.totalPurchasedDays).coerceIn(0f, 1f)

    val isRtl = lang == AppLanguage.PERSIAN
    CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        DarkThemeBackground(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 110.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.mySubscription(lang),
                        color = colors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirmatnFontFamily
                    )

                    CircularHeaderButton(
                        icon = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        onClick = {
                            viewModel.syncWithFirestore()
                            giftStatusMessage = if (lang == AppLanguage.PERSIAN)
                                "اطلاعات با فایربیس همگام‌سازی شد" else "Account synced with Firebase"
                            isSuccessMessage = true
                        }
                    )
                }

                // Two Circular Progress Gauge Cards (Side-by-Side)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Right Card: Remaining Data
                    CircularGaugeCard(
                        title = remainingGbStr,
                        unit = AppStrings.gigabyte(lang),
                        subtitleTop = AppStrings.remainingData(lang),
                        subtitleBottom = "${AppStrings.of(lang)} $totalQuotaStr ${AppStrings.gigabyte(lang)}",
                        progress = remainingProgress,
                        gaugeColor = colors.accentTeal,
                        trackColor = colors.dialTrack,
                        modifier = Modifier.weight(1f)
                    )

                    // Left Card: Remaining Days
                    CircularGaugeCard(
                        title = "${viewModel.remainingDays}",
                        unit = AppStrings.days(lang),
                        subtitleTop = AppStrings.remainingTime(lang),
                        subtitleBottom = "${AppStrings.of(lang)} ${viewModel.totalPurchasedDays} ${AppStrings.days(lang)}",
                        progress = daysProgress,
                        gaugeColor = colors.accentOrange,
                        trackColor = colors.dialTrack,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Account Details Table Card
                DarkGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subscription_details_table"),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        SubscriptionTableRow(
                            label = AppStrings.planType(lang),
                            value = if (lang == AppLanguage.PERSIAN) viewModel.userPlanType.titleFa else viewModel.userPlanType.titleEn,
                            dotColor = colors.accentTeal
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            color = colors.cardBorder,
                            thickness = 1.dp
                        )

                        SubscriptionTableRow(
                            label = AppStrings.connectedDevices(lang),
                            value = "${viewModel.activeDevicesCount} / ${viewModel.userPlanType.maxDevices}",
                            dotColor = colors.accentTeal
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            color = colors.cardBorder,
                            thickness = 1.dp
                        )

                        SubscriptionTableRow(
                            label = AppStrings.totalUsage(lang),
                            value = "$usedGbStr ${AppStrings.gigabyte(lang)}",
                            dotColor = colors.accentTeal
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            color = colors.cardBorder,
                            thickness = 1.dp
                        )

                        SubscriptionTableRow(
                            label = AppStrings.totalQuota(lang),
                            value = "$totalQuotaStr ${AppStrings.gigabyte(lang)}",
                            dotColor = colors.accentTeal
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            color = colors.cardBorder,
                            thickness = 1.dp
                        )

                        SubscriptionTableRow(
                            label = AppStrings.purchasedDays(lang),
                            value = "${viewModel.totalPurchasedDays} ${AppStrings.days(lang)}",
                            dotColor = colors.accentTeal
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            color = colors.cardBorder,
                            thickness = 1.dp
                        )

                        SubscriptionTableRow(
                            label = AppStrings.remainingTime(lang),
                            value = "${viewModel.remainingDays} ${AppStrings.days(lang)}",
                            dotColor = colors.accentTeal
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            color = colors.cardBorder,
                            thickness = 1.dp
                        )

                        SubscriptionTableRow(
                            label = AppStrings.subscriptionExpiry(lang),
                            value = viewModel.expiryDate,
                            dotColor = colors.accentOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gift Code Box
                DarkGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gift_code_box"),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isGiftExpanded = !isGiftExpanded }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.accentOrange.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CardGiftcard,
                                        contentDescription = "Gift",
                                        tint = colors.accentOrange,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = AppStrings.haveGiftCode(lang),
                                    color = colors.textPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )
                            }

                            Icon(
                                imageVector = if (isGiftExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = colors.textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Expandable input field
                        AnimatedVisibility(
                            visible = isGiftExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = giftCodeInput,
                                        onValueChange = { giftCodeInput = it },
                                        placeholder = {
                                            Text(
                                                AppStrings.giftCodePlaceholder(lang),
                                                color = colors.textMuted,
                                                fontSize = 13.sp,
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.accentTeal,
                                            unfocusedBorderColor = colors.cardBorder,
                                            focusedTextColor = colors.textPrimary,
                                            unfocusedTextColor = colors.textPrimary
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Button(
                                        onClick = {
                                            viewModel.redeemGiftCode(giftCodeInput) { success, msg ->
                                                giftStatusMessage = msg
                                                isSuccessMessage = success
                                                if (success) giftCodeInput = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.accentTeal,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Text(
                                            AppStrings.apply(lang),
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = VazirmatnFontFamily
                                        )
                                    }
                                }

                                giftStatusMessage?.let { msg ->
                                    Text(
                                        text = msg,
                                        color = if (isSuccessMessage) colors.accentTeal else Color(0xFFEF4444),
                                        fontSize = 12.sp,
                                        fontFamily = VazirmatnFontFamily,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularGaugeCard(
    title: String,
    unit: String,
    subtitleTop: String,
    subtitleBottom: String,
    progress: Float,
    gaugeColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalAlphaAppColors.current

    DarkGlassCard(
        modifier = modifier.height(210.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 9.dp.toPx()

                    drawCircle(
                        color = trackColor,
                        radius = (size.width - strokeW) / 2f,
                        style = Stroke(width = strokeW)
                    )

                    drawArc(
                        color = gaugeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        color = colors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirmatnFontFamily
                    )
                    Text(
                        text = unit,
                        color = colors.textMuted,
                        fontSize = 12.5.sp,
                        fontFamily = VazirmatnFontFamily
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = subtitleTop,
                    color = colors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitleBottom,
                    color = colors.textMuted,
                    fontSize = 11.5.sp,
                    fontFamily = VazirmatnFontFamily,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SubscriptionTableRow(
    label: String,
    value: String,
    dotColor: Color
) {
    val colors = LocalAlphaAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = colors.textSecondary,
                fontSize = 13.5.sp,
                fontFamily = VazirmatnFontFamily
            )
        }

        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = VazirmatnFontFamily
        )
    }
}
