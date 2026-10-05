package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AlphaBrandTitle
import com.example.ui.components.TelegramSupportLink
import com.example.ui.theme.AlphaAccentOrange
import com.example.ui.theme.AlphaAccentOrangeDark
import com.example.ui.theme.AlphaBgEnd
import com.example.ui.theme.AlphaBgMiddle
import com.example.ui.theme.AlphaBgStart
import com.example.ui.theme.AlphaCardBg
import com.example.ui.theme.AlphaFieldBorder
import com.example.ui.theme.AlphaPrimaryBlue
import com.example.ui.theme.AlphaPrimaryBlueDark
import com.example.ui.theme.AlphaTextDark
import com.example.ui.theme.AlphaTextMuted
import com.example.ui.theme.VazirmatnFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class VpnServer(
    val id: String,
    val country: String,
    val city: String,
    val flag: String,
    val pingMs: Int,
    val isVip: Boolean = true
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    username: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onLogout()
    }

    var connectionState by remember { mutableStateOf(ConnectionState.CONNECTED) }
    var durationSeconds by remember { mutableIntStateOf(142) }
    var downloadSpeed by remember { mutableStateOf("18.4") }
    var uploadSpeed by remember { mutableStateOf("4.2") }
    var selectedServer by remember {
        mutableStateOf(
            VpnServer("de_fra", "آلمان", "فرانکفورت (سرعت بالا)", "🇩🇪", 42)
        )
    }
    var showServerSheet by remember { mutableStateOf(false) }
    var selectedProtocol by remember { mutableStateOf("V2Ray / VLESS") }

    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    // Timer when connected
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            while (true) {
                delay(1000)
                durationSeconds++
                // subtle random speed variation for realistic feedback
                downloadSpeed = String.format("%.1f", 16.0 + (Math.random() * 5.0))
                uploadSpeed = String.format("%.1f", 3.5 + (Math.random() * 1.8))
            }
        }
    }

    val toggleConnection: () -> Unit = {
        when (connectionState) {
            ConnectionState.DISCONNECTED -> {
                connectionState = ConnectionState.CONNECTING
                coroutineScope.launch {
                    delay(1500)
                    connectionState = ConnectionState.CONNECTED
                    durationSeconds = 0
                }
            }
            ConnectionState.CONNECTING -> {
                connectionState = ConnectionState.DISCONNECTED
            }
            ConnectionState.CONNECTED -> {
                connectionState = ConnectionState.DISCONNECTED
            }
        }
    }

    val serverList = listOf(
        VpnServer("de_fra", "آلمان", "فرانکفورت - اختصاصی", "🇩🇪", 42),
        VpnServer("nl_ams", "هلند", "آمستردام - پرسرعت", "🇳🇱", 48),
        VpnServer("fi_hel", "فنلاند", "هلسینکی - امنیتی", "🇫🇮", 55),
        VpnServer("tr_ist", "ترکیه", "استانبول - پینگ پایین", "🇹🇷", 34),
        VpnServer("gb_lon", "انگلستان", "لندن - بدون قطعی", "🇬🇧", 52),
        VpnServer("fr_par", "فرانسه", "پاریس - پهنای باند نامحدود", "🇫🇷", 49)
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AlphaBgStart, AlphaBgMiddle, AlphaBgEnd)
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("dashboard_root")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        AlphaBrandTitle(fontSize = 20.sp, letterSpacing = 1.sp)
                        Text(
                            text = "خوش آمدید، $username",
                            color = AlphaTextDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = VazirmatnFontFamily
                        )
                    }

                    // VIP Badge + Logout Button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "VIP ۳۰ روزه",
                                color = Color(0xFFB45309),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = VazirmatnFontFamily
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.testTag("dashboard_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                                contentDescription = "خروج",
                                tint = AlphaAccentOrangeDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Big VPN Connect/Disconnect Power Button with Pulsing Rings
                VpnPowerButton(
                    state = connectionState,
                    onClick = { toggleConnection() },
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Status Label
                Text(
                    text = when (connectionState) {
                        ConnectionState.DISCONNECTED -> "برای اتصال ضربه بزنید"
                        ConnectionState.CONNECTING -> "در حال اتصال به سرور..."
                        ConnectionState.CONNECTED -> "اتصال با موفقیت برقرار است"
                    },
                    color = when (connectionState) {
                        ConnectionState.DISCONNECTED -> AlphaTextMuted
                        ConnectionState.CONNECTING -> AlphaAccentOrange
                        ConnectionState.CONNECTED -> AlphaPrimaryBlue
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily,
                    modifier = Modifier.testTag("connection_status_text")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Telemetry Cards: Download, Upload, Ping, Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryCard(
                        title = "دانلود",
                        value = if (connectionState == ConnectionState.CONNECTED) "$downloadSpeed MB/s" else "0.0 MB/s",
                        icon = Icons.Outlined.ArrowDownward,
                        iconColor = AlphaPrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryCard(
                        title = "آپلود",
                        value = if (connectionState == ConnectionState.CONNECTED) "$uploadSpeed MB/s" else "0.0 MB/s",
                        icon = Icons.Outlined.ArrowUpward,
                        iconColor = AlphaAccentOrange,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryCard(
                        title = "پینگ",
                        value = "${selectedServer.pingMs} ms",
                        icon = Icons.Outlined.Speed,
                        iconColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryCard(
                        title = "مدت زمان",
                        value = formatDuration(if (connectionState == ConnectionState.CONNECTED) durationSeconds else 0),
                        icon = Icons.Outlined.Timer,
                        iconColor = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Server Selector Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x10004664))
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showServerSheet = true }
                        .testTag("server_selector_card"),
                    colors = CardDefaults.cardColors(containerColor = AlphaCardBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedServer.flag,
                                fontSize = 26.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = "${selectedServer.country} - ${selectedServer.city}",
                                    color = AlphaTextDark,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "پینگ: ${selectedServer.pingMs} میلی‌ثانیه • نامحدود",
                                        color = AlphaTextMuted,
                                        fontSize = 11.5.sp,
                                        fontFamily = VazirmatnFontFamily
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "انتخاب سرور",
                            tint = AlphaPrimaryBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Protocol selector chip row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("V2Ray / VLESS", "Shadowsocks", "WireGuard").forEach { proto ->
                        val isSelected = selectedProtocol == proto
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFE0F2FE) else AlphaCardBg)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AlphaPrimaryBlue else AlphaFieldBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedProtocol = proto }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = proto,
                                color = if (isSelected) AlphaPrimaryBlueDark else AlphaTextMuted,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = VazirmatnFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TelegramSupportLink()
            }

            // Server Selection Bottom Sheet
            if (showServerSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showServerSheet = false },
                    sheetState = sheetState,
                    containerColor = AlphaCardBg,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .navigationBarsPadding()
                        ) {
                            Text(
                                text = "انتخاب سرور پرسرعت",
                                color = AlphaTextDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = VazirmatnFontFamily,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            serverList.forEach { server ->
                                val isChosen = server.id == selectedServer.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isChosen) Color(0xFFF0F9FF) else Color(0xFFFAFAFA))
                                        .border(
                                            1.dp,
                                            if (isChosen) AlphaPrimaryBlue else AlphaFieldBorder,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            selectedServer = server
                                            showServerSheet = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = server.flag, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "${server.country} (${server.city})",
                                                color = AlphaTextDark,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = VazirmatnFontFamily
                                            )
                                            Text(
                                                text = "پینگ: ${server.pingMs}ms",
                                                color = Color(0xFF10B981),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        }
                                    }

                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "انتخاب شده",
                                            tint = AlphaPrimaryBlue
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VpnPowerButton(
    state: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")
    val pulseRingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale"
    )
    val pulseRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )

    Box(
        modifier = modifier
            .size(190.dp)
            .testTag("vpn_power_button"),
        contentAlignment = Alignment.Center
    ) {
        // Outer animated ripple ring when connected or connecting
        if (state != ConnectionState.DISCONNECTED) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(pulseRingScale)
                    .clip(CircleShape)
                    .background(
                        if (state == ConnectionState.CONNECTED)
                            AlphaPrimaryBlue.copy(alpha = pulseRingAlpha)
                        else
                            AlphaAccentOrange.copy(alpha = pulseRingAlpha)
                    )
            )
        }

        // Inner glowing border ring
        Box(
            modifier = Modifier
                .size(155.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            if (state == ConnectionState.CONNECTED) Color(0xFFD0F0FD) else Color(0xFFFFEDD5)
                        )
                    )
                )
                .border(
                    width = 3.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            AlphaPrimaryBlue,
                            AlphaAccentOrange,
                            AlphaPrimaryBlueDark,
                            AlphaPrimaryBlue
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Main Button Core
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = if (state == ConnectionState.CONNECTED) AlphaPrimaryBlueDark else AlphaAccentOrangeDark
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = when (state) {
                                ConnectionState.DISCONNECTED -> listOf(Color(0xFFF1F5F9), Color(0xFFCBD5E1))
                                ConnectionState.CONNECTING -> listOf(Color(0xFFFF8A50), Color(0xFFE05326))
                                ConnectionState.CONNECTED -> listOf(Color(0xFF00B4D8), Color(0xFF00779E))
                            }
                        )
                    )
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "اتصال و قطع وی‌پی‌ان",
                    tint = if (state == ConnectionState.DISCONNECTED) Color(0xFF64748B) else Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }
        }
    }
}

@Composable
private fun TelemetryCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x0C004664))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = AlphaCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = AlphaTextMuted,
                    fontSize = 12.sp,
                    fontFamily = VazirmatnFontFamily
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = AlphaTextDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = VazirmatnFontFamily
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
