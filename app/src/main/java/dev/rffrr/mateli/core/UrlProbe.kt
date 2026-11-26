package dev.rffrr.mateli.core

import android.content.Context
import dev.rffrr.mateli.config.buildProbeConfig
import dev.rffrr.mateli.config.unsupportedReason
import dev.rffrr.mateli.profiles.Profile
import dev.rffrr.mateli.settings.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.URL

/** Result of one URL test: the time in ms, or the reason it failed. */
data class UrlResult(val ms: Long?, val error: String?)

/**
 * URL tests for many profiles at once. The profiles run in a headless core in its own process
 * (see [ProbeService]), each on its own local port, and every request goes through that port.
 * Works connected or not.
 */
object UrlProbe {
    private const val CORE_START_TIMEOUT_MS = 10_000L
    private const val REQUEST_TIMEOUT_MS = 5_000
    private const val PARALLEL = 16
    private const val STOP_GAP_MS = 300L

    /** Returns a result for every profile. Profiles that cannot run here get an error, not a number. */
    suspend fun testAll(
        context: Context,
        prefs: Prefs,
        profiles: List<Profile>,
        url: String
    ): Map<String, UrlResult> = withContext(Dispatchers.IO) {
        val results = LinkedHashMap<String, UrlResult>()
        val runnable = mutableListOf<Profile>()
        for (profile in profiles) {
            val reason = unsupportedReason(profile)
            if (reason != null) results[profile.id] = UrlResult(null, reason) else runnable += profile
        }
        if (runnable.isNotEmpty()) results.putAll(testRun(context, prefs, runnable, url))
        results
    }

    /**
     * One probe core for these profiles. When that core will not start (usually one profile's config
     * is bad), the profiles are tested one at a time, so only the bad profile reports an error.
     */
    private suspend fun testRun(
        context: Context,
        prefs: Prefs,
        group: List<Profile>,
        url: String
    ): Map<String, UrlResult> {
        val ports = group.map { freePort() }
        val status = File(context.cacheDir, ProbeService.STATUS_FILE)
        status.delete()
        ProbeService.start(context, buildProbeConfig(prefs, group.zip(ports)))
        val coreError = awaitStart(status)
        if (coreError != null) {
            ProbeService.stop(context)
            delay(STOP_GAP_MS)
            if (group.size == 1) return mapOf(group[0].id to UrlResult(null, coreError))
            val out = LinkedHashMap<String, UrlResult>()
            for (profile in group) out.putAll(testRun(context, prefs, listOf(profile), url))
            return out
        }
        try {
            val gate = Semaphore(PARALLEL)
            return coroutineScope {
                group.zip(ports).map { (profile, port) ->
                    async { profile.id to gate.withPermit { fetchVia(port, url) } }
                }.awaitAll()
            }.toMap()
        } finally {
            ProbeService.stop(context)
            delay(STOP_GAP_MS)
        }
    }

    /** Waits for the probe core to report. Returns null when it started, or the reason it did not. */
    private suspend fun awaitStart(status: File): String? {
        val deadline = System.currentTimeMillis() + CORE_START_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (status.exists()) {
                val text = status.readText().trim()
                return if (text == "ok") null else text.removePrefix("error: ").ifBlank { "core failed to start" }
            }
            delay(150)
        }
        return "core did not start in time"
    }

    private fun fetchVia(port: Int, url: String): UrlResult {
        return try {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", port))
            val connection = URL(url).openConnection(proxy) as HttpURLConnection
            try {
                connection.connectTimeout = REQUEST_TIMEOUT_MS
                connection.readTimeout = REQUEST_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                val start = System.nanoTime()
                val code = connection.responseCode
                val ms = (System.nanoTime() - start) / 1_000_000
                if (code in 200..399) UrlResult(ms, null) else UrlResult(null, "HTTP $code")
            } finally {
                connection.disconnect()
            }
        } catch (e: java.net.SocketTimeoutException) {
            UrlResult(null, "timeout")
        } catch (e: Exception) {
            UrlResult(null, e.message ?: e.javaClass.simpleName)
        }
    }

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }
}
