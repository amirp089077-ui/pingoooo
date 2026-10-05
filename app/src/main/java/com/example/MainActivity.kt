package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainDarkApp
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ProvideAlphaColors
import com.example.ui.viewmodel.AlphaVpnViewModel

enum class AppScreen {
    SPLASH,   // 1. First Screen: Splash
    LOGIN,    // 2. Second Screen: Login
    MAIN_APP  // 3. Main Application (4 tabs: Home, Servers, Subscription, Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AlphaVpnApp()
            }
        }
    }
}

@Composable
fun AlphaVpnApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.example.data.SessionManager.getInstance(context) }
    val viewModel: AlphaVpnViewModel = viewModel()

    // If a saved session exists, bypass login and open MAIN_APP directly!
    var currentScreen by remember {
        mutableStateOf(if (sessionManager.isLoggedIn) AppScreen.MAIN_APP else AppScreen.SPLASH)
    }

    ProvideAlphaColors(themeMode = viewModel.themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "screen_navigation"
                ) { screen ->
                    when (screen) {
                        AppScreen.SPLASH -> {
                            SplashScreen(
                                language = viewModel.appLanguage,
                                onNavigateToLogin = {
                                    currentScreen = AppScreen.LOGIN
                                }
                            )
                        }
                        AppScreen.LOGIN -> {
                            LoginScreen(
                                viewModel = viewModel,
                                language = viewModel.appLanguage,
                                onLoginSuccess = { user ->
                                    viewModel.updateUsername(user)
                                    currentScreen = AppScreen.MAIN_APP
                                },
                                onNavigateBack = {
                                    currentScreen = AppScreen.SPLASH
                                }
                            )
                        }
                        AppScreen.MAIN_APP -> {
                            MainDarkApp(
                                viewModel = viewModel,
                                onLogoutToLogin = {
                                    viewModel.performLogout(context) {
                                        currentScreen = AppScreen.LOGIN
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
