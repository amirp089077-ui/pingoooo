package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AlphaLogo
import com.example.ui.components.DarkGlassCard
import com.example.ui.components.DarkThemeBackground
import com.example.ui.theme.LocalAlphaAppColors
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import com.example.ui.viewmodel.AlphaVpnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AlphaVpnViewModel,
    onLogoutToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAlphaAppColors.current
    val context = LocalContext.current
    val lang = viewModel.appLanguage

    var isAppearanceExpanded by remember { mutableStateOf(true) }
    var isConnectionExpanded by remember { mutableStateOf(true) }

    // Dialogs / Sheets
    var showAppWhitelistSheet by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Trigger loading device apps when sheet opens
    LaunchedEffect(showAppWhitelistSheet) {
        if (showAppWhitelistSheet) {
            viewModel.loadRealDeviceApps(context)
        }
    }

    val copyUsername = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("username", viewModel.username)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "${AppStrings.usernameCopied(lang)}: ${viewModel.username}", Toast.LENGTH_SHORT).show()
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
                // Logout Confirmation Dialog
                if (showLogoutConfirmDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showLogoutConfirmDialog = false },
                        title = {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "خروج از حساب کاربری" else "Log Out",
                                fontFamily = VazirmatnFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        },
                        text = {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN)
                                    "آیا مطمئن هستید؟ با خروج شما از حساب، این دستگاه از لیست دستگاه‌های فعال در فایربیس آزاد می‌شود و می‌توانید از دستگاه دیگر متصل شوید."
                                else
                                    "Are you sure? Logging out will free up this device slot in Firebase so you can log in on another device.",
                                fontFamily = VazirmatnFontFamily,
                                color = colors.textMuted
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showLogoutConfirmDialog = false
                                    onLogoutToLogin()
                                }
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "خروج و آزادسازی دستگاه" else "Log Out",
                                    fontFamily = VazirmatnFontFamily,
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { showLogoutConfirmDialog = false }
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "انصراف" else "Cancel",
                                    fontFamily = VazirmatnFontFamily,
                                    color = colors.textMuted
                                )
                            }
                        },
                        containerColor = colors.cardBg,
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.settings(lang),
                        color = colors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirmatnFontFamily
                    )

                    IconButton(
                        onClick = { showLogoutConfirmDialog = true },
                        modifier = Modifier.testTag("settings_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                            contentDescription = AppStrings.logout(lang),
                            tint = colors.accentOrange
                        )
                    }
                }

                // User Profile Card
                DarkGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_profile_card"),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = viewModel.deviceName,
                                    color = colors.textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = AppStrings.deviceId(lang),
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = viewModel.deviceId,
                                        color = colors.textMuted,
                                        fontSize = 13.sp,
                                        fontFamily = VazirmatnFontFamily
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(colors.cardBgSecondary)
                                    .border(1.5.dp, colors.accentTeal, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AlphaLogo(size = 46.dp, animated = false)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Inner Username Pill Row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.cardBgSecondary)
                                .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AlternateEmail,
                                        contentDescription = "یوزرنیم",
                                        tint = colors.accentTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = AppStrings.username(lang),
                                        color = colors.textSecondary,
                                        fontSize = 13.5.sp,
                                        fontFamily = VazirmatnFontFamily
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { copyUsername() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = viewModel.username,
                                        color = colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = VazirmatnFontFamily
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "کپی",
                                        tint = colors.accentTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 1 Header: Appearance
                SectionHeaderRow(
                    title = AppStrings.appearance(lang),
                    isExpanded = isAppearanceExpanded,
                    onToggle = { isAppearanceExpanded = !isAppearanceExpanded }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = isAppearanceExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    DarkGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // 1. Theme Mode Switcher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.displayMode(lang),
                                    color = colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(colors.accentOrange.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NightlightRound,
                                        contentDescription = "Theme",
                                        tint = colors.accentOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3-Option Segmented Control (Auto / Light / Dark)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.cardBgSecondary)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                                    .padding(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf(
                                        ThemeMode.AUTO to AppStrings.auto(lang),
                                        ThemeMode.LIGHT to AppStrings.light(lang),
                                        ThemeMode.DARK to AppStrings.dark(lang)
                                    ).forEach { (mode, label) ->
                                        val isSelected = viewModel.themeMode == mode
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) colors.accentTeal else Color.Transparent
                                                )
                                                .clickable { viewModel.setTheme(mode) }
                                                .padding(vertical = 10.dp)
                                                .testTag("theme_btn_${mode.name.lowercase()}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) {
                                                    if (colors.isDark) Color(0xFF020608) else Color.White
                                                } else {
                                                    colors.textSecondary
                                                },
                                                fontSize = 13.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            HorizontalDivider(color = colors.cardBorder)
                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. Language Switcher (Bilingual: فارسی / English)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.language(lang),
                                    color = colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(colors.accentTeal.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Language",
                                        tint = colors.accentTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Language Segmented Control (فارسی / English)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.cardBgSecondary)
                                    .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                                    .padding(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf(
                                        AppLanguage.PERSIAN to "فارسی",
                                        AppLanguage.ENGLISH to "English"
                                    ).forEach { (l, label) ->
                                        val isSelected = viewModel.appLanguage == l
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) colors.accentTeal else Color.Transparent
                                                )
                                                .clickable { viewModel.setLanguage(l) }
                                                .padding(vertical = 10.dp)
                                                .testTag("lang_btn_${l.code}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) {
                                                    if (colors.isDark) Color(0xFF020608) else Color.White
                                                } else {
                                                    colors.textSecondary
                                                },
                                                fontSize = 13.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2 Header: Connection
                SectionHeaderRow(
                    title = AppStrings.connection(lang),
                    isExpanded = isConnectionExpanded,
                    onToggle = { isConnectionExpanded = !isConnectionExpanded }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = isConnectionExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    DarkGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            // 1. لیست سفید برنامه‌ها
                            SettingItemWithArrow(
                                title = AppStrings.appWhitelist(lang),
                                subtitle = AppStrings.appWhitelistDesc(viewModel.whitelistApps.count { it.isVpnEnabled }, lang),
                                icon = Icons.Default.Apps,
                                iconColor = colors.accentTeal,
                                onClick = { showAppWhitelistSheet = true }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                color = colors.cardBorder
                            )

                            // 2. عبور مستقیم سایت‌های ایرانی
                            SettingItemWithSwitch(
                                title = AppStrings.directIranianSites(lang),
                                subtitle = if (viewModel.directIranianSites) {
                                    val savedMbStr = String.format("%.1f", viewModel.domesticDataSavedMb)
                                    if (lang == AppLanguage.PERSIAN)
                                        "فعال: ۱,۴۲۰ رنج IP ملی بای‌پَس می‌شود ($savedMbStr مگابایت ترافیک رایگان داخلی)"
                                    else
                                        "Active: 1,420 IR-CIDR ranges bypass tunnel ($savedMbStr MB domestic data saved)"
                                } else {
                                    AppStrings.directIranianSitesDesc(lang)
                                },
                                icon = Icons.Default.Flag,
                                iconColor = colors.accentTeal,
                                checked = viewModel.directIranianSites,
                                onCheckedChange = { viewModel.toggleIranianSites(it) }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                color = colors.cardBorder
                            )

                            // 3. مسدودسازی تبلیغات
                            SettingItemWithSwitch(
                                title = AppStrings.adBlocking(lang),
                                subtitle = if (viewModel.adBlocking) {
                                    if (lang == AppLanguage.PERSIAN)
                                        "فعال با AdGuard DNS • ${viewModel.blockedAdsCount} تبلیغ و ردیاب مسدود شده"
                                    else
                                        "Active via AdGuard DNS • ${viewModel.blockedAdsCount} ads & trackers blocked"
                                } else {
                                    AppStrings.adBlockingDesc(lang)
                                },
                                icon = Icons.Default.Block,
                                iconColor = if (viewModel.adBlocking) Color(0xFF10B981) else Color(0xFFEF4444),
                                checked = viewModel.adBlocking,
                                onCheckedChange = { viewModel.toggleAdBlocking(it) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Logout Action Card (Decrements active devices in Firestore)
                DarkGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutConfirmDialog = true }
                        .testTag("settings_bottom_logout_card"),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                                    contentDescription = "Logout",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "خروج از حساب کاربری" else "Log Out",
                                    color = Color(0xFFEF4444),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN)
                                        "آزادسازی سهمیه این دستگاه (${viewModel.activeDevicesCount} دستگاه متصل)"
                                    else
                                        "Free up this device slot (${viewModel.activeDevicesCount} active)",
                                    color = colors.textMuted,
                                    fontSize = 11.5.sp,
                                    fontFamily = VazirmatnFontFamily
                                )
                            }
                        }
                    }
                }
            }



            // Real App Whitelist / Split Tunneling Modal Bottom Sheet
            if (showAppWhitelistSheet) {
                val filteredApps = remember(viewModel.whitelistApps, appSearchQuery) {
                    if (appSearchQuery.isBlank()) {
                        viewModel.whitelistApps
                    } else {
                        viewModel.whitelistApps.filter {
                            it.name.contains(appSearchQuery, ignoreCase = true) ||
                            it.packageName.contains(appSearchQuery, ignoreCase = true)
                        }
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = { showAppWhitelistSheet = false },
                    sheetState = sheetState,
                    containerColor = colors.cardBg,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(560.dp)
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .navigationBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.appWhitelist(lang),
                                    color = colors.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )

                                Row {
                                    TextButton(onClick = { viewModel.setAllAppsVpn(true) }) {
                                        Text(AppStrings.selectAll(lang), color = colors.accentTeal, fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { viewModel.setAllAppsVpn(false) }) {
                                        Text(AppStrings.deselectAll(lang), color = colors.textMuted, fontSize = 12.sp)
                                    }
                                }
                            }

                            // Search bar
                            OutlinedTextField(
                                value = appSearchQuery,
                                onValueChange = { appSearchQuery = it },
                                placeholder = {
                                    Text(AppStrings.searchApps(lang), color = colors.textMuted, fontSize = 13.sp)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.textMuted)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accentTeal,
                                    unfocusedBorderColor = colors.cardBorder,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                ),
                                singleLine = true
                            )

                            // App list
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredApps, key = { it.id }) { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.cardBgSecondary)
                                            .clickable { viewModel.toggleAppWhitelist(app.id, !app.isVpnEnabled) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = app.name,
                                                color = colors.textPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = VazirmatnFontFamily
                                            )
                                            Text(
                                                text = if (app.isVpnEnabled)
                                                    (if (lang == AppLanguage.PERSIAN) "عبور از فیلتر VPN (فعال)" else "Routes through VPN tunnel")
                                                else
                                                    (if (lang == AppLanguage.PERSIAN) "مستقیم از اینترنت گوشی (بای‌پس)" else "Bypasses VPN (Direct internet)"),
                                                color = if (app.isVpnEnabled) colors.accentTeal else colors.textMuted,
                                                fontSize = 11.sp,
                                                fontFamily = VazirmatnFontFamily
                                            )
                                        }

                                        Switch(
                                            checked = app.isVpnEnabled,
                                            onCheckedChange = { viewModel.toggleAppWhitelist(app.id, it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = colors.accentTeal
                                            )
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

@Composable
private fun SectionHeaderRow(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val colors = LocalAlphaAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = VazirmatnFontFamily
        )

        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = title,
            tint = colors.accentOrange,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingItemWithArrow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit = {}
) {
    val colors = LocalAlphaAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontFamily = VazirmatnFontFamily
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "باز کردن",
            tint = colors.textMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SettingItemWithSwitch(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalAlphaAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontFamily = VazirmatnFontFamily
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accentTeal,
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.cardBorder
            )
        )
    }
}
