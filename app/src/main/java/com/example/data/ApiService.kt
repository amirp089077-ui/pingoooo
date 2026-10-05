package com.example.data

import android.util.Log
import com.example.ui.viewmodel.AppServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// ─── مدل‌های response ────────────────────────────────────────────────

data class ApiUserProfile(
    val username: String,
    val remainingGb: Float,
    val usedGb: Float,
    val totalQuotaGb: Float,
    val remainingDays: Int,
    val totalDays: Int,
    val planType: String,
    val maxDevices: Int,
    val activeDevices: Int,
    val expiryDate: String,
    val status: String
)

data class ApiLoginResult(
    val token: String,
    val profile: ApiUserProfile
)

data class ApiAppConfig(
    val telegramSupport: String,
    val channelUrl: String,
    val announcement: String,
    val appVersion: String,
    val forceUpdate: Boolean
)

data class ApiGiftCode(
    val code: String,
    val bonusGb: Float,
    val bonusDays: Int,
    val description: String
)

// ─── سرویس ───────────────────────────────────────────────────────────

object ApiService {

    private const val TAG = "ApiService"

    // سرور شخصی — پورت 80 از طریق nginx reverse proxy
    private const val BASE_URL = "http://179.237.79.75"

    // ─── Auth ────────────────────────────────────────────────────────

