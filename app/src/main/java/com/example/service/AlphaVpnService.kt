package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.viewmodel.VpnStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AlphaVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null
    private var coreController: CoreController? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "AlphaVpnService"
        const val ACTION_CONNECT    = "com.example.alphavpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.alphavpn.DISCONNECT"

        const val EXTRA_SERVER_HOST     = "extra_server_host"
        const val EXTRA_SERVER_PORT     = "extra_server_port"
        const val EXTRA_SERVER_CITY     = "extra_server_city"
        const val EXTRA_DNS_SERVER      = "extra_dns_server"
        const val EXTRA_CONFIG_URI      = "extra_config_uri"
        const val EXTRA_DISALLOWED_APPS = "extra_disallowed_apps"

        private const val NOTIFICATION_CHANNEL_ID = "alpha_vpn_service_channel"
        private const val NOTIFICATION_ID         = 1001
        private const val XRAY_SOCKS_PORT         = 10808
        private const val XRAY_HTTP_PORT          = 10809

        fun startVpn(
            context: Context,
            serverHost: String,
            serverPort: Int,
            serverCity: String,
            dnsServer: String,
            configUri: String = "",
            disallowedApps: ArrayList<String> = arrayListOf()
        ) {
            val intent = Intent(context, AlphaVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_SERVER_HOST, serverHost)
                putExtra(EXTRA_SERVER_PORT, serverPort)
                putExtra(EXTRA_SERVER_CITY, serverCity)
                putExtra(EXTRA_DNS_SERVER, dnsServer)
                putExtra(EXTRA_CONFIG_URI, configUri)
                putStringArrayListExtra(EXTRA_DISALLOWED_APPS, disallowedApps)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        fun stopVpn(context: Context) {
            context.startService(Intent(context, AlphaVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val host       = intent.getStringExtra(EXTRA_SERVER_HOST) ?: return START_NOT_STICKY
                val port       = intent.getIntExtra(EXTRA_SERVER_PORT, 443)
                val city       = intent.getStringExtra(EXTRA_SERVER_CITY) ?: "Server"
                val dns        = intent.getStringExtra(EXTRA_DNS_SERVER)  ?: "1.1.1.1"
                val configUri  = intent.getStringExtra(EXTRA_CONFIG_URI)  ?: ""
                val disallowed = intent.getStringArrayListExtra(EXTRA_DISALLOWED_APPS) ?: arrayListOf()
                startVpnTunnel(host, port, city, dns, configUri, disallowed)
            }
            ACTION_DISCONNECT -> { stopVpnTunnel(); stopSelf() }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() { stopVpnTunnel(); super.onDestroy() }
    override fun onRevoke()  { stopVpnTunnel(); super.onRevoke()  }

    // ── Start ──────────────────────────────────────────────────────────────

    private fun startVpnTunnel(
        host: String, port: Int, city: String,
        dnsServer: String, configUri: String, disallowedApps: List<String>
    ) {
        AlphaVpnState.updateStatus(VpnStatus.CONNECTING)
        startForeground(NOTIFICATION_ID, buildNotification("در حال اتصال به $city..."))

        serviceJob?.cancel()
        serviceJob = scope.launch {
            try {
                val parsed = VpnConfigParser.parse(configUri, host, port)
                Log.i(TAG, "Connecting: ${parsed.protocol} → ${parsed.serverHost}:${parsed.serverPort}")

                // 1. آماده‌سازی assets
                val assetDir = prepareAssets()
                Libv2ray.initCoreEnv(assetDir.absolutePath, "")

                // 2. ساخت TUN interface
                val tun = buildTunInterface(dnsServer, disallowedApps)
                if (tun == null) {
                    Log.e(TAG, "TUN establish failed")
                    AlphaVpnState.updateStatus(VpnStatus.DISCONNECTED)
                    stopSelf(); return@launch
                }
                tunInterface = tun
                Log.i(TAG, "TUN fd=${tun.fd}")

                // 3. ساخت xray config
                val config = buildXrayConfig(parsed, dnsServer)

                // 4. راه‌اندازی xray-core با TUN fd
                val cb = object : CoreCallbackHandler {
                    override fun startup(): Long = 0L
                    override fun shutdown(): Long = 0L
                    override fun onEmitStatus(s: Long, msg: String): Long {
                        Log.i(TAG, "xray[$s]: $msg"); return 0L
                    }
                }
                coreController = Libv2ray.newCoreController(cb)
                coreController!!.startLoop(config, tun.fd)

                Log.i(TAG, "xray-core started")
                AlphaVpnState.updateStatus(VpnStatus.CONNECTED)
                updateNotification("متصل به $city (${parsed.protocol.uppercase()})")

                // 5. speed loop
                runSpeedLoop()

            } catch (e: Exception) {
                Log.e(TAG, "VPN start failed", e)
                AlphaVpnState.updateStatus(VpnStatus.DISCONNECTED)
                stopSelf()
            }
        }
    }

    // ── TUN ────────────────────────────────────────────────────────────────

    private fun buildTunInterface(dnsServer: String, disallowedApps: List<String>): ParcelFileDescriptor? {
        val builder = Builder()
            .setSession("Alpha VPN")
            .addAddress("10.8.0.2", 24)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .setMtu(1500)

        val cleanDns = dnsServer.substringAfter("(").substringBefore(")").trim().ifEmpty { "1.1.1.1" }
        try { builder.addDnsServer(cleanDns) } catch (_: Exception) { builder.addDnsServer("1.1.1.1") }
        builder.addDnsServer("8.8.8.8")

        for (pkg in disallowedApps) {
            try { builder.addDisallowedApplication(pkg) } catch (_: Exception) {}
        }
        return builder.establish()
    }

    // ── xray config ────────────────────────────────────────────────────────

    private fun buildXrayConfig(parsed: ParsedVpnConfig, dnsServer: String): String {
        val cleanDns = dnsServer.substringAfter("(").substringBefore(")").trim().ifEmpty { "1.1.1.1" }

        val log    = JSONObject().put("loglevel", "warning")
        val stats  = JSONObject()
        val policy = JSONObject().put("system", JSONObject()
            .put("statsOutboundUplink", true)
            .put("statsOutboundDownlink", true))

        val inbounds = JSONArray().apply {
            put(JSONObject().apply {
                put("tag", "socks-in"); put("port", XRAY_SOCKS_PORT); put("listen", "127.0.0.1")
                put("protocol", "socks")
                put("settings", JSONObject().put("udp", true).put("auth", "noauth"))
                put("sniffing", JSONObject().put("enabled", true)
                    .put("destOverride", JSONArray().put("http").put("tls")).put("routeOnly", true))
            })
            put(JSONObject().apply {
                put("tag", "http-in"); put("port", XRAY_HTTP_PORT); put("listen", "127.0.0.1")
                put("protocol", "http")
            })
        }

        val outbounds = JSONArray().apply {
            put(buildProxyOutbound(parsed))
            put(JSONObject().put("tag", "direct").put("protocol", "freedom")
                .put("settings", JSONObject().put("domainStrategy", "UseIPv4")))
            put(JSONObject().put("tag", "blocked").put("protocol", "blackhole")
                .put("settings", JSONObject()))
        }

        val dns = JSONObject().put("servers", JSONArray().put(cleanDns).put("8.8.8.8").put("localhost"))
            .put("queryStrategy", "UseIPv4")

        val routing = JSONObject().put("domainStrategy", "IPIfNonMatch").put("rules", JSONArray().apply {
            put(JSONObject().put("type", "field").put("outboundTag", "direct")
                .put("ip", JSONArray().put("geoip:private").put("10.0.0.0/8")
                    .put("172.16.0.0/12").put("192.168.0.0/16")))
        })

        return JSONObject().put("log", log).put("stats", stats).put("policy", policy)
            .put("inbounds", inbounds).put("outbounds", outbounds)
            .put("dns", dns).put("routing", routing).toString(2)
    }

    private fun buildProxyOutbound(p: ParsedVpnConfig): JSONObject = when (p.protocol.lowercase()) {
        "vless" -> JSONObject().put("tag", "proxy").put("protocol", "vless").put("settings",
            JSONObject().put("vnext", JSONArray().put(JSONObject().put("address", p.serverHost)
                .put("port", p.serverPort).put("users", JSONArray().put(JSONObject()
                    .put("id", p.uuid).put("encryption", "none").put("flow", p.flow))))
            )).put("streamSettings", buildStream(p))

        "vmess" -> JSONObject().put("tag", "proxy").put("protocol", "vmess").put("settings",
            JSONObject().put("vnext", JSONArray().put(JSONObject().put("address", p.serverHost)
                .put("port", p.serverPort).put("users", JSONArray().put(JSONObject()
                    .put("id", p.uuid).put("alterId", 0).put("security", "auto"))))
            )).put("streamSettings", buildStream(p))

        "trojan" -> JSONObject().put("tag", "proxy").put("protocol", "trojan").put("settings",
            JSONObject().put("servers", JSONArray().put(JSONObject().put("address", p.serverHost)
                .put("port", p.serverPort).put("password", p.password)))
            ).put("streamSettings", buildStream(p))

        "ss", "shadowsocks" -> JSONObject().put("tag", "proxy").put("protocol", "shadowsocks")
            .put("settings", JSONObject().put("servers", JSONArray().put(JSONObject()
                .put("address", p.serverHost).put("port", p.serverPort)
                .put("method", p.method.ifEmpty { "chacha20-ietf-poly1305" })
                .put("password", p.password))))

        else -> JSONObject().put("tag", "proxy").put("protocol", "freedom").put("settings", JSONObject())
    }

    private fun buildStream(p: ParsedVpnConfig): JSONObject {
        val obj = JSONObject().put("network", p.network.ifEmpty { "tcp" })
        when (p.security.lowercase()) {
            "tls" -> obj.put("security", "tls").put("tlsSettings", JSONObject().apply {
                if (p.sni.isNotEmpty()) put("serverName", p.sni)
                put("allowInsecure", false)
                if (p.alpn.isNotEmpty()) put("alpn", JSONArray().apply { p.alpn.split(",").forEach { put(it.trim()) } })
            })
            "reality" -> obj.put("security", "reality").put("realitySettings", JSONObject()
                .put("serverName", p.sni).put("publicKey", p.publicKey)
                .put("shortId", p.shortId).put("fingerprint", p.fingerprint.ifEmpty { "chrome" }))
            else -> obj.put("security", "none")
        }
        if (p.network == "ws") obj.put("wsSettings", JSONObject().put("path", p.path.ifEmpty { "/" })
            .put("headers", JSONObject().apply { if (p.host.isNotEmpty()) put("Host", p.host) }))
        if (p.network == "grpc") obj.put("grpcSettings", JSONObject().put("serviceName", p.path))
        return obj
    }

    // ── Assets ─────────────────────────────────────────────────────────────

    private fun prepareAssets(): File {
        val dir = File(filesDir, "xray-assets").also { it.mkdirs() }
        for (name in listOf("geoip.dat", "geosite.dat")) {
            val dest = File(dir, name)
            if (!dest.exists()) {
                try { assets.open(name).use { s -> dest.outputStream().use { s.copyTo(it) } }
                } catch (e: Exception) { Log.w(TAG, "asset $name: ${e.message}") }
            }
        }
        return dir
    }

    // ── Speed loop ─────────────────────────────────────────────────────────

    private suspend fun runSpeedLoop() {
        var totalRx = 0L; var totalTx = 0L
        while (scope.isActive && tunInterface != null) {
            delay(1000)
            val stats = try { coreController?.queryAllOutboundTrafficStats() ?: "" } catch (_: Exception) { "" }
            var rx = 0L; var tx = 0L
            stats.split(";").forEach { e ->
                if (e.isBlank()) return@forEach
                val p = e.split(",")
                if (p.size >= 3) when (p[1]) {
                    "downlink" -> rx += p[2].toLongOrNull() ?: 0L
                    "uplink"   -> tx += p[2].toLongOrNull() ?: 0L
                }
            }
            totalRx += rx; totalTx += tx
            AlphaVpnState.updateSpeeds(rx / (1024f * 1024f), tx / (1024f * 1024f), totalRx + totalTx)
        }
    }

    // ── Stop ───────────────────────────────────────────────────────────────

    private fun stopVpnTunnel() {
        serviceJob?.cancel(); serviceJob = null
        try { coreController?.stopLoop() } catch (e: Exception) { Log.e(TAG, "stop xray", e) }
        coreController = null
        try { tunInterface?.close() } catch (e: Exception) { Log.e(TAG, "close TUN", e) }
        tunInterface = null
        AlphaVpnState.reset()
        stopForeground(true)
        Log.i(TAG, "VPN stopped")
    }

    // ── Notification ───────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(NOTIFICATION_CHANNEL_ID, "Alpha VPN", NotificationManager.IMPORTANCE_LOW)
                    .also { it.setShowBadge(false) })
    }

    private fun buildNotification(content: String): Notification =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Alpha VPN").setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(PendingIntent.getActivity(this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).build()

    private fun updateNotification(content: String) =
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification(content))
}
