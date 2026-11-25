package dev.rffrr.mateli.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.Locale

/** Real network probes. They measure whatever path the device currently uses. */
object Probes {

    /** Times one request to [url]. Returns milliseconds, or null when the request fails. */
    suspend fun urlTest(url: String): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                connection.instanceFollowRedirects = true
                val start = System.nanoTime()
                val code = connection.responseCode
                val elapsed = (System.nanoTime() - start) / 1_000_000
                if (code in 200..399) elapsed else null
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    /** Times a TCP connect to host:port. Returns milliseconds, or null when it fails. */
    suspend fun tcpPing(host: String, port: Int): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 5_000)
            }
            (System.nanoTime() - start) / 1_000_000
        }.getOrNull()
    }
}

/** Human readable byte count, for example 1.4 MB. */
fun formatBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.size - 1) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes B" else String.format(Locale.US, "%.1f %s", value, units[unit])
}

/** Human readable speed, for example 320.5 KB/s. */
fun formatSpeed(bytesPerSecond: Long): String = formatBytes(bytesPerSecond) + "/s"
