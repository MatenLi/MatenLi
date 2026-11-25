package dev.rffrr.mateli.profiles

import org.json.JSONObject
import java.util.UUID

/** Protocols offered by Manual Settings. */
val manualProtocols: List<String> = listOf(
    "socks", "http", "shadowsocks", "vmess", "vless", "trojan", "hysteria2", "tuic"
)

enum class FieldKind { Text, Number, Secret, Bool, Choice }

/** One editable field. Keys starting with "tls." live in the tls block. */
data class EditField(
    val label: String,
    val key: String,
    val section: String,
    val kind: FieldKind,
    val choices: List<String> = emptyList()
)

private val TLS_TYPES = setOf("vmess", "vless", "trojan", "hysteria2", "tuic", "http")

/** A new profile with the defaults for [type]. */
fun blankProfile(type: String): Profile {
    val out = JSONObject()
        .put("type", type)
        .put("tag", "proxy")
        .put("server", "")
        .put("server_port", 443)
    when (type) {
        "socks" -> out.put("version", "5")
        "shadowsocks" -> out.put("method", "aes-256-gcm").put("password", "")
        "vmess" -> out.put("uuid", UUID.randomUUID().toString()).put("security", "auto").put("alter_id", 0)
        "vless" -> out.put("uuid", "")
        "trojan", "hysteria2" -> out.put("password", "")
        "tuic" -> out.put("uuid", "").put("password", "").put("congestion_control", "bbr")
    }
    if (type == "trojan" || type == "hysteria2" || type == "tuic") {
        out.put("tls", JSONObject().put("enabled", true).put("server_name", ""))
    }
    return Profile(UUID.randomUUID().toString(), "New $type", type, out)
}

/** Fields shown in the editor for a protocol type. */
fun fieldsFor(type: String): List<EditField> = buildList {
    add(EditField("Server", "server", "Server", FieldKind.Text))
    add(EditField("Port", "server_port", "Server", FieldKind.Number))
    when (type) {
        "socks" -> {
            add(EditField("Version", "version", "Server", FieldKind.Choice, listOf("5", "4a", "4")))
            add(EditField("Username (optional)", "username", "Server", FieldKind.Text))
            add(EditField("Password (optional)", "password", "Server", FieldKind.Secret))
        }
        "http" -> {
            add(EditField("Username (optional)", "username", "Server", FieldKind.Text))
            add(EditField("Password (optional)", "password", "Server", FieldKind.Secret))
        }
        "shadowsocks" -> {
            add(EditField("Method", "method", "Server", FieldKind.Choice, listOf(
                "aes-128-gcm", "aes-256-gcm", "chacha20-ietf-poly1305",
                "2022-blake3-aes-128-gcm", "2022-blake3-aes-256-gcm", "2022-blake3-chacha20-poly1305", "none"
            )))
            add(EditField("Password", "password", "Server", FieldKind.Secret))
        }
        "vmess" -> {
            add(EditField("UUID", "uuid", "Server", FieldKind.Secret))
            add(EditField("Security", "security", "Server", FieldKind.Choice, listOf("auto", "aes-128-gcm", "chacha20-poly1305", "none", "zero")))
            add(EditField("Alter ID", "alter_id", "Server", FieldKind.Number))
        }
        "vless" -> {
            add(EditField("UUID", "uuid", "Server", FieldKind.Secret))
            add(EditField("Flow", "flow", "Server", FieldKind.Choice, listOf("", "xtls-rprx-vision")))
        }
        "trojan", "hysteria2" -> add(EditField("Password", "password", "Server", FieldKind.Secret))
        "tuic" -> {
            add(EditField("UUID", "uuid", "Server", FieldKind.Secret))
            add(EditField("Password", "password", "Server", FieldKind.Secret))
            add(EditField("Congestion control", "congestion_control", "Server", FieldKind.Choice, listOf("bbr", "cubic", "new_reno")))
        }
    }
    if (type in TLS_TYPES) {
        add(EditField("Server name (SNI)", "tls.server_name", "TLS", FieldKind.Text))
        add(EditField("Allow insecure", "tls.insecure", "TLS", FieldKind.Bool))
    }
}

/** Reads a field as text for the editor. */
fun readField(profile: Profile, field: EditField): String {
    val isTls = field.key.startsWith("tls.")
    val source = if (isTls) profile.outbound.optJSONObject("tls") else profile.outbound
    val key = field.key.removePrefix("tls.")
    if (source == null) return if (field.kind == FieldKind.Bool) "false" else ""
    if (field.kind == FieldKind.Bool) return source.optBoolean(key, false).toString()
    return source.opt(key)?.takeIf { it != JSONObject.NULL }?.toString().orEmpty()
}

/**
 * Applies the editor values to a copy of the profile. Throws IllegalArgumentException
 * with a message the user can read when a value is invalid.
 */
fun applyEdits(profile: Profile, name: String, values: Map<String, String>): Profile {
    val out = JSONObject(profile.outbound.toString())
    for (field in fieldsFor(profile.type)) {
        val raw = values[field.key] ?: continue
        val key = field.key.removePrefix("tls.")
        val target = if (field.key.startsWith("tls.")) {
            out.optJSONObject("tls") ?: JSONObject().put("enabled", true).also { out.put("tls", it) }
        } else {
            out
        }
        when (field.kind) {
            FieldKind.Bool -> target.put(key, raw == "true")
            FieldKind.Number -> {
                val number = raw.trim().toIntOrNull()
                    ?: throw IllegalArgumentException("${field.label} must be a number")
                target.put(key, number)
            }
            else -> if (raw.isEmpty()) target.remove(key) else target.put(key, raw.trim())
        }
    }
    val server = out.optString("server")
    require(server.isNotBlank()) { "Server is required" }
    val port = out.optInt("server_port")
    require(port in 1..65535) { "Port must be between 1 and 65535" }
    if (profile.type == "vless" || profile.type == "vmess" || profile.type == "tuic") {
        require(out.optString("uuid").isNotBlank()) { "UUID is required" }
    }
    return profile.copy(
        name = name.trim().ifEmpty { "${profile.type} $server" },
        outbound = out
    )
}
