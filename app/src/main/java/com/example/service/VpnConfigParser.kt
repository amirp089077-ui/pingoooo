package com.example.service

import android.net.Uri
import android.util.Base64
import android.util.Log
import org.json.JSONObject

data class ParsedVpnConfig(
    val protocol: String = "vless",
    val uuidOrPassword: String = "",
    val serverHost: String = "",
    val serverPort: Int = 443,
    val sni: String = "",
    val path: String = "",
    val transportType: String = "tcp",
    val security: String = "tls",
    val remarks: String = ""
)

object VpnConfigParser {
    private const val TAG = "VpnConfigParser"

    fun parse(configUri: String?, fallbackHost: String, fallbackPort: Int): ParsedVpnConfig {
        if (configUri.isNullOrBlank()) {
            return ParsedVpnConfig(
                protocol = "direct",
                serverHost = fallbackHost,
                serverPort = fallbackPort
            )
        }

        val trimmed = configUri.trim()
        return try {
            when {
                trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
                trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
                trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
                trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed)
                else -> ParsedVpnConfig(
                    protocol = "raw",
                    serverHost = fallbackHost,
                    serverPort = fallbackPort,
                    remarks = trimmed
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse configUri, falling back to direct host:port: ${e.message}")
            ParsedVpnConfig(
                protocol = "fallback",
                serverHost = fallbackHost,
                serverPort = fallbackPort
            )
        }
    }

    private fun parseVless(uriString: String): ParsedVpnConfig {
        val uri = Uri.parse(uriString)
        val userInfo = uri.userInfo ?: ""
        val host = uri.host ?: ""
        val port = if (uri.port > 0) uri.port else 443
        val query = uri.query ?: ""
        val fragment = uri.fragment ?: ""

        val params = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) ?: "" }
        val security = params["security"] ?: "reality"
        val sni = params["sni"] ?: params["host"] ?: host
        val type = params["type"] ?: "tcp"
        val path = params["path"] ?: ""

        return ParsedVpnConfig(
            protocol = "vless",
            uuidOrPassword = userInfo,
            serverHost = host,
            serverPort = port,
            sni = sni,
            path = path,
            transportType = type,
            security = security,
            remarks = fragment
        )
    }

    private fun parseTrojan(uriString: String): ParsedVpnConfig {
        val uri = Uri.parse(uriString)
        val password = uri.userInfo ?: ""
        val host = uri.host ?: ""
        val port = if (uri.port > 0) uri.port else 443
        val sni = uri.getQueryParameter("sni") ?: host
        val type = uri.getQueryParameter("type") ?: "tcp"

        return ParsedVpnConfig(
            protocol = "trojan",
            uuidOrPassword = password,
            serverHost = host,
            serverPort = port,
            sni = sni,
            transportType = type,
            security = "tls",
            remarks = uri.fragment ?: ""
        )
    }

    private fun parseVmess(uriString: String): ParsedVpnConfig {
        val rawBase64 = uriString.substringAfter("vmess://").trim()
        val decoded = String(Base64.decode(rawBase64, Base64.DEFAULT), Charsets.UTF_8)
        val json = JSONObject(decoded)

        return ParsedVpnConfig(
            protocol = "vmess",
            uuidOrPassword = json.optString("id"),
            serverHost = json.optString("add"),
            serverPort = json.optInt("port", 443),
            sni = json.optString("sni", json.optString("host")),
            path = json.optString("path"),
            transportType = json.optString("net", "tcp"),
            security = json.optString("tls", "tls"),
            remarks = json.optString("ps")
        )
    }

    private fun parseShadowsocks(uriString: String): ParsedVpnConfig {
        val afterScheme = uriString.substringAfter("ss://")
        val mainPart = afterScheme.substringBefore("#")
        val remarks = if (afterScheme.contains("#")) afterScheme.substringAfter("#") else ""

        return if (mainPart.contains("@")) {
            val user = mainPart.substringBefore("@")
            val hostPort = mainPart.substringAfter("@")
            val host = hostPort.substringBefore(":")
            val port = hostPort.substringAfter(":").toIntOrNull() ?: 443
            ParsedVpnConfig(
                protocol = "shadowsocks",
                uuidOrPassword = user,
                serverHost = host,
                serverPort = port,
                remarks = remarks
            )
        } else {
            val decoded = String(Base64.decode(mainPart, Base64.DEFAULT), Charsets.UTF_8)
            val user = decoded.substringBefore("@")
            val hostPort = decoded.substringAfter("@")
            val host = hostPort.substringBefore(":")
            val port = hostPort.substringAfter(":").toIntOrNull() ?: 443
            ParsedVpnConfig(
                protocol = "shadowsocks",
                uuidOrPassword = user,
                serverHost = host,
                serverPort = port,
                remarks = remarks
            )
        }
    }
}
