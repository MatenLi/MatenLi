package dev.rffrr.mateli.profiles

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.util.Base64
import java.util.UUID

/**
 * Turns one line of input into a profile.
 * Share links: vless, vmess, trojan, shadowsocks, hysteria2, tuic, socks, http.
 * Anything else (WireGuard, SSH, ...): paste the sing-box outbound JSON.
 * Throws IllegalArgumentException with a readable message for bad input.
 */
object ShareLinkParser {

    private val NON_PROXY_TYPES = setOf("direct", "block", "dns", "selector", "urltest")

    fun parse(input: String): Profile {
        val text = input.trim()
        require(text.isNotEmpty()) { "Empty line" }
        return if (text.startsWith("{")) fromJson(text) else fromLink(text)
    }

    private fun fromJson(text: String): Profile {
        val root = JSONObject(text)
        val outbound = if (root.has("outbounds")) {
            val list = root.getJSONArray("outbounds")
            (0 until list.length())
                .map { list.getJSONObject(it) }
                .firstOrNull { it.optString("type") !in NON_PROXY_TYPES }
                ?: throw IllegalArgumentException("No proxy outbound in this config")
        } else {
            root
        }
        val type = outbound.optString("type")
        require(type.isNotEmpty() && type !in NON_PROXY_TYPES) { "JSON has no proxy outbound type" }
        val server = outbound.optString("server")
        val name = outbound.optString("tag").takeIf { it.isNotEmpty() && it != "proxy" }
            ?: listOf(type, server).filter { it.isNotEmpty() }.joinToString(" ")
        val copy = JSONObject(outbound.toString()).put("tag", "proxy")
        return Profile(UUID.randomUUID().toString(), name, type, copy)
    }

    private fun fromLink(text: String): Profile {
        require(text.contains("://")) { "Not a share link or outbound JSON" }
        val scheme = text.substringBefore("://").lowercase()
        val rest = text.substringAfter("://")
        val label = decode(rest.substringAfter('#', ""))
        val body = rest.substringBefore('#')
        return when (scheme) {
            "vmess" -> vmess(body, label)
            "vless" -> vless(body, label)
            "trojan" -> trojan(body, label)
            "ss" -> shadowsocks(body, label)
            "hysteria2", "hy2" -> hysteria2(body, label)
            "tuic" -> tuic(body, label)
            "socks", "socks5", "socks5h" -> socks(body, label)
            "http", "https" -> http(scheme == "https", body, label)
            else -> throw IllegalArgumentException("Unsupported link type: $scheme")
        }
    }

    private fun vless(body: String, label: String): Profile {
        val e = endpoint(body)
        require(e.userinfo.isNotEmpty()) { "vless link has no UUID" }
        val out = base("vless", e.host, e.port).put("uuid", e.userinfo)
        e.query.value("flow")?.let { out.put("flow", it) }
        attach(out, e.query, e.host, forceTls = false)
        return profile(label, "vless", e.host, out)
    }

    private fun trojan(body: String, label: String): Profile {
        val e = endpoint(body)
        require(e.userinfo.isNotEmpty()) { "trojan link has no password" }
        val out = base("trojan", e.host, e.port).put("password", e.userinfo)
        attach(out, e.query, e.host, forceTls = true)
        return profile(label, "trojan", e.host, out)
    }

    private fun vmess(body: String, label: String): Profile {
        val json = JSONObject(base64(body))
        val host = json.optString("add")
        require(host.isNotEmpty()) { "vmess link has no server address" }
        val port = json.optString("port").toIntOrNull()
            ?: throw IllegalArgumentException("vmess link has a bad port")
        val q = mapOf(
            "type" to json.optString("net", "tcp"),
            "host" to json.optString("host"),
            "path" to json.optString("path"),
            "security" to json.optString("tls"),
            "sni" to json.optString("sni"),
            "alpn" to json.optString("alpn"),
            "fp" to json.optString("fp")
        )
        val out = base("vmess", host, port)
            .put("uuid", json.optString("id"))
            .put("security", json.optString("scy").ifEmpty { "auto" })
            .put("alter_id", json.optString("aid", "0").toIntOrNull() ?: 0)
        attach(out, q, host, forceTls = false)
        return profile(label.ifEmpty { json.optString("ps") }, "vmess", host, out)
    }

