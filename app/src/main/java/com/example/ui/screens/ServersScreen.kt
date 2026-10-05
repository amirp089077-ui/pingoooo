package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
fun ServersScreen(
    viewModel: AlphaVpnViewModel,
    onServerSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAlphaAppColors.current
    val lang = viewModel.appLanguage

    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing)
        ),
        label = "spin"
    )

    var expandedCountries by remember { mutableStateOf(setOf("آلمان", "انگلیس", "سوئد", "فرانسه", "اسپانیا")) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val vpnPrepareLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.startRealVpn(context)
        }
    }

    val connectToServer: (com.example.ui.viewmodel.AppServer, String) -> Unit = { server, label ->
        viewModel.selectServerAndConnect(server, context)
        val prepareIntent = android.net.VpnService.prepare(context)
        if (prepareIntent != null) {
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            viewModel.startRealVpn(context)
        }
        onServerSelected(label)
    }

    val connectToSmartServer: () -> Unit = {
        val chosen = viewModel.selectSmartServerAndConnect(context)
        val prepareIntent = android.net.VpnService.prepare(context)
        if (prepareIntent != null) {
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            viewModel.startRealVpn(context)
        }
        onServerSelected("${chosen.country} - ${chosen.city}")
    }

    // Dynamic servers strictly grouped from Firebase
    val groupedServers = remember(viewModel.serverList) {
        viewModel.serverList.groupBy { it.country }
    }

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
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = AppStrings.servers(lang),
                            color = colors.textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = VazirmatnFontFamily
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (viewModel.serverList.isNotEmpty()) {
                                AppStrings.serversAvailable(viewModel.serverList.size, lang)
                            } else {
                                if (lang == AppLanguage.PERSIAN) "در حال اتصال به فایربیس..." else "Connecting to Firebase..."
                            },
                            color = colors.textMuted,
                            fontSize = 13.sp,
                            fontFamily = VazirmatnFontFamily
                        )
                    }

                    // Two circular action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularHeaderButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = "بازخوانی سرورها از فایربیس",
                            isSpinning = viewModel.isRefreshingPings || viewModel.isSyncingFirestore,
                            spinAngle = spinAngle,
                            onClick = {
                                viewModel.syncWithFirestore()
                                viewModel.refreshPings()
                            }
                        )
                        CircularHeaderButton(
                            icon = Icons.Default.NorthEast,
                            contentDescription = "مرتب‌سازی بر اساس پینگ",
                            onClick = { viewModel.sortServersBySpeed() }
                        )
                    }
                }

                // 1. Smart Server Card (Direct connect on click!)
                if (viewModel.serverList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.cardBg)
                            .border(
                                width = 1.2.dp,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        if (viewModel.isSmartServer) colors.accentTeal else colors.cardBorderGlow,
                                        if (viewModel.isSmartServer) colors.accentOrange else colors.cardBorderGlow
                                    )
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .clickable {
                                connectToSmartServer()
                            }
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                            .testTag("smart_server_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // AI Icon + Title & Subtitle
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(colors.accentTeal.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Smart Connect",
                                        tint = colors.accentTeal,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = AppStrings.smartServer(lang),
                                        color = colors.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = VazirmatnFontFamily
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = AppStrings.smartServerDesc(lang),
                                        color = colors.textMuted,
                                        fontSize = 12.sp,
                                        fontFamily = VazirmatnFontFamily
                                    )
                                }
                            }

                            // Radio Selection Circle
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        if (viewModel.isSmartServer) colors.accentTeal else colors.textMuted,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (viewModel.isSmartServer) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(colors.accentTeal)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 2. Loading State from Firebase if empty
                if (viewModel.serverList.isEmpty()) {
                    DarkGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (viewModel.isSyncingFirestore) {
                                CircularProgressIndicator(
                                    color = colors.accentTeal,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN)
                                        "در حال دریافت سرورها از فایربیس..."
                                    else
                                        "Loading servers from Firebase...",
                                    color = colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = VazirmatnFontFamily
                                )
                            } else {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN)
                                        "هنوز سروری از فایربیس دریافت نشده است"
                                    else
                                        "No servers loaded from Firebase",
                                    color = colors.textMuted,
                                    fontSize = 14.5.sp,
                                    fontFamily = VazirmatnFontFamily
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.syncWithFirestore() },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentTeal),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN) "بارگذاری مجدد" else "Reload",
                                        color = Color.White,
                                        fontFamily = VazirmatnFontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Dynamic Country Accordion Cards (Fetched Strictly from Firebase)
                groupedServers.forEach { (countryName, servers) ->
                    val isSingle = servers.size == 1
                    val firstServer = servers.first()
                    val isExpanded = expandedCountries.contains(countryName)
                    val isSingleChosen = isSingle && viewModel.selectedServer.id == firstServer.id && !viewModel.isSmartServer

                    DarkGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable {
                                if (isSingle) {
                                    connectToServer(firstServer, countryName)
                                } else {
                                    expandedCountries = if (isExpanded) {
                                        expandedCountries - countryName
                                    } else {
                                        expandedCountries + countryName
                                    }
                                }
                            }
                            .testTag("country_card_$countryName"),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Country Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularFlagIcon(flag = firstServer.flag, size = 38)
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = countryName,
                                                color = colors.textPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = VazirmatnFontFamily
                                            )
                                            if (firstServer.badge != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFFDC2626))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = firstServer.badge,
                                                        color = Color.White,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            if (firstServer.emoji.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = firstServer.emoji, fontSize = 13.sp)
                                            }
                                        }

                                        if (!isSingle) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${servers.size} ${if (lang == AppLanguage.PERSIAN) "لوکیشن" else "locations"}",
                                                color = colors.accentTeal,
                                                fontSize = 12.sp,
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${AppStrings.ping(lang)}: ${firstServer.ping}ms",
                                                color = colors.textMuted,
                                                fontSize = 11.5.sp,
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        }
                                    }
                                }

                                if (isSingle) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .border(
                                                2.dp,
                                                if (isSingleChosen) colors.accentTeal else colors.textMuted,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSingleChosen) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.accentTeal)
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "باز کردن لیست",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Sub-locations accordion dropdown (Direct Connect on click! NO COPY)
                            AnimatedVisibility(
                                visible = isExpanded && !isSingle,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    servers.forEach { loc ->
                                        val isChosen = viewModel.selectedServer.id == loc.id && !viewModel.isSmartServer
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isChosen) colors.accentTeal.copy(alpha = 0.18f) else colors.cardBgSecondary
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isChosen) colors.accentTeal else colors.cardBorder,
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    // User selects server -> DIRECTLY CONNECTS via real Android VpnService and navigates to Home!
                                                    connectToServer(loc, "${countryName} - ${loc.city}")
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = loc.city,
                                                    color = colors.textPrimary,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                                    fontFamily = VazirmatnFontFamily
                                                )
                                                Text(
                                                    text = "${AppStrings.ping(lang)}: ${loc.ping}ms",
                                                    color = colors.textMuted,
                                                    fontSize = 11.sp,
                                                    fontFamily = VazirmatnFontFamily
                                                )
                                            }

                                            if (isChosen) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(colors.accentTeal),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "متصل",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
