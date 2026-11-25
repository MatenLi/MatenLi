package dev.rffrr.mateli.profiles

import org.json.JSONObject
import java.net.URLEncoder
import java.util.Base64

/**
 * Writes the Standard share link for a profile. It is the inverse of [ShareLinkParser], so a link
 * written here imports back to the same profile. Returns null for types that have no link format yet.
 */
object ShareLinkWriter {

    fun standard(profile: Profile): String? {
        val o = profile.outbound
        val host = o.optString("server")
        val port = o.optInt("server_port")
        if (host.isEmpty() || port !in 1..65535) return null
        val hostPort = if (host.contains(':')) "[$host]:$port" else "$host:$port"
        val name = enc(profile.name)
        return when (o.optString("type")) {
            "trojan" -> "trojan://${enc(o.optString("password"))}@$hostPort/?${join(transportAndTls(o))}#$name"
            "vless" -> {
                val params = LinkedHashMap<String, String>()
                params["encryption"] = "none"
                o.optString("flow").takeIf { it.isNotEmpty() }?.let { params["flow"] = it }
                params.putAll(transportAndTls(o))
                "vless://${enc(o.optString("uuid"))}@$hostPort/?${join(params)}#$name"
            }
            "vmess" -> {
                val tr = o.optJSONObject("transport")
                val tls = o.optJSONObject("tls")
                val json = JSONObject()
                    .put("v", "2")
                    .put("ps", profile.name)
                    .put("add", host)
                    .put("port", port.toString())
                    .put("id", o.optString("uuid"))
                    .put("aid", o.optInt("alter_id", 0).toString())
                    .put("scy", o.optString("security", "auto"))
                    .put("net", tr?.optString("type").orEmpty().ifEmpty { "tcp" })
                    .put("type", "none")
                    .put("host", transportHost(tr))
                    .put("path", tr?.optString("path").orEmpty())
                    .put("tls", if (tls != null && tls.optBoolean("enabled")) "tls" else "")
                    .put("sni", tls?.optString("server_name").orEmpty())
                    .put("alpn", alpn(tls))
                    .put("fp", tls?.optJSONObject("utls")?.optString("fingerprint").orEmpty())
                "vmess://" + Base64.getEncoder().encodeToString(json.toString().toByteArray(Charsets.UTF_8))
            }
            "shadowsocks" -> {
                val credentials = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("${o.optString("method")}:${o.optString("password")}".toByteArray(Charsets.UTF_8))
                val params = LinkedHashMap<String, String>()
                o.optString("plugin").takeIf { it.isNotEmpty() }?.let { plugin ->
                    val opts = o.optString("plugin_opts")
                    params["plugin"] = if (opts.isEmpty()) plugin else "$plugin;$opts"
                }
                val query = if (params.isEmpty()) "" else "/?${join(params)}"
                "ss://$credentials@$hostPort$query#$name"
            }
            "hysteria2" -> {
                val params = LinkedHashMap<String, String>()
                val tls = o.optJSONObject("tls")
                if (tls != null) {
                    tls.optString("server_name").takeIf { it.isNotEmpty() }?.let { params["sni"] = it }
                    if (tls.optBoolean("insecure")) params["insecure"] = "1"
                }
                o.optJSONObject("obfs")?.let { obfs ->
                    params["obfs"] = obfs.optString("type")
                    params["obfs-password"] = obfs.optString("password")
                }
                "hysteria2://${enc(o.optString("password"))}@$hostPort/?${join(params)}#$name"
            }
            "tuic" -> {
                val tls = o.optJSONObject("tls")
                val params = LinkedHashMap<String, String>()
                params["congestion_control"] = o.optString("congestion_control", "bbr")
                params["udp_relay_mode"] = o.optString("udp_relay_mode", "native")
                alpn(tls).takeIf { it.isNotEmpty() }?.let { params["alpn"] = it }
                if (tls != null) {
                    tls.optString("server_name").takeIf { it.isNotEmpty() }?.let { params["sni"] = it }
                    if (tls.optBoolean("insecure")) params["allowInsecure"] = "1"
                }
                "tuic://${enc(o.optString("uuid"))}:${enc(o.optString("password"))}@$hostPort/?${join(params)}#$name"
            }
            "socks" -> "socks://${auth(o)}$hostPort#$name"
            "http" -> {
                val scheme = if (o.optJSONObject("tls") != null) "https" else "http"
                "$scheme://${auth(o)}$hostPort#$name"
            }
            else -> null
        }
    }

    /** TLS and transport parameters, in the order the Standard links use: type, security, sni, alpn, fp. */
    private fun transportAndTls(o: JSONObject): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        val tr = o.optJSONObject("transport")
        val type = tr?.optString("type").orEmpty().ifEmpty { "tcp" }
        map["type"] = type
        val tls = o.optJSONObject("tls")
        if (tls != null && tls.optBoolean("enabled")) {
            val reality = tls.optJSONObject("reality")?.optBoolean("enabled") == true
            map["security"] = if (reality) "reality" else "tls"
            tls.optString("server_name").takeIf { it.isNotEmpty() }?.let { map["sni"] = it }
            alpn(tls).takeIf { it.isNotEmpty() }?.let { map["alpn"] = it }
            tls.optJSONObject("utls")?.optString("fingerprint")?.takeIf { it.isNotEmpty() }?.let { map["fp"] = it }
            if (tls.optBoolean("insecure")) map["allowInsecure"] = "1"
            tls.optJSONObject("reality")?.let { r ->
                map["pbk"] = r.optString("public_key")
                map["sid"] = r.optString("short_id")
            }
        }
        when (type) {
            "ws", "http", "httpupgrade" -> {
                map["path"] = tr?.optString("path").orEmpty().ifEmpty { "/" }
                transportHost(tr).takeIf { it.isNotEmpty() }?.let { map["host"] = it }
            }
            "grpc" -> map["serviceName"] = tr?.optString("service_name").orEmpty()
        }
        return map
    }

    private fun transportHost(tr: JSONObject?): String {
        if (tr == null) return ""
        tr.optJSONObject("headers")?.optString("Host")?.takeIf { it.isNotEmpty() }?.let { return it }
        tr.optJSONArray("host")?.let { arr -> if (arr.length() > 0) return arr.getString(0) }
        return tr.optString("host")
    }

    private fun alpn(tls: JSONObject?): String {
        val arr = tls?.optJSONArray("alpn") ?: return ""
        return (0 until arr.length()).joinToString(",") { arr.getString(it) }
    }

    private fun auth(o: JSONObject): String {
        val user = o.optString("username")
        if (user.isEmpty()) return ""
        return "${enc(user)}:${enc(o.optString("password"))}@"
    }

    private fun join(map: Map<String, String>): String =
        map.entries.joinToString("&") { "${it.key}=${enc(it.value)}" }

    /** Percent-encodes with %20 for spaces, as the Standard links do. */
    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