    private fun shadowsocks(body: String, label: String): Profile {
        val addr = body.substringBefore('?')
        val query = parseQuery(body.substringAfter('?', ""))
        val credentials: String
        val hostPart: String
        if (addr.contains('@')) {
            val info = addr.substringBeforeLast('@')
            credentials = if (info.contains(':')) decode(info) else base64(info)
            hostPart = addr.substringAfterLast('@')
        } else {
            val decoded = base64(addr)
            credentials = decoded.substringBeforeLast('@')
            hostPart = decoded.substringAfterLast('@')
        }
        val method = credentials.substringBefore(':')
        val password = credentials.substringAfter(':', "")
        require(method.isNotEmpty() && password.isNotEmpty()) { "Shadowsocks link has no method or password" }
        val (host, port) = hostPort(hostPart)
        val out = base("shadowsocks", host, port)
            .put("method", method)
            .put("password", password)
        query.value("plugin")?.let { plugin ->
            out.put("plugin", plugin.substringBefore(';'))
            if (plugin.contains(';')) out.put("plugin_opts", plugin.substringAfter(';'))
        }
        return profile(label, "shadowsocks", host, out)
    }

    private fun hysteria2(body: String, label: String): Profile {
        val e = endpoint(body)
        require(e.userinfo.isNotEmpty()) { "hysteria2 link has no password" }
        val out = base("hysteria2", e.host, e.port).put("password", e.userinfo)
        e.query.value("obfs")?.let { kind ->
            out.put(
                "obfs",
                JSONObject()
                    .put("type", kind)
                    .put("password", e.query.value("obfs-password").orEmpty())
            )
        }
        out.put("tls", tlsBlock(e.query, e.host))
        return profile(label, "hysteria2", e.host, out)
    }

    private fun tuic(body: String, label: String): Profile {
        val e = endpoint(body)
        val uuid = e.userinfo.substringBefore(':')
        val password = e.userinfo.substringAfter(':', "")
        require(uuid.isNotEmpty()) { "tuic link has no UUID" }
        val out = base("tuic", e.host, e.port)
            .put("uuid", uuid)
            .put("password", password)
            .put("congestion_control", e.query.value("congestion_control") ?: "bbr")
            .put("udp_relay_mode", e.query.value("udp_relay_mode") ?: "native")
        val tls = tlsBlock(e.query, e.host)
        tls.put("alpn", JSONArray((e.query.value("alpn") ?: "h3").split(',')))
        out.put("tls", tls)
        return profile(label, "tuic", e.host, out)
    }

    private fun socks(body: String, label: String): Profile {
        val e = endpoint(body)
        val out = base("socks", e.host, e.port).put("version", "5")
        if (e.userinfo.isNotEmpty()) {
            out.put("username", e.userinfo.substringBefore(':'))
            out.put("password", e.userinfo.substringAfter(':', ""))
        }
        return profile(label, "socks", e.host, out)
    }

    private fun http(secure: Boolean, body: String, label: String): Profile {
        val e = endpoint(body)
        val out = base("http", e.host, e.port)
        if (e.userinfo.isNotEmpty()) {
            out.put("username", e.userinfo.substringBefore(':'))
            out.put("password", e.userinfo.substringAfter(':', ""))
        }
        if (secure) out.put("tls", tlsBlock(e.query, e.host))
        return profile(label, "http", e.host, out)
    }

    // ---- shared helpers ----

    private data class Endpoint(
        val userinfo: String,
        val host: String,
        val port: Int,
        val query: Map<String, String>
    )

