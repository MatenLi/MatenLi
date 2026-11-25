package dev.rffrr.mateli.profiles

import android.content.Context
import dev.rffrr.mateli.settings.PrefKeys
import dev.rffrr.mateli.settings.Prefs
import dev.rffrr.mateli.storage.DataDir
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.UUID

/**
 * A subscription URL and its group settings. [profileIds] are the servers it last added,
 * so a refresh replaces only them. [filter] is a regex on profile names (empty keeps all).
 */
data class Subscription(
    val id: String,
    val name: String,
    val url: String,
    val profileIds: List<String>,
    val updatedAt: Long,
    val autoUpdate: Boolean = false,
    val updateMinutes: Int = 1440,
    val filter: String = "",
    val dedupe: Boolean = false,
    val userAgent: String = "MatenLi/0.1"
)

/** A new subscription with an id and no settings yet, for the add dialog. */
fun blankSubscription(): Subscription = Subscription(UUID.randomUUID().toString(), "", "", emptyList(), 0L)

/**
 * Subscription mirror. On by default: downloads go through the mirror first and fall back to the
 * original URL when the mirror fails or has no servers. Link form:
 * base/ua=../os=../sua="../h=<hwid>/<fmt>/<upstream>
 */
object Mirror {
    const val DEFAULT_BASE = "https://p.kfwl.lol"
    const val DEFAULT_UA = "happ"
    const val DEFAULT_OS = "android"
    const val DEFAULT_SUA = "useragent/1.0"
    const val DEFAULT_FMT = "b64"
    private val upstreamPrefixes = listOf("http://", "https://", "happ://", "incy://")

    fun enabled(prefs: Prefs): Boolean = prefs.bool(PrefKeys.MIRROR_ENABLED, true)

    /** Device id sent as h=: 8 random bytes in hex, created once and kept in settings. */
    fun hwid(prefs: Prefs): String {
        val saved = prefs.string(PrefKeys.MIRROR_HWID, "")
        if (saved.length == 16) return saved
        val bytes = ByteArray(8).also { java.security.SecureRandom().nextBytes(it) }
        val created = bytes.joinToString("") { "%02x".format(it) }
        prefs.putString(PrefKeys.MIRROR_HWID, created)
        return created
    }

    /** Mirror link for one upstream URL, or null when the mirror cannot be used for it. */
    fun url(prefs: Prefs, upstream: String): String? {
        if (upstreamPrefixes.none { upstream.startsWith(it) }) return null
        val base = prefs.string(PrefKeys.MIRROR_BASE, DEFAULT_BASE).trim().trimEnd('/')
        if (base.isBlank()) return null
        val parts = mutableListOf(
            "ua=" + prefs.string(PrefKeys.MIRROR_UA, DEFAULT_UA),
            "os=" + prefs.string(PrefKeys.MIRROR_OS, DEFAULT_OS)
        )
        val sua = prefs.string(PrefKeys.MIRROR_SUA, DEFAULT_SUA)
        if (sua.isNotBlank()) parts += "sua=\"" + sua + "\""
        parts += "h=" + hwid(prefs)
        val fmt = prefs.string(PrefKeys.MIRROR_FMT, DEFAULT_FMT)
        if (fmt.isNotBlank()) parts += fmt
        parts += upstream
        return base + "/" + parts.joinToString("/")
    }
}

/** What a refresh changed, by profile name. */
data class RefreshResult(
    val subscription: Subscription,
    val added: List<String>,
    val updated: List<String>,
    val deleted: List<String>
)

/** Subscriptions live in a JSON file in app storage. */
class SubscriptionStore(context: Context) {
    private val app = context.applicationContext
    private val file: File get() = DataDir.file(app, "subscriptions.json")

    fun list(): List<Subscription> = try {
        val arr = JSONArray(file.readText())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ids = o.getJSONArray("profileIds")
            Subscription(
                id = o.getString("id"),
                name = o.getString("name"),
                url = o.getString("url"),
                profileIds = (0 until ids.length()).map { ids.getString(it) },
                updatedAt = o.optLong("updatedAt", 0L),
                autoUpdate = o.optBoolean("autoUpdate", false),
                updateMinutes = o.optInt("updateMinutes", 1440),
                filter = o.optString("filter", ""),
                dedupe = o.optBoolean("dedupe", false),
                userAgent = o.optString("userAgent", "MatenLi/0.1")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun save(subscriptions: List<Subscription>) {
        val arr = JSONArray()
        subscriptions.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("url", s.url)
                    .put("profileIds", JSONArray(s.profileIds))
                    .put("updatedAt", s.updatedAt)
                    .put("autoUpdate", s.autoUpdate)
                    .put("updateMinutes", s.updateMinutes)
                    .put("filter", s.filter)
                    .put("dedupe", s.dedupe)
                    .put("userAgent", s.userAgent)
            )
        }
        file.writeText(arr.toString())
    }

