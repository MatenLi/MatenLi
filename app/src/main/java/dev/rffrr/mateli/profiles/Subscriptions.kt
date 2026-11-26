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
    val userAgent: String = "MatenLi/0.1",
    val mirror: MirrorConfig = MirrorConfig()
)

/** A new subscription with an id and no settings yet, for the add dialog. */
fun blankSubscription(): Subscription = Subscription(UUID.randomUUID().toString(), "", "", emptyList(), 0L)

/** Mirror settings of one subscription. A new subscription starts from the global defaults in Settings. */
data class MirrorConfig(
    val enabled: Boolean = true,
    val base: String = Mirror.DEFAULT_BASE,
    val client: String = Mirror.DEFAULT_CLIENT,
    val os: String = Mirror.DEFAULT_OS,
    val hwid: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("enabled", enabled)
        .put("base", base)
        .put("client", client)
        .put("os", os)
        .put("hwid", hwid)

    companion object {
        fun fromJson(o: JSONObject?): MirrorConfig {
            if (o == null) return MirrorConfig()
            return MirrorConfig(
                enabled = o.optBoolean("enabled", true),
                base = o.optString("base", Mirror.DEFAULT_BASE),
                client = o.optString("client", Mirror.DEFAULT_CLIENT),
                os = o.optString("os", Mirror.DEFAULT_OS),
                hwid = o.optString("hwid", "")
            )
        }
    }
}

/**
 * Subscription mirror. Downloads go through the mirror first and fall back to the original URL.
 * Link form, always base64: base/ua=<client>/os=<system>/h=<16 hex>/b64/<upstream>
 */
object Mirror {
    const val DEFAULT_BASE = "https://p.kfwl.lol"
    const val DEFAULT_CLIENT = "happ"
    const val DEFAULT_OS = "android"

    /** Clients and systems the mirror accepts. Only these ids are ever sent. */
    val CLIENTS = listOf(
        "happ" to "Happ",
        "v2rayng" to "v2rayNG",
        "hiddify" to "Hiddify",
        "singbox" to "sing-box",
        "clashmeta" to "Clash Meta",
        "nekobox" to "NekoBox"
    )
    val SYSTEMS = listOf(
        "android" to "Android",
        "ios" to "iOS",
        "windows" to "Windows",
        "macos" to "macOS",
        "linux" to "Linux"
    )

    private val HWID = Regex("^[0-9a-f]{16}$")
    private val upstreamPrefixes = listOf("http://", "https://", "happ://", "incy://")

    fun isValidHwid(value: String): Boolean = HWID.matches(value)

    /** A random device id: 8 bytes in hex, always 16 characters. */
    fun newHwid(): String =
        ByteArray(8).also { java.security.SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }

    /** Stable device id for a subscription saved before device ids existed. */
    fun hwidFor(seed: String): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
            .take(8).joinToString("") { "%02x".format(it) }

    /** Global defaults for new subscriptions. The default device id is created once and kept. */
    fun defaults(prefs: Prefs): MirrorConfig {
        val saved = prefs.string(PrefKeys.MIRROR_HWID, "")
        val hwid = if (isValidHwid(saved)) {
            saved
        } else {
            newHwid().also { prefs.putString(PrefKeys.MIRROR_HWID, it) }
        }
        return MirrorConfig(
            enabled = prefs.bool(PrefKeys.MIRROR_ENABLED, true),
            base = prefs.string(PrefKeys.MIRROR_BASE, DEFAULT_BASE),
            client = prefs.string(PrefKeys.MIRROR_CLIENT, DEFAULT_CLIENT),
            os = prefs.string(PrefKeys.MIRROR_OS, DEFAULT_OS),
            hwid = hwid
        )
    }

    /** Mirror link for one upstream, or null when the mirror is off or cannot be used for it. */
    fun url(config: MirrorConfig, subscriptionId: String, upstream: String): String? {
        if (!config.enabled) return null
        if (upstreamPrefixes.none { upstream.startsWith(it) }) return null
        val base = config.base.trim().trimEnd('/')
        if (base.isBlank()) return null
        val hwid = if (isValidHwid(config.hwid)) config.hwid else hwidFor(subscriptionId)
        return listOf(base, "ua=" + config.client, "os=" + config.os, "h=" + hwid, "b64", upstream)
            .joinToString("/")
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
                userAgent = o.optString("userAgent", "MatenLi/0.1"),
                mirror = MirrorConfig.fromJson(o.optJSONObject("mirror"))
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
                    .put("mirror", s.mirror.toJson())
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
    fun refresh(profiles: ProfileStore, subscriptions: SubscriptionStore, sub: Subscription): RefreshResult {
        val body = fetchBody(sub)
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
    private fun fetchBody(sub: Subscription): String {
        val mirrored = Mirror.url(sub.mirror, sub.id, sub.url)
        if (mirrored != null) {
            val body = runCatching { fetch(mirrored, sub.userAgent) }.getOrNull()
            if (body != null && hasServers(body)) return body
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