    suspend fun login(username: String, password: String): ApiLoginResult? =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                }
                val resp = httpPost("$BASE_URL/api/auth/login", body.toString()) ?: return@withContext null
                val json = JSONObject(resp)

                val profile = ApiUserProfile(
                    username      = json.optString("username"),
                    remainingGb   = json.optDouble("remaining_gb", 0.0).toFloat(),
                    usedGb        = json.optDouble("used_gb", 0.0).toFloat(),
                    totalQuotaGb  = json.optDouble("total_quota_gb", 0.0).toFloat(),
                    remainingDays = json.optInt("remaining_days", 0),
                    totalDays     = json.optInt("total_days", 30),
                    planType      = json.optString("plan_type", "MULTI_USER"),
                    maxDevices    = json.optInt("max_devices", 2),
                    activeDevices = json.optInt("active_devices", 1),
                    expiryDate    = json.optString("expiry_date", ""),
                    status        = json.optString("status", "ACTIVE")
                )
                ApiLoginResult(token = json.optString("token"), profile = profile)
            } catch (e: Exception) {
                Log.e(TAG, "login failed", e)
                null
            }
        }

    /** خطای login رو با پیام برمیگردونه */
    suspend fun loginWithError(username: String, password: String): Pair<ApiLoginResult?, String?> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                }
                val (status, resp) = httpPostWithStatus("$BASE_URL/api/auth/login", body.toString())
                if (resp == null) return@withContext Pair(null, "خطا در اتصال به سرور")

                val json = JSONObject(resp)
                if (status !in 200..299) {
                    val detail = json.optString("detail", "خطای ناشناخته")
                    return@withContext Pair(null, detail)
                }

                val profile = ApiUserProfile(
                    username      = json.optString("username"),
                    remainingGb   = json.optDouble("remaining_gb", 0.0).toFloat(),
                    usedGb        = json.optDouble("used_gb", 0.0).toFloat(),
                    totalQuotaGb  = json.optDouble("total_quota_gb", 0.0).toFloat(),
                    remainingDays = json.optInt("remaining_days", 0),
                    totalDays     = json.optInt("total_days", 30),
                    planType      = json.optString("plan_type", "MULTI_USER"),
                    maxDevices    = json.optInt("max_devices", 2),
                    activeDevices = json.optInt("active_devices", 1),
                    expiryDate    = json.optString("expiry_date", ""),
                    status        = json.optString("status", "ACTIVE")
                )
                Pair(ApiLoginResult(token = json.optString("token"), profile = profile), null)
            } catch (e: Exception) {
                Log.e(TAG, "loginWithError failed", e)
                Pair(null, "خطا در اتصال به سرور")
            }
        }

    suspend fun logout(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            httpPost("$BASE_URL/api/auth/logout", "{}", token = token) != null
        } catch (e: Exception) {
            false
        }
    }

    // ─── User ────────────────────────────────────────────────────────

    suspend fun fetchUserProfile(token: String): ApiUserProfile? = withContext(Dispatchers.IO) {
        try {
            val resp = httpGet("$BASE_URL/api/users/me", token) ?: return@withContext null
            val json = JSONObject(resp)
            ApiUserProfile(
                username      = json.optString("username"),
                remainingGb   = json.optDouble("remaining_gb", 0.0).toFloat(),
                usedGb        = json.optDouble("used_gb", 0.0).toFloat(),
                totalQuotaGb  = json.optDouble("total_quota_gb", 0.0).toFloat(),
                remainingDays = json.optInt("remaining_days", 0),
                totalDays     = json.optInt("total_days", 30),
                planType      = json.optString("plan_type", "MULTI_USER"),
                maxDevices    = json.optInt("max_devices", 2),
                activeDevices = json.optInt("active_devices", 1),
                expiryDate    = json.optString("expiry_date", ""),
                status        = json.optString("status", "ACTIVE")
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchUserProfile failed", e)
            null
        }
    }

    suspend fun updateUserUsage(token: String, usedGb: Float, remainingGb: Float): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("used_gb", usedGb.toDouble())
                    put("remaining_gb", remainingGb.toDouble())
                }
                httpPatch("$BASE_URL/api/users/me/usage", body.toString(), token) != null
            } catch (e: Exception) {
                Log.e(TAG, "updateUserUsage failed", e)
                false
            }
        }

    suspend fun updateActiveDevices(token: String, activeDevices: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply { put("active_devices", activeDevices) }
                httpPatch("$BASE_URL/api/users/me/devices", body.toString(), token) != null
            } catch (e: Exception) {
                Log.e(TAG, "updateActiveDevices failed", e)
                false
            }
        }

    // ─── Servers ─────────────────────────────────────────────────────

    suspend fun fetchServers(token: String): List<AppServer>? = withContext(Dispatchers.IO) {
        try {
            val resp = httpGet("$BASE_URL/api/servers", token) ?: return@withContext null
            val json = JSONObject(resp)
            val arr: JSONArray = json.optJSONArray("servers") ?: return@withContext null
            val list = mutableListOf<AppServer>()
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                list.add(
                    AppServer(
                        id         = s.optString("id"),
                        country    = s.optString("country"),
                        city       = s.optString("city"),
                        flag       = s.optString("flag", "🌐"),
                        host       = s.optString("host"),
                        port       = s.optInt("port", 443),
                        badge      = s.optString("badge").takeIf { it.isNotBlank() },
                        emoji      = s.optString("emoji", ""),
                        isPro      = s.optBoolean("is_pro", false),
                        configUri  = s.optString("config_uri", ""),
                        ping       = s.optInt("ping", 50)
                    )
                )
            }
            list.ifEmpty { null }
        } catch (e: Exception) {
            Log.e(TAG, "fetchServers failed", e)
            null
        }
    }

    // ─── Config ──────────────────────────────────────────────────────

    suspend fun fetchAppConfig(): ApiAppConfig? = withContext(Dispatchers.IO) {
        try {
            val resp = httpGet("$BASE_URL/api/config") ?: return@withContext null
            val json = JSONObject(resp)
            ApiAppConfig(
                telegramSupport = json.optString("telegram_support", "AlphaSupport_ir"),
                channelUrl      = json.optString("channel_url", "https://t.me/AlphaSupport_ir"),
                announcement    = json.optString("announcement", ""),
                appVersion      = json.optString("app_version", "1.0.0"),
                forceUpdate     = json.optBoolean("force_update", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchAppConfig failed", e)
            null
        }
    }

    // ─── Gift Codes ──────────────────────────────────────────────────

    suspend fun redeemGiftCode(token: String, code: String): Pair<ApiGiftCode?, String?> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply { put("code", code) }
                val (status, resp) = httpPostWithStatus("$BASE_URL/api/giftcodes/redeem", body.toString(), token)
                if (resp == null) return@withContext Pair(null, "خطا در اتصال به سرور")
                val json = JSONObject(resp)
                if (status !in 200..299) {
                    return@withContext Pair(null, json.optString("detail", "کد نامعتبر است"))
                }
                val gift = ApiGiftCode(
                    code        = json.optString("code"),
                    bonusGb     = json.optDouble("bonus_gb", 0.0).toFloat(),
                    bonusDays   = json.optInt("bonus_days", 0),
                    description = json.optString("description", "کد هدیه فعال شد")
                )
                Pair(gift, null)
            } catch (e: Exception) {
                Log.e(TAG, "redeemGiftCode failed", e)
                Pair(null, "خطا در اتصال به سرور")
            }
        }

    // ─── HTTP Helpers ────────────────────────────────────────────────

    private fun httpGet(urlStr: String, token: String? = null): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                if (token != null) setRequestProperty("Authorization", "Bearer $token")
            }
            if (conn.responseCode in 200..299) readStream(conn) else null
        } catch (e: Exception) {
            Log.e(TAG, "GET $urlStr failed", e)
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpPost(urlStr: String, jsonBody: String, token: String? = null): String? {
        return httpPostWithStatus(urlStr, jsonBody, token).second
    }

    private fun httpPostWithStatus(urlStr: String, jsonBody: String, token: String? = null): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                if (token != null) setRequestProperty("Authorization", "Bearer $token")
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(jsonBody); it.flush() }
            val code = conn.responseCode
            val body = if (code in 200..299) readStream(conn) else readErrorStream(conn)
            Pair(code, body)
        } catch (e: Exception) {
            Log.e(TAG, "POST $urlStr failed", e)
            Pair(0, null)
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpPatch(urlStr: String, jsonBody: String, token: String? = null): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("X-HTTP-Method-Override", "PATCH")
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                if (token != null) setRequestProperty("Authorization", "Bearer $token")
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(jsonBody); it.flush() }
            if (conn.responseCode in 200..299) readStream(conn) else null
        } catch (e: Exception) {
            Log.e(TAG, "PATCH $urlStr failed", e)
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun readStream(conn: HttpURLConnection): String =
        BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }

    private fun readErrorStream(conn: HttpURLConnection): String? =
        try { BufferedReader(InputStreamReader(conn.errorStream, "UTF-8")).use { it.readText() } }
        catch (e: Exception) { null }
}
