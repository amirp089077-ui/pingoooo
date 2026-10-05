package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.ThemeMode
import com.example.ui.util.AppLanguage
import com.example.ui.viewmodel.UserPlanType

data class SavedUserSession(
    val username: String,
    val token: String,
    val planType: UserPlanType,
    val remainingGb: Float,
    val usedGb: Float,
    val totalQuotaGb: Float,
    val remainingDays: Int,
    val totalDays: Int,
    val expiryDate: String,
    val activeDevices: Int,
    val maxDevices: Int
)

data class SavedAppSettings(
    val language: AppLanguage,
    val themeMode: ThemeMode,
    val adBlocking: Boolean,
    val directIranianSites: Boolean,
    val selectedServerId: String
)

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "alpha_vpn_session_prefs"
        
        // Session Keys
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USERNAME = "key_username"
        private const val KEY_TOKEN = "key_token"
        private const val KEY_PLAN_TYPE = "key_plan_type"
        private const val KEY_REMAINING_GB = "key_remaining_gb"
        private const val KEY_USED_GB = "key_used_gb"
        private const val KEY_TOTAL_QUOTA_GB = "key_total_quota_gb"
        private const val KEY_REMAINING_DAYS = "key_remaining_days"
        private const val KEY_TOTAL_DAYS = "key_total_days"
        private const val KEY_EXPIRY_DATE = "key_expiry_date"
        private const val KEY_ACTIVE_DEVICES = "key_active_devices"
        private const val KEY_MAX_DEVICES = "key_max_devices"
        
        // App Settings Keys
        private const val KEY_APP_LANGUAGE = "key_app_language"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_AD_BLOCKING = "key_ad_blocking"
        private const val KEY_DIRECT_IRANIAN_SITES = "key_direct_iranian_sites"
        private const val KEY_SELECTED_SERVER_ID = "key_selected_server_id"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    val isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !prefs.getString(KEY_USERNAME, "").isNullOrBlank()

    val savedToken: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""

    fun saveUserSession(
        username: String,
        token: String,
        planType: String,
        remainingGb: Float,
        usedGb: Float,
        totalQuotaGb: Float,
        remainingDays: Int,
        totalDays: Int,
        expiryDate: String,
        activeDevices: Int,
        maxDevices: Int
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USERNAME, username)
            .putString(KEY_TOKEN, token)
            .putString(KEY_PLAN_TYPE, planType)
            .putFloat(KEY_REMAINING_GB, remainingGb)
            .putFloat(KEY_USED_GB, usedGb)
            .putFloat(KEY_TOTAL_QUOTA_GB, totalQuotaGb)
            .putInt(KEY_REMAINING_DAYS, remainingDays)
            .putInt(KEY_TOTAL_DAYS, totalDays)
            .putString(KEY_EXPIRY_DATE, expiryDate)
            .putInt(KEY_ACTIVE_DEVICES, activeDevices)
            .putInt(KEY_MAX_DEVICES, maxDevices)
            .apply()
    }

    fun getUserSession(): SavedUserSession? {
        if (!isLoggedIn) return null
        val username = prefs.getString(KEY_USERNAME, "") ?: return null
        val token    = prefs.getString(KEY_TOKEN, "") ?: return null
        val planTypeStr = prefs.getString(KEY_PLAN_TYPE, "MULTI_USER") ?: "MULTI_USER"
        val planType = if (planTypeStr.contains("SINGLE", ignoreCase = true)) UserPlanType.SINGLE_USER else UserPlanType.MULTI_USER

        return SavedUserSession(
            username      = username,
            token         = token,
            planType      = planType,
            remainingGb   = prefs.getFloat(KEY_REMAINING_GB, 34.1f),
            usedGb        = prefs.getFloat(KEY_USED_GB, 0f),
            totalQuotaGb  = prefs.getFloat(KEY_TOTAL_QUOTA_GB, 34.1f),
            remainingDays = prefs.getInt(KEY_REMAINING_DAYS, 17),
            totalDays     = prefs.getInt(KEY_TOTAL_DAYS, 30),
            expiryDate    = prefs.getString(KEY_EXPIRY_DATE, "") ?: "",
            activeDevices = prefs.getInt(KEY_ACTIVE_DEVICES, 1),
            maxDevices    = prefs.getInt(KEY_MAX_DEVICES, 2)
        )
    }

    fun updateUsage(remainingGb: Float, usedGb: Float) {
        prefs.edit()
            .putFloat(KEY_REMAINING_GB, remainingGb)
            .putFloat(KEY_USED_GB, usedGb)
            .apply()
    }

    fun updateActiveDevices(count: Int) {
        prefs.edit()
            .putInt(KEY_ACTIVE_DEVICES, count)
            .apply()
    }

    fun updateToken(newToken: String) {
        prefs.edit()
            .putString(KEY_TOKEN, newToken)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USERNAME)
            .remove(KEY_TOKEN)
            .remove(KEY_PLAN_TYPE)
            .remove(KEY_REMAINING_GB)
            .remove(KEY_USED_GB)
            .remove(KEY_TOTAL_QUOTA_GB)
            .remove(KEY_REMAINING_DAYS)
            .remove(KEY_TOTAL_DAYS)
            .remove(KEY_EXPIRY_DATE)
            .remove(KEY_ACTIVE_DEVICES)
            .remove(KEY_MAX_DEVICES)
            .apply()
    }

    fun saveAppSettings(
        language: AppLanguage,
        themeMode: ThemeMode,
        adBlocking: Boolean,
        directIranianSites: Boolean,
        selectedServerId: String
    ) {
        prefs.edit()
            .putString(KEY_APP_LANGUAGE, language.code)
            .putString(KEY_THEME_MODE, themeMode.name)
            .putBoolean(KEY_AD_BLOCKING, adBlocking)
            .putBoolean(KEY_DIRECT_IRANIAN_SITES, directIranianSites)
            .putString(KEY_SELECTED_SERVER_ID, selectedServerId)
            .apply()
    }

    fun loadAppSettings(): SavedAppSettings {
        val langCode = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.PERSIAN.code) ?: AppLanguage.PERSIAN.code
        val language = if (langCode == AppLanguage.ENGLISH.code) AppLanguage.ENGLISH else AppLanguage.PERSIAN

        val themeName = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val themeMode = try {
            ThemeMode.valueOf(themeName)
        } catch (e: Exception) {
            ThemeMode.DARK
        }

        return SavedAppSettings(
            language = language,
            themeMode = themeMode,
            adBlocking = prefs.getBoolean(KEY_AD_BLOCKING, false),
            directIranianSites = prefs.getBoolean(KEY_DIRECT_IRANIAN_SITES, true),
            selectedServerId = prefs.getString(KEY_SELECTED_SERVER_ID, "srv_de_1") ?: "srv_de_1"
        )
    }
}
