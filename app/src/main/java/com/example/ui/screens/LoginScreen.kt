package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AlphaBrandTitle
import com.example.ui.components.AlphaGradientButton
import com.example.ui.components.AlphaInputField
import com.example.ui.components.AlphaLogo
import com.example.ui.components.DarkThemeBackground
import com.example.ui.components.TelegramSupportLink
import com.example.ui.theme.LocalAlphaAppColors
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import com.example.ui.viewmodel.AlphaVpnViewModel

@Composable
fun LoginScreen(
    viewModel: AlphaVpnViewModel,
    language: AppLanguage,
    onLoginSuccess: (username: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    val colors = LocalAlphaAppColors.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val handleLogin: () -> Unit = {
        focusManager.clearFocus()
        if (username.isBlank()) {
            errorMessage = AppStrings.enterUsernameError(language)
        } else if (password.isBlank()) {
            errorMessage = AppStrings.enterPasswordError(language)
        } else {
            errorMessage = null
            isLoading = true
            viewModel.loginUser(username.trim(), password.trim()) { success, errorMsg ->
                isLoading = false
                if (success) {
                    onLoginSuccess(username.trim())
                } else {
                    errorMessage = errorMsg ?: (if (language == AppLanguage.PERSIAN) "خطا در ورود به حساب" else "Login failed")
                }
            }
        }
    }

    val isRtl = language == AppLanguage.PERSIAN
    CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        DarkThemeBackground(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .testTag("login_screen_root")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Section: Compact Logo + Title + Welcome
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AlphaLogo(
                        size = 130.dp,
                        animated = false,
                        modifier = Modifier.testTag("login_logo")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    AlphaBrandTitle(
                        fontSize = 24.sp,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = AppStrings.welcome(language),
                        color = colors.textSecondary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = VazirmatnFontFamily,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("login_welcome_text")
                    )
                }

                // Middle Section: Glass Card (Pure White in Light, Glass Dark in Dark)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (colors.isDark) 16.dp else 12.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = colors.shadowColor,
                            spotColor = if (colors.isDark) Color(0x33000000) else Color(0x1F004664)
                        )
                        .testTag("login_card"),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 26.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = AppStrings.loginTitle(language),
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = VazirmatnFontFamily,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_title")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = AppStrings.loginSubtitle(language),
                            color = colors.textMuted,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            fontFamily = VazirmatnFontFamily,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_subtitle")
                        )

                        // Error Banner if validation fails
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            errorMessage?.let { errorText ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .background(
                                            Color(0xFFFEF2F2),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = "خطا",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = errorText,
                                        color = Color(0xFFDC2626),
                                        fontSize = 12.5.sp,
                                        fontFamily = VazirmatnFontFamily
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Field 1: Username
                        AlphaInputField(
                            value = username,
                            onValueChange = {
                                username = it
                                if (errorMessage != null) errorMessage = null
                            },
                            placeholder = AppStrings.username(language),
                            trailingIcon = Icons.Outlined.Person,
                            testTag = "username_input",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Field 2: Password
                        AlphaInputField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (errorMessage != null) errorMessage = null
                            },
                            placeholder = AppStrings.password(language),
                            trailingIcon = Icons.Outlined.Lock,
                            isPassword = true,
                            testTag = "password_input",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { handleLogin() }
                            )
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // Action Button: "ورود" / "Log In"
                        AlphaGradientButton(
                            text = AppStrings.signIn(language),
                            onClick = { handleLogin() },
                            isLoading = isLoading,
                            testTag = "login_button"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer Section: Telegram Support Link
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TelegramSupportLink(
                        modifier = Modifier.testTag("footer_telegram_link")
                    )
                }
            }
        }
    }
}
