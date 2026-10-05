package com.example

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SessionManager
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainDarkApp
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ProvideAlphaColors
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.AlphaVpnViewModel

enum class AppScreen { SPLASH, LOGIN, MAIN_APP }

class MainActivity : ComponentActivity() {

    private var vpnPermissionCallback: ((Boolean) -> Unit)? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vpnPermissionCallback?.invoke(result.resultCode == RESULT_OK)
        vpnPermissionCallback = null
    }

    fun requestVpnPermission(onResult: (Boolean) -> Unit) {
        val intent = VpnService.prepare(this)
        if (intent == null) { onResult(true) }
        else { vpnPermissionCallback = onResult; vpnPermissionLauncher.launch(intent) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AlphaVpnApp(activity = this) }
    }
}

@Composable
fun AlphaVpnApp(activity: MainActivity) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val viewModel: AlphaVpnViewModel = viewModel()

    var currentScreen by remember {
        mutableStateOf(if (sessionManager.isLoggedIn) AppScreen.MAIN_APP else AppScreen.SPLASH)
    }

    val isDark = viewModel.themeMode == ThemeMode.DARK ||
        (viewModel.themeMode == ThemeMode.AUTO && isSystemInDarkTheme())

    ProvideAlphaColors(themeMode = viewModel.themeMode) {
        MyApplicationTheme(isDark = isDark) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState.ordinal > initialState.ordinal)
                                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                            else
                                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        },
                        label = "nav"
                    ) { screen ->
                        when (screen) {
                            AppScreen.SPLASH -> SplashScreen(
                                language = viewModel.appLanguage,
                                onNavigateToLogin = { currentScreen = AppScreen.LOGIN }
                            )
                            AppScreen.LOGIN -> LoginScreen(
                                viewModel = viewModel,
                                language = viewModel.appLanguage,
                                onLoginSuccess = { user ->
                                    viewModel.updateUsername(user)
                                    activity.requestVpnPermission { granted ->
                                        viewModel.onVpnPermissionResult(granted)
                                        currentScreen = AppScreen.MAIN_APP
                                    }
                                },
                                onNavigateBack = { currentScreen = AppScreen.SPLASH }
                            )
                            AppScreen.MAIN_APP -> MainDarkApp(
                                viewModel = viewModel,
                                activity = activity,
                                onLogoutToLogin = {
                                    viewModel.performLogout(context) { currentScreen = AppScreen.LOGIN }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
