package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAlphaAppColors
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings

enum class MainTab {
    HOME,
    SERVERS,
    SUBSCRIPTION,
    SETTINGS
}

@Composable
fun FloatingBottomNavigationBar(
    currentTab: MainTab,
    language: AppLanguage,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAlphaAppColors.current
    val shape = RoundedCornerShape(36.dp)

    val isRtl = language == AppLanguage.PERSIAN
    CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .shadow(
                    elevation = if (colors.isDark) 16.dp else 10.dp,
                    shape = shape,
                    ambientColor = colors.shadowColor,
                    spotColor = if (colors.isDark) Color(0x33000000) else Color(0x1F004664)
                )
                .clip(shape)
                .background(colors.navBg)
                .border(width = 1.2.dp, color = colors.navBorder, shape = shape)
                .padding(horizontal = 8.dp, vertical = 7.dp)
                .testTag("floating_bottom_bar"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab

                    val title = when (tab) {
                        MainTab.HOME -> AppStrings.tabHome(language)
                        MainTab.SERVERS -> AppStrings.tabServers(language)
                        MainTab.SUBSCRIPTION -> AppStrings.tabSubscription(language)
                        MainTab.SETTINGS -> AppStrings.tabSettings(language)
                    }

                    val icon: ImageVector = when (tab) {
                        MainTab.HOME -> if (isSelected) Icons.Filled.Shield else Icons.Outlined.Shield
                        MainTab.SERVERS -> if (isSelected) Icons.Filled.ViewAgenda else Icons.Outlined.ViewAgenda
                        MainTab.SUBSCRIPTION -> Icons.Outlined.DonutLarge
                        MainTab.SETTINGS -> if (isSelected) Icons.Filled.Tune else Icons.Outlined.Tune
                    }

                    if (isSelected) {
                        // Active Tab Capsule with Gradient
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            colors.accentTeal,
                                            colors.accentOrange
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onTabSelected(tab) }
                                )
                                .testTag("tab_${tab.name.lowercase()}_active"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFontFamily
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        // Inactive Tab Icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onTabSelected(tab) }
                                )
                                .testTag("tab_${tab.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = colors.textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