    private fun endpoint(body: String): Endpoint {
        val addr = body.substringBefore('?')
        val hasAuth = addr.contains('@')
        val (host, port) = hostPort(addr.substringAfterLast('@'))
        val userinfo = if (hasAuth) decode(addr.substringBeforeLast('@')) else ""
        return Endpoint(userinfo, host, port, parseQuery(body.substringAfter('?', "")))
    }

    private fun hostPort(raw: String): Pair<String, Int> {
        val hp = raw.trimEnd('/')
        val host = hp.substringBeforeLast(':').removePrefix("[").removeSuffix("]")
        val port = hp.substringAfterLast(':').toIntOrNull()
        require(host.isNotEmpty() && port != null && port in 1..65535) { "Bad server address: $hp" }
        return host to port
    }

    private fun base(type: String, host: String, port: Int): JSONObject =
        JSONObject()
            .put("type", type)
            .put("tag", "proxy")
            .put("server", host)
            .put("server_port", port)

    private fun profile(name: String, type: String, host: String, out: JSONObject): Profile =
        Profile(UUID.randomUUID().toString(), name.ifEmpty { "$type $host" }, type, out)

    private fun attach(out: JSONObject, q: Map<String, String>, host: String, forceTls: Boolean) {
        if (forceTls || q.value("security") == "tls" || q.value("security") == "reality") {
            out.put("tls", tlsBlock(q, host, reality = q.value("security") == "reality"))
        }
        transport(q, host)?.let { out.put("transport", it) }
    }

    private fun tlsBlock(q: Map<String, String>, host: String, reality: Boolean = false): JSONObject {
        val tls = JSONObject()
            .put("enabled", true)
            .put("server_name", q.value("sni", "peer") ?: host)
            .put("insecure", insecureFlag(q))
        val fingerprint = q.value("fp") ?: if (reality) "chrome" else null
        if (fingerprint != null) {
            tls.put("utls", JSONObject().put("enabled", true).put("fingerprint", fingerprint))
        }
        if (reality) {
            tls.put(
                "reality",
                JSONObject()
                    .put("enabled", true)
                    .put("public_key", q.value("pbk").orEmpty())
                    .put("short_id", q.value("sid").orEmpty())
            )
        }
        return tls
    }

    private fun transport(q: Map<String, String>, host: String): JSONObject? {
        val hostHeader = q.value("host") ?: host
        val path = q.value("path") ?: "/"
        return when (q.value("type") ?: "tcp") {
            "ws" -> JSONObject()
                .put("type", "ws")
                .put("path", path)
                .put("headers", JSONObject().put("Host", hostHeader))
            "grpc" -> JSONObject()
                .put("type", "grpc")
                .put("service_name", q.value("serviceName") ?: q.value("path").orEmpty())
            "h2", "http" -> JSONObject()
                .put("type", "http")
                .put("host", JSONArray().put(hostHeader))
                .put("path", path)
            "xhttp", "splithttp" -> JSONObject().put("type", "xhttp")
            "httpupgrade" -> JSONObject()
                .put("type", "httpupgrade")
                .put("host", hostHeader)
                .put("path", path)
            else -> null
        }
    }

    private fun insecureFlag(q: Map<String, String>): Boolean {
        val v = q.value("allowInsecure", "insecure", "skip-cert-verify") ?: return false
        return v == "1" || v == "true"
    }

    private fun Map<String, String>.value(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> this[key]?.takeIf { it.isNotEmpty() } }

    private fun parseQuery(q: String): Map<String, String> =
        q.split('&')
            .filter { it.contains('=') }
            .associate { part -> decode(part.substringBefore('=')) to decode(part.substringAfter('=')) }

    private fun decode(s: String): String = URLDecoder.decode(s, "UTF-8")

    private fun base64(s: String): String {
        val clean = s.trim().replace('-', '+').replace('_', '/')
        val padded = clean + "=".repeat((4 - clean.length % 4) % 4)
        return String(Base64.getDecoder().decode(padded), Charsets.UTF_8)
    }
}