    fun add(name: String, url: String): Subscription {
        val created = Subscription(UUID.randomUUID().toString(), name, url, emptyList(), 0L)
        save(list() + created)
        return created
    }

    fun create(sub: Subscription) {
        save(list() + sub)
    }

    fun update(sub: Subscription) {
        save(list().map { if (it.id == sub.id) sub else it })
    }

    fun remove(id: String, profiles: ProfileStore) {
        list().firstOrNull { it.id == id }?.profileIds?.forEach { profiles.remove(it) }
        save(list().filterNot { it.id == id })
    }
}

/** Downloads a subscription and replaces its servers. Call from a background thread. */
object SubscriptionUpdater {

    /** Throws with a readable message when the download, filter or parse fails. */
    fun refresh(
        profiles: ProfileStore,
        subscriptions: SubscriptionStore,
        sub: Subscription,
        prefs: Prefs
    ): RefreshResult {
        val body = fetchBody(prefs, sub)
        var parsed = decodeLines(body).mapNotNull { line ->
            runCatching { ShareLinkParser.parse(line) }.getOrNull()
        }
        if (sub.filter.isNotBlank()) {
            val regex = runCatching { Regex(sub.filter) }.getOrElse {
                throw IllegalArgumentException("filter is not a valid regex")
            }
            parsed = parsed.filter { regex.containsMatchIn(it.name) }
        }
        if (sub.dedupe) {
            val seen = HashSet<String>()
            parsed = parsed.filter { seen.add(it.outbound.toString()) }
        }
        require(parsed.isNotEmpty()) { "no usable servers in the subscription" }

        val old = profiles.list().filter { it.id in sub.profileIds }
        val oldByName = old.associateBy { it.name }
        val newNames = parsed.map { it.name }.toSet()
        val added = parsed.filter { it.name !in oldByName }.map { it.name }
        val updated = parsed.filter { p ->
            val before = oldByName[p.name]
            before != null && before.outbound.toString() != p.outbound.toString()
        }.map { it.name }
        val deleted = old.filter { it.name !in newNames }.map { it.name }

        sub.profileIds.forEach { profiles.remove(it) }
        val ids = parsed.map { profile ->
            profiles.add(profile)
            profile.id
        }
        val updatedSub = sub.copy(profileIds = ids, updatedAt = System.currentTimeMillis())
        subscriptions.update(updatedSub)
        return RefreshResult(updatedSub, added, updated, deleted)
    }

    /** Mirror first when it is on. Any failure, or a mirror with no servers, falls back to the original URL. */
    private fun fetchBody(prefs: Prefs, sub: Subscription): String {
        if (Mirror.enabled(prefs)) {
            val mirrored = Mirror.url(prefs, sub.url)
            if (mirrored != null) {
                val body = runCatching { fetch(mirrored, sub.userAgent) }.getOrNull()
                if (body != null && hasServers(body)) return body
            }
        }
        return fetch(sub.url, sub.userAgent)
    }

    private fun hasServers(body: String): Boolean = runCatching {
        decodeLines(body).any { line -> runCatching { ShareLinkParser.parse(line) }.isSuccess }
    }.getOrDefault(false)

    private fun fetch(url: String, userAgent: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", userAgent)
            val code = connection.responseCode
            require(code in 200..299) { "server answered $code" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** Subscriptions are either plain lines or one base64 block of lines. */
    private fun decodeLines(body: String): List<String> {
        val trimmed = body.trim()
        val text = if (trimmed.contains("://") || trimmed.startsWith("{")) {
            trimmed
        } else {
            val clean = trimmed.replace("\n", "").replace('-', '+').replace('_', '/')
            val padded = clean + "=".repeat((4 - clean.length % 4) % 4)
            String(Base64.getDecoder().decode(padded), Charsets.UTF_8)
        }
        return text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }
}
