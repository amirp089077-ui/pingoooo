package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlphaPrimaryBlue
import com.example.ui.theme.AlphaTextSlogan
import com.example.ui.theme.VazirmatnFontFamily

@Composable
fun TelegramSupportLink(
    modifier: Modifier = Modifier,
    telegramUrl: String = "https://t.me/alphavpn_support"
) {
    val context = LocalContext.current

    val openTelegram = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "در حال انتقال به تلگرام...", Toast.LENGTH_SHORT).show()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = modifier
                .clickable { openTelegram() }
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .testTag("telegram_support_link"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Telegram Icon / Arrow icon
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "پشتیبانی تلگرام",
                tint = AlphaPrimaryBlue,
                modifier = Modifier.size(17.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "در صورت مشکل ",
                color = AlphaTextSlogan,
                fontSize = 13.5.sp,
                fontFamily = VazirmatnFontFamily
            )

            Text(
                text = "به ما در تلگرام پیام دهید",
                color = AlphaPrimaryBlue,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = VazirmatnFontFamily
            )
        }
    }
}
