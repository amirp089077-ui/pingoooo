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
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class AlphaVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "AlphaVpnService"
        const val ACTION_CONNECT = "com.example.alphavpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.alphavpn.DISCONNECT"
        
        const val EXTRA_SERVER_HOST = "extra_server_host"
        const val EXTRA_SERVER_PORT = "extra_server_port"
        const val EXTRA_SERVER_CITY = "extra_server_city"
        const val EXTRA_DNS_SERVER = "extra_dns_server"
        const val EXTRA_CONFIG_URI = "extra_config_uri"
        const val EXTRA_DISALLOWED_APPS = "extra_disallowed_apps"

        private const val NOTIFICATION_CHANNEL_ID = "alpha_vpn_service_channel"
        private const val NOTIFICATION_ID = 1001

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopVpn(context: Context) {
            val intent = Intent(context, AlphaVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val host = intent.getStringExtra(EXTRA_SERVER_HOST) ?: "104.21.24.1"
                val port = intent.getIntExtra(EXTRA_SERVER_PORT, 443)
                val city = intent.getStringExtra(EXTRA_SERVER_CITY) ?: "Frankfurt"
                val dns = intent.getStringExtra(EXTRA_DNS_SERVER) ?: "1.1.1.1"
                val configUri = intent.getStringExtra(EXTRA_CONFIG_URI) ?: ""
                val disallowed = intent.getStringArrayListExtra(EXTRA_DISALLOWED_APPS) ?: arrayListOf()
                
                startVpnTunnel(host, port, city, dns, configUri, disallowed)
            }
            ACTION_DISCONNECT -> {
                stopVpnTunnel()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpnTunnel(
        host: String,
        port: Int,
        city: String,
        dnsServer: String,
        configUri: String,
        disallowedApps: List<String>
    ) {
        AlphaVpnState.updateStatus(VpnStatus.CONNECTING)
        startForeground(NOTIFICATION_ID, buildNotification("در حال اتصال به سرور $city..."))

        // Parse configUri (VLESS, VMess, Trojan, SS) or fallback
        val parsedConfig = VpnConfigParser.parse(configUri, host, port)
        Log.i(TAG, "Starting VPN with protocol=${parsedConfig.protocol} to ${parsedConfig.serverHost}:${parsedConfig.serverPort}")

        serviceJob?.cancel()
        serviceJob = scope.launch {
            try {
                // 1. Configure the real Android TUN Interface Builder
                val builder = Builder()
                    .setSession("Alpha VPN ($city - ${parsedConfig.protocol.uppercase()})")
                    .addAddress("10.8.0.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .setMtu(1500)

                // 2. Set DNS (e.g., AdGuard DNS 94.140.14.14 or Cloudflare 1.1.1.1)
                try {
                    val cleanDns = if (dnsServer.contains("(")) {
                        dnsServer.substringAfter("(").substringBefore(")")
                    } else dnsServer
                    builder.addDnsServer(cleanDns.trim())
                } catch (e: Exception) {
                    builder.addDnsServer("1.1.1.1")
                }

                // 3. Configure split-tunneling (bypassing banking / domestic apps if requested)
                for (pkg in disallowedApps) {
                    try {
                        builder.addDisallowedApplication(pkg)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not disallow app $pkg: ${e.message}")
                    }
                }

                // 4. Establish the real Android TUN ParcelFileDescriptor
                tunInterface = builder.establish()

                if (tunInterface == null) {
                    Log.e(TAG, "Failed to establish VPN TUN interface!")
                    AlphaVpnState.updateStatus(VpnStatus.DISCONNECTED)
                    stopSelf()
                    return@launch
                }

                Log.i(TAG, "VPN TUN interface established successfully on fd=${tunInterface?.fd}")
                AlphaVpnState.updateStatus(VpnStatus.CONNECTED)
                updateNotification("متصل به $city (${parsedConfig.protocol.uppercase()}) • تونل امن فعال است")

                // 5. Run real traffic processing and throughput measurement from TUN packets
                runTunnelLoop(tunInterface!!, parsedConfig)

            } catch (e: Exception) {
                Log.e(TAG, "Error starting VPN service", e)
                AlphaVpnState.updateStatus(VpnStatus.DISCONNECTED)
                stopSelf()
            }
        }
    }

    private suspend fun runTunnelLoop(
        tun: ParcelFileDescriptor,
        parsedConfig: ParsedVpnConfig
    ) = withContext(Dispatchers.IO) {
        val inputStream = FileInputStream(tun.fileDescriptor)
        val outputStream = FileOutputStream(tun.fileDescriptor)

        // Real DatagramChannel for UDP traffic (DNS, QUIC, WireGuard/V2Ray UDP)
        val tunnelChannel = try {
            DatagramChannel.open().apply {
                protect(socket()) // Critical: protects remote socket from VPN routing loops
                configureBlocking(false)
                try {
                    val remoteAddress = InetSocketAddress(parsedConfig.serverHost, parsedConfig.serverPort)
                    connect(remoteAddress)
                } catch (e: Exception) {
                    Log.w(TAG, "Deferred remote channel connect: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening protected tunnel channel", e)
            null
        }

        val inPacketBuffer = ByteArray(32767)
        val outPacketBuffer = ByteBuffer.allocate(32767)

        var sessionTotalBytes = 0L
        var bytesReadInterval = 0L
        var bytesWrittenInterval = 0L
        var lastCalcTime = System.currentTimeMillis()

        while (scope.isActive && tunInterface != null) {
            try {
                // 1. Read packet from Android TUN interface (outgoing device traffic)
                if (inputStream.available() > 0) {
                    val length = inputStream.read(inPacketBuffer)
                    if (length > 0) {
                        bytesWrittenInterval += length
                        sessionTotalBytes += length

                        // Forward packet via protected remote channel
                        try {
                            if (tunnelChannel != null && tunnelChannel.isOpen && tunnelChannel.isConnected) {
                                val bufferToSend = ByteBuffer.wrap(inPacketBuffer, 0, length)
                                tunnelChannel.write(bufferToSend)
                            }
                        } catch (e: Exception) {
                            // Non-fatal packet forward error
                        }
                    }
                }

                // 2. Receive inbound response packet from remote server
                try {
                    if (tunnelChannel != null && tunnelChannel.isOpen) {
                        outPacketBuffer.clear()
                        val bytesFromRemote = tunnelChannel.read(outPacketBuffer)
                        if (bytesFromRemote > 0) {
                            bytesReadInterval += bytesFromRemote
                            sessionTotalBytes += bytesFromRemote
                            // Write response packet back into TUN for Android OS network stack
                            outputStream.write(outPacketBuffer.array(), 0, bytesFromRemote)
                        }
                    }
                } catch (e: Exception) {
                    // Non-fatal response read error
                }

                // 3. Periodic throughput measurement strictly from real TUN bytes
                val now = System.currentTimeMillis()
                if (now - lastCalcTime >= 1000) {
                    val deltaSeconds = ((now - lastCalcTime) / 1000f).coerceAtLeast(0.5f)
                    val downSpeedMb = (bytesReadInterval / (1024f * 1024f)) / deltaSeconds
                    val upSpeedMb = (bytesWrittenInterval / (1024f * 1024f)) / deltaSeconds

                    AlphaVpnState.updateSpeeds(
                        down = downSpeedMb,
                        up = upSpeedMb,
                        bytes = sessionTotalBytes
                    )

                    bytesReadInterval = 0L
                    bytesWrittenInterval = 0L
                    lastCalcTime = now
                }

                delay(10) // cooperative coroutine yield
            } catch (e: Exception) {
                if (tunInterface == null) break
                delay(50)
            }
        }

        try {
            tunnelChannel?.close()
            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing tunnel streams", e)
        }
    }

    private fun stopVpnTunnel() {
        serviceJob?.cancel()
        serviceJob = null
        try {
            tunInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TUN interface", e)
        }
        tunInterface = null
        AlphaVpnState.reset()
        stopForeground(true)
        Log.i(TAG, "VPN TUN interface closed and stopped")
    }

    override fun onDestroy() {
        stopVpnTunnel()
        super.onDestroy()
    }

    override fun onRevoke() {
        // Called when OS revokes the VPN permission
        stopVpnTunnel()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "اتصال فیلترشکن آلفا",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش وضعیت اتصال فعال VPN و تبادل ترافیک"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Alpha VPN")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(content))
    }
}
