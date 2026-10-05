package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// IranSans Bold FontFamily
val IranSansBoldFontFamily = FontFamily(
    Font(R.font.iransans_bold, FontWeight.Bold),
    Font(R.font.iransans_bold, FontWeight.Normal)
)

val VazirmatnFontFamily = IranSansBoldFontFamily

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = IranSansBoldFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        color = AlphaTextDark
    ),
    headlineMedium = TextStyle(
        fontFamily = IranSansBoldFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = AlphaTextDark
    ),
    titleLarge = TextStyle(
        fontFamily = IranSansBoldFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = AlphaTextDark
    ),
    titleMedium = TextStyle(
        fontFamily = IranSansBoldFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = AlphaTextDark
    ),
    bodyLarge = TextStyle(
        fontFamily = IranSansBoldFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = AlphaTextDark
    ),
    bodyMedium = TextStyle(
        fontFamily = IranSansBoldFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
        color = AlphaTextMuted
    ),
    labelLarge = TextStyle(
        fontFamily = IranSansBoldFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = AlphaCardBg
    )
)
