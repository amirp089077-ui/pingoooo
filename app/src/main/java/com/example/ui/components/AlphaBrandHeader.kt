package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlphaAccentOrange
import com.example.ui.theme.AlphaPrimaryBlue
import com.example.ui.theme.AlphaTextSlogan

@Composable
fun AlphaBrandTitle(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    letterSpacing: TextUnit = 1.5.sp
) {
    Row(
        modifier = modifier.testTag("brand_title"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "ALPH" in vibrant Cyan-Blue
        Text(
            text = "ALPH",
            color = AlphaPrimaryBlue,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = letterSpacing
        )
        // "A" in vibrant Accent Orange
        Text(
            text = "A",
            color = AlphaAccentOrange,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = letterSpacing
        )
        // Space
        Text(
            text = " ",
            fontSize = fontSize
        )
        // "VPN" in vibrant Accent Orange
        Text(
            text = "VPN",
            color = AlphaAccentOrange,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = letterSpacing
        )
    }
}

@Composable
fun AlphaBrandHeader(
    modifier: Modifier = Modifier,
    subtitle: String,
    titleSize: TextUnit = 28.sp,
    subtitleSize: TextUnit = 15.sp,
    subtitleColor: Color = AlphaTextSlogan
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlphaBrandTitle(fontSize = titleSize)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = subtitle,
            color = subtitleColor,
            fontSize = subtitleSize,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
