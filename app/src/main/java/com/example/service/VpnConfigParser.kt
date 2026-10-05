package com.example.service

import android.net.Uri
import android.util.Base64
import android.util.Log
import org.json.JSONObject

data class ParsedVpnConfig(
    val protocol:    String = "vless",
    val uuid:        String = "",
    val password:    String = "",
    val serverHost:  String = "",
    val serverPort:  Int    = 443,
    val network:     String = "tcp",
    val security:    String = "tls",
    val sni:         String = "",
    val alpn:        String = "",
    val fingerprint: String = "chrome",
    val flow:        String = "",
    val publicKey:   String = "",
    val shortId:     String = "",
    val path:        String = "",
    val host:        String = "",
    val method:      String = "",
    val remarks:     String = ""
)

object VpnConfigParser {
    private const val TAG = "VpnConfigParser"

    fun parse(configUri: String?, fallbackHost: String, fallbackPort: Int): ParsedVpnConfig {
        if (configUri.isNullOrBlank()) return ParsedVpnConfig(
            protocol = "freedom", serverHost = fallbackHost, serverPort = fallbackPort)
        return try {
            val t = configUri.trim()
            when {
                t.startsWith("vless://",  true) -> parseVless(t)
                t.startsWith("vmess://",  true) -> parseVmess(t)
                t.startsWith("trojan://", true) -> parseTrojan(t)
                t.startsWith("ss://",     true) -> parseShadowsocks(t)
                else -> ParsedVpnConfig(protocol = "freedom", serverHost = fallbackHost, serverPort = fallbackPort)
            }
        } catch (e: Exception) {
            Log.w(TAG, "parse failed: ${e.message}")
            ParsedVpnConfig(protocol = "freedom", serverHost = fallbackHost, serverPort = fallbackPort)
        }
    }

    private fun parseVless(uri: String): ParsedVpnConfig {
        val u = Uri.parse(uri)
        val p = u.queryParameterNames.associateWith { u.getQueryParameter(it) ?: "" }
        return ParsedVpnConfig(
            protocol = "vless", uuid = u.userInfo ?: "",
            serverHost = u.host ?: "", serverPort = u.port.takeIf { it > 0 } ?: 443,
            network = p["type"] ?: "tcp", security = p["security"] ?: "tls",
            sni = p["sni"] ?: p["host"] ?: u.host ?: "", alpn = p["alpn"] ?: "",
            fingerprint = p["fp"] ?: "chrome", flow = p["flow"] ?: "",
            publicKey = p["pbk"] ?: "", shortId = p["sid"] ?: "",
            path = p["path"] ?: p["serviceName"] ?: "", host = p["host"] ?: "",
            remarks = u.fragment ?: "")
    }

    private fun parseVmess(uri: String): ParsedVpnConfig {
        val raw  = uri.substringAfter("vmess://").trim()
        val json = JSONObject(String(Base64.decode(raw, Base64.DEFAULT or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8))
        return ParsedVpnConfig(
            protocol = "vmess", uuid = json.optString("id"),
            serverHost = json.optString("add"), serverPort = json.optInt("port", 443),
            network = json.optString("net", "tcp"),
            security = json.optString("tls", "none").ifEmpty { "none" },
            sni = json.optString("sni", json.optString("host")),
            alpn = json.optString("alpn", ""), fingerprint = json.optString("fp", "chrome"),
            path = json.optString("path", ""), host = json.optString("host", ""),
            remarks = json.optString("ps", ""))
    }

    private fun parseTrojan(uri: String): ParsedVpnConfig {
        val u = Uri.parse(uri)
        val p = u.queryParameterNames.associateWith { u.getQueryParameter(it) ?: "" }
        return ParsedVpnConfig(
            protocol = "trojan", password = u.userInfo ?: "",
            serverHost = u.host ?: "", serverPort = u.port.takeIf { it > 0 } ?: 443,
            network = p["type"] ?: "tcp", security = "tls",
            sni = p["sni"] ?: u.host ?: "", alpn = p["alpn"] ?: "",
            fingerprint = p["fp"] ?: "chrome", path = p["path"] ?: "",
            host = p["host"] ?: "", remarks = u.fragment ?: "")
    }

    private fun parseShadowsocks(uri: String): ParsedVpnConfig {
        val after   = uri.substringAfter("ss://")
        val remarks = after.substringAfter("#", "")
        val main    = after.substringBefore("#")
        val (userPart, hostPart) = if (main.contains("@"))
            main.substringBefore("@") to main.substringAfter("@")
        else {
            val dec = String(Base64.decode(main, Base64.DEFAULT), Charsets.UTF_8)
            dec.substringBefore("@") to dec.substringAfter("@")
        }
        val (method, password) = try {
            if (userPart.contains(":")) userPart.substringBefore(":") to userPart.substringAfter(":")
            else {
                val d = String(Base64.decode(userPart, Base64.DEFAULT), Charsets.UTF_8)
                d.substringBefore(":") to d.substringAfter(":")
            }
        } catch (_: Exception) { "" to userPart }
        return ParsedVpnConfig(
            protocol = "ss", password = password, method = method,
            serverHost = hostPart.substringBefore(":"),
            serverPort = hostPart.substringAfterLast(":").toIntOrNull() ?: 443,
            security = "none", remarks = remarks)
    }
}
