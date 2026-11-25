package dev.rffrr.mateli.core

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Where the tunnel exits, as reported by api.ip2location.io. */
data class ExitInfo(
    val ip: String,
    val countryCode: String,
    val country: String,
    val region: String
)

object ExitIp {
    /**
     * Looks up the caller's public IP. Without a key the free tier applies. The request goes through
     * the tunnel while connected, so the answer is the exit address. Call from a background thread.
     */
    fun lookup(key: String): ExitInfo? {
        return runCatching {
            val url = buildString {
                append("https://api.ip2location.io/?format=json")
                if (key.isNotBlank()) {
                    append("&key=").append(URLEncoder.encode(key.trim(), "UTF-8"))
                }
            }
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("User-Agent", "MatenLi/0.1")
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                ExitInfo(
                    ip = json.optString("ip"),
                    countryCode = json.optString("country_code"),
                    country = json.optString("country_name"),
                    region = json.optString("region_name")
                )
            } finally {
                connection.disconnect()
            }
        }.getOrNull()?.takeIf { it.ip.isNotBlank() }
    }
}
