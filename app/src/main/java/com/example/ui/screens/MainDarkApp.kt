package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.FloatingBottomNavigationBar
import com.example.ui.components.MainTab
import com.example.ui.theme.ProvideAlphaColors
import com.example.ui.util.AppLanguage
import com.example.ui.viewmodel.AlphaVpnViewModel

@Composable
fun MainDarkApp(
    viewModel: AlphaVpnViewModel,
    onLogoutToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: MainTab = MainTab.HOME
) {
    var currentTab by remember { mutableStateOf(initialTab) }
    val isRtl = viewModel.appLanguage == AppLanguage.PERSIAN

    ProvideAlphaColors(themeMode = viewModel.themeMode) {
        CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            Box(modifier = modifier.fillMaxSize()) {
                // Tab Content with smooth fade transitions
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                    label = "tab_content_transition"
                ) { tab ->
                    when (tab) {
                        MainTab.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToServers = { currentTab = MainTab.SERVERS },
                                onNavigateToSubscription = { currentTab = MainTab.SUBSCRIPTION }
                            )
                        }
                        MainTab.SERVERS -> {
                            ServersScreen(
                                viewModel = viewModel,
                                onServerSelected = { _ ->
                                    // Direct connect executed, immediately show Home screen!
                                    currentTab = MainTab.HOME
                                }
                            )
                        }
                        MainTab.SUBSCRIPTION -> {
                            SubscriptionScreen(viewModel = viewModel)
                        }
                        MainTab.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onLogoutToLogin = onLogoutToLogin
                            )
                        }
                    }
                }

                // Floating Bottom Navigation Bar
                FloatingBottomNavigationBar(
                    currentTab = currentTab,
                    language = viewModel.appLanguage,
                    onTabSelected = { currentTab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}
