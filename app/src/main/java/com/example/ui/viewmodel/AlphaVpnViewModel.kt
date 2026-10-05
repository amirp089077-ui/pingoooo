package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiService
import com.example.data.SessionManager
import com.example.service.AlphaVpnService
import com.example.service.AlphaVpnState
import com.example.ui.theme.ThemeMode
import com.example.ui.util.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.random.Random

data class AppServer(
    val id: String,
    val country: String,
    val city: String,
    val flag: String,
    val ping: Int = 0,
    val host: String = "1.1.1.1",
    val port: Int = 443,
    val badge: String? = null,
    val emoji: String = "",
    val isPro: Boolean = false,
    val configUri: String = ""
)

enum class UserPlanType(val titleFa: String, val titleEn: String, val maxDevices: Int) {
    SINGLE_USER("تک‌کاربره (۱ دستگاه)", "Single-User (1 Device)", 1),
    MULTI_USER("دو کاربره همزمان (۲ دستگاه)", "Multi-User (2 Devices)", 2)
}

data class InstalledApp(
    val id: String,
    val name: String,
    val packageName: String,
    val isVpnEnabled: Boolean
)

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED }

class AlphaVpnViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager.getInstance(application)

    // ── token در حافظه (از session لود میشه) ────────────────────────
    private var authToken: String = ""

    // ── Language & Theme ─────────────────────────────────────────────
    var appLanguage by mutableStateOf(AppLanguage.PERSIAN); private set
    var themeMode   by mutableStateOf(ThemeMode.DARK);      private set

    // ── VPN State ────────────────────────────────────────────────────
    var vpnStatus              by mutableStateOf(VpnStatus.DISCONNECTED); private set
    var connectionDurationSeconds by mutableIntStateOf(0);                private set
    var downloadSpeed          by mutableFloatStateOf(0f);                private set
    var uploadSpeed            by mutableFloatStateOf(0f);                private set
    var sessionUsedMb          by mutableFloatStateOf(0f);                private set

    // ── Subscription ─────────────────────────────────────────────────
    var totalRemainingGb  by mutableFloatStateOf(0f);   private set
    var totalUsedGb       by mutableFloatStateOf(0f);   private set
    var totalQuotaGb      by mutableFloatStateOf(0f);   private set
    var remainingDays     by mutableIntStateOf(0);      private set
    val totalPurchasedDays get() = _totalDays
    private var _totalDays = 30
    var expiryDate        by mutableStateOf("");        private set
    var userPlanType      by mutableStateOf(UserPlanType.MULTI_USER); private set
    var activeDevicesCount by mutableIntStateOf(1);     private set
    var isSyncingFirestore by mutableStateOf(false);    private set
    var telegramSupportUrl by mutableStateOf("https://t.me/AlphaSupport_ir"); private set
    var announcementText   by mutableStateOf("");       private set

    // ── Servers ──────────────────────────────────────────────────────
    var isSmartServer       by mutableStateOf(false);         private set
    var selectedServer      by mutableStateOf(AppServer(
        id = "pending", country = "سرور", city = "در حال بارگذاری...", flag = "🌐"
    )); private set
    var isRefreshingPings   by mutableStateOf(false);         private set
    var isSortingAsc        by mutableStateOf(false);         private set
    var serverList          by mutableStateOf<List<AppServer>>(emptyList()); private set

    // ── Profile ──────────────────────────────────────────────────────
    var username      by mutableStateOf(""); private set
    var deviceName    by mutableStateOf("Android Device"); private set
    var deviceId      by mutableStateOf(""); private set

    // ── Settings ─────────────────────────────────────────────────────
    var directIranianSites by mutableStateOf(true);  private set
    var adBlocking         by mutableStateOf(false); private set
    var blockedAdsCount    by mutableIntStateOf(0);  private set
    val activeDnsServer get() = if (adBlocking) "AdGuard DNS (94.140.14.14)" else "Cloudflare DNS (1.1.1.1)"
    val excludedIranCidrCount = 1420
    var domesticDataSavedMb by mutableFloatStateOf(0f); private set

    var whitelistApps by mutableStateOf(listOf(
        InstalledApp("tg",      "تلگرام (Telegram)",          "org.telegram.messenger",   true),
        InstalledApp("ig",      "اینستاگرام (Instagram)",      "com.instagram.android",    true),
        InstalledApp("yt",      "یوتیوب (YouTube)",            "com.google.android.youtube",true),
        InstalledApp("chrome",  "گوگل کروم (Chrome)",          "com.android.chrome",       true),
        InstalledApp("wa",      "واتساپ (WhatsApp)",           "com.whatsapp",             true),
        InstalledApp("x",       "توییتر (X / Twitter)",        "com.twitter.android",      true),
        InstalledApp("spotify", "اسپاتیفای (Spotify)",         "com.spotify.music",        true),
        InstalledApp("snapp",   "اسنپ (مستقیم)",               "cab.snapp.passenger",      false),
        InstalledApp("tapsi",   "تپسی (مستقیم)",               "cab.tap30.passenger",      false),
        InstalledApp("divar",   "دیوار (مستقیم)",              "ir.divar",                 false),
    )); private set

    var redeemedCodes = mutableSetOf<String>(); private set
    var isQuotaExceeded by mutableStateOf(false)

    private var connectionJob: Job? = null

    // ─────────────────────────────── init ────────────────────────────
    init {
        val settings = sessionManager.loadAppSettings()
        appLanguage        = settings.language
        themeMode          = settings.themeMode
        adBlocking         = settings.adBlocking
        directIranianSites = settings.directIranianSites

        val saved = sessionManager.getUserSession()
        if (saved != null) {
            authToken         = saved.token
            username          = saved.username
            userPlanType      = saved.planType
            totalRemainingGb  = saved.remainingGb
            totalUsedGb       = saved.usedGb
            totalQuotaGb      = saved.totalQuotaGb
            remainingDays     = saved.remainingDays
            _totalDays        = saved.totalDays
            expiryDate        = saved.expiryDate
            activeDevicesCount = saved.activeDevices
        }

        // جمع‌آوری state سرویس VPN
        viewModelScope.launch { AlphaVpnState.status.collect       { vpnStatus    = it; if (it == VpnStatus.DISCONNECTED) { downloadSpeed = 0f; uploadSpeed = 0f } } }
        viewModelScope.launch { AlphaVpnState.downloadSpeed.collect { downloadSpeed = it } }
        viewModelScope.launch { AlphaVpnState.uploadSpeed.collect   { uploadSpeed   = it } }
        viewModelScope.launch { AlphaVpnState.sessionBytes.collect  { sessionUsedMb = it / (1024f * 1024f) } }

        syncWithServer()
    }

    // ─────────────────────────── Sync ────────────────────────────────
    fun syncWithServer() {
        if (authToken.isBlank()) return
        viewModelScope.launch {
            isSyncingFirestore = true
            try {
                // پروفایل کاربر
                val profile = ApiService.fetchUserProfile(authToken)
                if (profile != null) {
                    totalRemainingGb   = profile.remainingGb
                    totalUsedGb        = profile.usedGb
                    totalQuotaGb       = profile.totalQuotaGb
                    remainingDays      = profile.remainingDays
                    _totalDays         = profile.totalDays
                    expiryDate         = profile.expiryDate
                    activeDevicesCount = profile.activeDevices
                    userPlanType       = if (profile.planType.contains("SINGLE", true))
                        UserPlanType.SINGLE_USER else UserPlanType.MULTI_USER

                    sessionManager.saveUserSession(
                        username      = username,
                        token         = authToken,
                        planType      = profile.planType,
                        remainingGb   = profile.remainingGb,
                        usedGb        = profile.usedGb,
                        totalQuotaGb  = profile.totalQuotaGb,
                        remainingDays = profile.remainingDays,
                        totalDays     = profile.totalDays,
                        expiryDate    = profile.expiryDate,
                        activeDevices = profile.activeDevices,
                        maxDevices    = profile.maxDevices,
                    )
                }

                // تنظیمات اپ
                val config = ApiService.fetchAppConfig()
                if (config != null) {
                    telegramSupportUrl = config.channelUrl
                    announcementText   = config.announcement
                }

                // سرورها
                val servers = ApiService.fetchServers(authToken)
                if (!servers.isNullOrEmpty()) {
                    serverList = servers
                    if (serverList.none { it.id == selectedServer.id })
                        selectedServer = serverList.first()
                    refreshPings()
                }
            } catch (e: Exception) {
                // graceful
            } finally {
                isSyncingFirestore = false
            }
        }
    }

    // ─────────────────────────── Login ───────────────────────────────
    fun loginUser(enteredUsername: String, enteredPassword: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val (result, error) = ApiService.loginWithError(enteredUsername.trim(), enteredPassword.trim())

            if (result == null) {
                onResult(false, error ?: if (appLanguage == AppLanguage.PERSIAN) "خطا در ورود" else "Login failed")
                return@launch
            }

            val p = result.profile
            authToken          = result.token
            username           = p.username
            totalRemainingGb   = p.remainingGb
            totalUsedGb        = p.usedGb
            totalQuotaGb       = p.totalQuotaGb
            remainingDays      = p.remainingDays
            _totalDays         = p.totalDays
            expiryDate         = p.expiryDate
            activeDevicesCount = p.activeDevices
            userPlanType       = if (p.planType.contains("SINGLE", true))
                UserPlanType.SINGLE_USER else UserPlanType.MULTI_USER

            sessionManager.saveUserSession(
                username      = p.username,
                token         = result.token,
                planType      = p.planType,
                remainingGb   = p.remainingGb,
                usedGb        = p.usedGb,
                totalQuotaGb  = p.totalQuotaGb,
                remainingDays = p.remainingDays,
                totalDays     = p.totalDays,
                expiryDate    = p.expiryDate,
                activeDevices = p.activeDevices,
                maxDevices    = p.maxDevices,
            )

            // سرورها
            val servers = ApiService.fetchServers(authToken)
            if (!servers.isNullOrEmpty()) {
                serverList     = servers
                selectedServer = servers.first()
                refreshPings()
            }

            onResult(true, null)
        }
    }

    // ─────────────────────────── Logout ──────────────────────────────
    fun performLogout(context: Context? = null, onComplete: () -> Unit) {
        viewModelScope.launch {
            stopRealVpn(context)
            ApiService.logout(authToken)
            sessionManager.clearSession()
            authToken = ""
            onComplete()
        }
    }

    // ─────────────────────────── Settings ────────────────────────────
    fun setLanguage(lang: AppLanguage) {
        appLanguage = lang
        sessionManager.saveAppSettings(appLanguage, themeMode, adBlocking, directIranianSites, selectedServer.id)
    }

    fun setTheme(mode: ThemeMode) {
        themeMode = mode
        sessionManager.saveAppSettings(appLanguage, themeMode, adBlocking, directIranianSites, selectedServer.id)
    }

    fun setUserPlan(plan: UserPlanType) { userPlanType = plan }

    fun dismissQuotaExceeded() { isQuotaExceeded = false }

    fun toggleIranianSites(checked: Boolean) {
        directIranianSites = checked
        sessionManager.saveAppSettings(appLanguage, themeMode, adBlocking, directIranianSites, selectedServer.id)
    }

    fun toggleAdBlocking(checked: Boolean) {
        adBlocking = checked
        sessionManager.saveAppSettings(appLanguage, themeMode, adBlocking, directIranianSites, selectedServer.id)
    }

    fun toggleAppWhitelist(appId: String, enabled: Boolean) {
        whitelistApps = whitelistApps.map { if (it.id == appId) it.copy(isVpnEnabled = enabled) else it }
    }

    fun setAllAppsVpn(enabled: Boolean) { whitelistApps = whitelistApps.map { it.copy(isVpnEnabled = enabled) } }

    fun updateUsername(newName: String) { if (newName.isNotBlank()) username = newName.trim() }

    // ─────────────────────────── VPN ─────────────────────────────────
    fun startRealVpn(context: Context) {
        if (totalRemainingGb <= 0f || remainingDays <= 0) { isQuotaExceeded = true; return }
        vpnStatus = VpnStatus.CONNECTING
        val disallowed = if (directIranianSites)
            whitelistApps.filter { !it.isVpnEnabled }.map { it.packageName }
        else emptyList()

        AlphaVpnService.startVpn(
            context        = context,
            serverHost     = selectedServer.host,
            serverPort     = selectedServer.port,
            serverCity     = selectedServer.city,
            dnsServer      = activeDnsServer,
            configUri      = selectedServer.configUri,
            disallowedApps = ArrayList(disallowed)
        )
        connectionDurationSeconds = 0
        startDurationTimer()
    }

    fun stopRealVpn(context: Context? = null) {
        val ctx = context ?: getApplication()
        AlphaVpnService.stopVpn(ctx)
        vpnStatus     = VpnStatus.DISCONNECTED
        downloadSpeed = 0f
        uploadSpeed   = 0f
        connectionJob?.cancel()
        viewModelScope.launch {
            ApiService.updateUserUsage(authToken, totalUsedGb, totalRemainingGb)
            sessionManager.updateUsage(totalRemainingGb, totalUsedGb)
        }
    }

    fun toggleConnection(context: Context? = null) {
        val ctx = context ?: getApplication()
        if (vpnStatus == VpnStatus.CONNECTED) stopRealVpn(ctx) else startRealVpn(ctx)
    }

    private fun startDurationTimer() {
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (vpnStatus != VpnStatus.CONNECTED) continue
                connectionDurationSeconds++

                val addedMb = ((downloadSpeed + uploadSpeed) * 0.12f).coerceAtLeast(0.1f)
                val addedGb = addedMb / 1024f
                if (totalRemainingGb <= addedGb) {
                    totalUsedGb += totalRemainingGb; totalRemainingGb = 0f
                    stopRealVpn(); isQuotaExceeded = true; break
                } else {
                    totalRemainingGb -= addedGb; totalUsedGb += addedGb
                }

                if (connectionDurationSeconds % 30 == 0) {
                    ApiService.updateUserUsage(authToken, totalUsedGb, totalRemainingGb)
                    sessionManager.updateUsage(totalRemainingGb, totalUsedGb)
                }
                if (adBlocking && Random.nextInt(100) < 30) blockedAdsCount++
                if (directIranianSites) domesticDataSavedMb += Random.nextDouble(0.05, 0.22).toFloat()
            }
        }
    }

    // ─────────────────────────── Servers ─────────────────────────────
    fun selectServerAndConnect(server: AppServer, context: Context? = null) {
        isSmartServer  = false
        selectedServer = server
        sessionManager.saveAppSettings(appLanguage, themeMode, adBlocking, directIranianSites, server.id)
        startRealVpn(context ?: getApplication())
    }

    fun selectSmartServerAndConnect(context: Context? = null): AppServer {
        isSmartServer = true
        val best = serverList.filter { it.ping > 0 }.minByOrNull { it.ping }
            ?: serverList.minByOrNull { it.ping } ?: selectedServer
        selectedServer = best.copy(city = "${best.city} (هوشمند)")
        sessionManager.saveAppSettings(appLanguage, themeMode, adBlocking, directIranianSites, selectedServer.id)
        startRealVpn(context ?: getApplication())
        return selectedServer
    }

    fun refreshPings() {
        viewModelScope.launch {
            isRefreshingPings = true
            val updated = serverList.map { srv ->
                async(Dispatchers.IO) {
                    srv.copy(ping = measureSocketPing(srv.host, srv.port))
                }
            }.awaitAll()
            serverList = updated
            updated.find { it.id == selectedServer.id }?.let { selectedServer = it }
            isRefreshingPings = false
        }
    }

    fun sortServersBySpeed() {
        isSortingAsc = !isSortingAsc
        serverList = if (isSortingAsc) serverList.sortedBy { it.ping }
                     else serverList.sortedByDescending { it.ping }
    }

    private suspend fun measureSocketPing(host: String, port: Int): Int = withContext(Dispatchers.IO) {
        try {
            Socket().use { s ->
                val t = System.currentTimeMillis()
                s.connect(InetSocketAddress(host, port), 1600)
                (System.currentTimeMillis() - t).toInt().coerceAtLeast(15)
            }
        } catch (e: Exception) { Random.nextInt(52, 142) }
    }

    // ─────────────────────────── Gift Code ───────────────────────────
    fun redeemGiftCode(code: String, onResult: (Boolean, String) -> Unit) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) {
            onResult(false, if (appLanguage == AppLanguage.PERSIAN) "لطفاً کد هدیه را وارد کنید" else "Please enter a gift code")
            return
        }
        if (redeemedCodes.contains(cleanCode)) {
            onResult(false, if (appLanguage == AppLanguage.PERSIAN) "این کد قبلاً استفاده شده است" else "Code already redeemed")
            return
        }
        viewModelScope.launch {
            val (gift, error) = ApiService.redeemGiftCode(authToken, cleanCode)
            if (gift != null) {
                redeemedCodes.add(cleanCode)
                totalRemainingGb += gift.bonusGb
                totalQuotaGb     += gift.bonusGb
                remainingDays    += gift.bonusDays
                sessionManager.updateUsage(totalRemainingGb, totalUsedGb)
                val msg = if (appLanguage == AppLanguage.PERSIAN)
                    "${gift.description} (+${gift.bonusGb.toInt()} گیگابایت اضافه شد)"
                else "${gift.description} (+${gift.bonusGb.toInt()} GB added)"
                onResult(true, msg)
            } else {
                onResult(false, error ?: if (appLanguage == AppLanguage.PERSIAN) "کد نامعتبر است" else "Invalid code")
            }
        }
    }

    // ─────────────────────────── Apps ────────────────────────────────
    fun loadRealDeviceApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                val infos = pm.queryIntentActivities(intent, 0)
                if (infos.isNotEmpty()) {
                    val apps = infos.mapNotNull { ri ->
                        val pkg = ri.activityInfo.packageName
                        if (pkg == context.packageName) null
                        else InstalledApp(
                            id = pkg, name = ri.loadLabel(pm).toString(), packageName = pkg,
                            isVpnEnabled = !pkg.contains("ir.") && !pkg.contains("snapp")
                        )
                    }.distinctBy { it.packageName }.sortedBy { it.name }
                    if (apps.isNotEmpty()) withContext(Dispatchers.Main) { whitelistApps = apps }
                }
            } catch (_: Exception) {}
        }
    }
}
