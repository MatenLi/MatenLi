package dev.rffrr.mateli.config

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import dev.rffrr.mateli.R
import dev.rffrr.mateli.profiles.Profile
import dev.rffrr.mateli.settings.PrefKeys
import dev.rffrr.mateli.settings.Prefs
import org.json.JSONArray
import org.json.JSONObject

/** Returns the reason a profile cannot run yet, or null when it can run. */
fun unsupportedReason(profile: Profile): String? {
    val transport = profile.outbound.optJSONObject("transport")?.optString("type")
    return if (transport == "xhttp") "xHTTP (SplitHTTP) transport" else null
}

/**
 * Builds the complete sing-box config from the settings, the selected profile and the route rules.
 * Without a profile everything goes direct.
 */
fun buildSingBoxConfig(prefs: Prefs, profile: Profile?, rules: List<RouteRule>): String {
    val serviceMode = prefs.string(PrefKeys.SERVICE_MODE, "vpn")
    val mixedPort = prefs.string(PrefKeys.MIXED_PORT, "2080").toIntOrNull() ?: 2080
    val allowLan = prefs.bool(PrefKeys.ALLOW_ACCESS, false)
    val mtu = prefs.string(PrefKeys.MTU, "9000").toIntOrNull() ?: 9000
    val tunAddress4 = prefs.string(PrefKeys.TUN_ADDRESS4, "172.19.0.1/30")
    val tunAddress6 = prefs.string(PrefKeys.TUN_ADDRESS6, "fdfe:dcba:9876::1/126")
    val fakeRange4 = prefs.string(PrefKeys.FAKEDNS_RANGE4, "198.18.0.0/15")
    val fakeRange6 = prefs.string(PrefKeys.FAKEDNS_RANGE6, "fc00::/18")
    val sniffOverride = prefs.string(PrefKeys.TRAFFIC_SNIFFING, "1") == "2"
    val perAppMode = prefs.string(PrefKeys.PER_APP_MODE, "off")
    val perAppPackages = prefs.string(PrefKeys.PER_APP_PACKAGES, "")
        .split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val sniff = prefs.string(PrefKeys.TRAFFIC_SNIFFING, "1") != "0"
    val bypassLan = prefs.bool(PrefKeys.BYPASS_LAN, false)
    val fakeDns = prefs.bool(PrefKeys.ENABLE_FAKEDNS, true)
    val clashApi = prefs.bool(PrefKeys.ENABLE_CLASH_API, false)
    val remoteStrategy = strategyOf(prefs.string(PrefKeys.DOMAIN_STRATEGY_REMOTE, "auto"))
    val stack = when (prefs.string(PrefKeys.TUN_IMPLEMENTATION, "0")) {
        "1" -> "system"
        "2" -> "mixed"
        else -> "gvisor"
    }
    val logLevel = when (prefs.string(PrefKeys.LOG_LEVEL, "0")) {
        "1" -> "warn"
        "2" -> "info"
        "3" -> "debug"
        "4" -> "trace"
        else -> "fatal"
    }
    val hasProxy = profile != null
    val proxyTag = if (hasProxy) "proxy" else "direct"

    val servers = JSONArray()
        .put(dnsServer("remote", prefs.string(PrefKeys.REMOTE_DNS, "https://dns.google/dns-query"), proxyTag))
        .put(dnsServer("local", prefs.string(PrefKeys.DIRECT_DNS, "https://223.5.5.5/dns-query"), "direct"))
    val dnsRules = JSONArray()
    if (fakeDns) {
        servers.put(
            JSONObject()
                .put("tag", "fakeip")
                .put("type", "fakeip")
                .put("inet4_range", fakeRange4)
                .put("inet6_range", fakeRange6)
        )
        dnsRules.put(
            JSONObject()
                .put("query_type", JSONArray().put("A").put("AAAA"))
                .put("server", "fakeip")
        )
    }
    val dns = JSONObject().put("servers", servers).put("rules", dnsRules).put("final", "remote")
    remoteStrategy?.let { dns.put("strategy", it) }

    val inbounds = JSONArray().put(
        JSONObject()
            .put("type", "mixed")
            .put("tag", "mixed-in")
            .put("listen", if (allowLan) "0.0.0.0" else "127.0.0.1")
            .put("listen_port", mixedPort)
    )
    if (serviceMode == "vpn") {
        inbounds.put(
            JSONObject()
                .put("type", "tun")
                .put("tag", "tun-in")
                .put("address", JSONArray().put(tunAddress4).put(tunAddress6))
                .put("mtu", mtu)
                .put("auto_route", true)
                .put("strict_route", true)
                .put("stack", stack)
                .put("sniff_override_destination", sniffOverride)
                .apply {
                    val key = perAppKey(perAppMode)
                    if (key != null && perAppPackages.isNotEmpty()) put(key, JSONArray(perAppPackages))
                }
        )
    }

    val routeRules = JSONArray()
    if (sniff) routeRules.put(JSONObject().put("action", "sniff"))
    routeRules.put(JSONObject().put("protocol", "dns").put("action", "hijack-dns"))
    if (bypassLan) {
        routeRules.put(JSONObject().put("ip_is_private", true).put("outbound", "direct"))
    }
    rules.forEach { rule ->
        ruleJson(rule, hasProxy)?.let { routeRules.put(it) }
    }

    val outbounds = JSONArray()
    if (profile != null) {
        outbounds.put(JSONObject(profile.outbound.toString()))
    }
    outbounds.put(JSONObject().put("type", "direct").put("tag", "direct"))

    val route = JSONObject()
        .put("rules", routeRules)
        .put("final", proxyTag)
        .put("auto_detect_interface", true)

    val config = JSONObject()
        .put("log", JSONObject().put("level", logLevel).put("timestamp", true))
        .put("dns", dns)
        .put("inbounds", inbounds)
        .put("outbounds", outbounds)
        .put("route", route)
    if (clashApi) {
        config.put(
            "experimental",
            JSONObject().put("clash_api", JSONObject().put("external_controller", "127.0.0.1:9090"))
        )
    }
    return config.toString(2)
}

/**
 * Config for URL tests. Each profile gets its own local mixed inbound on [port] and an outbound of its own,
 * so one core can test a whole group. There is no TUN, so it does not touch the VPN.
 */
fun buildProbeConfig(prefs: Prefs, runs: List<Pair<Profile, Int>>): String {
    val remote = prefs.string(PrefKeys.REMOTE_DNS, "https://dns.google/dns-query")
    val direct = prefs.string(PrefKeys.DIRECT_DNS, "https://223.5.5.5/dns-query")
    val inbounds = JSONArray()
    val outbounds = JSONArray()
    val rules = JSONArray()
    runs.forEachIndexed { index, (profile, port) ->
        val inTag = "probe-in-$index"
        val outTag = "probe-out-$index"
        inbounds.put(
            JSONObject()
                .put("type", "mixed")
                .put("tag", inTag)
                .put("listen", "127.0.0.1")
                .put("listen_port", port)
        )
        outbounds.put(JSONObject(profile.outbound.toString()).put("tag", outTag))
        rules.put(JSONObject().put("inbound", JSONArray().put(inTag)).put("outbound", outTag))
    }
    outbounds.put(JSONObject().put("type", "direct").put("tag", "direct"))
    val servers = JSONArray()
        .put(dnsServer("remote", remote, "direct"))
        .put(dnsServer("local", direct, "direct"))
    val dns = JSONObject().put("servers", servers).put("final", "remote")
    val route = JSONObject()
        .put("rules", rules)
        .put("final", "direct")
        .put("auto_detect_interface", true)
    return JSONObject()
        .put("log", JSONObject().put("level", "warn").put("timestamp", true))
        .put("dns", dns)
        .put("inbounds", inbounds)
        .put("outbounds", outbounds)
        .put("route", route)
        .toString(2)
}

/** Copies the config to the clipboard and tells the user where it went. */
fun copySingBoxConfig(context: Context, prefs: Prefs, profile: Profile?, rules: List<RouteRule>) {
    val json = buildSingBoxConfig(prefs, profile, rules)
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("sing-box config", json))
    Toast.makeText(context, R.string.config_copied, Toast.LENGTH_SHORT).show()
}

private fun ruleJson(rule: RouteRule, hasProxy: Boolean): JSONObject? {
    val values = rule.value.split(',', ' ', '\n', '\t').map { it.trim() }.filter { it.isNotEmpty() }
    if (values.isEmpty()) return null
    val json = JSONObject()
    when (rule.kind) {
        "domain_suffix" -> json.put("domain_suffix", JSONArray(values))
        "domain_keyword" -> json.put("domain_keyword", JSONArray(values))
        "ip_cidr" -> json.put("ip_cidr", JSONArray(values))
        "port" -> {
            val ports = values.mapNotNull { it.toIntOrNull() }
            if (ports.isEmpty()) return null
            json.put("port", JSONArray(ports))
        }
        else -> return null
    }
    return when (rule.outbound) {
        "block" -> json.put("action", "reject")
        "direct" -> json.put("outbound", "direct")
        "proxy" -> if (hasProxy) json.put("outbound", "proxy") else null
        else -> null
    }
}

/** Per-app VPN: "include" limits the VPN to the listed apps, "exclude" keeps them out of it. */
private fun perAppKey(mode: String): String? = when (mode) {
    "include" -> "include_package"
    "exclude" -> "exclude_package"
    else -> null
}

private fun strategyOf(value: String): String? = when (value) {
    "prefer_ipv6", "prefer_ipv4", "ipv4_only", "ipv6_only" -> value
    else -> null
}

/** Turns a DNS URL such as https://dns.google/dns-query into a sing-box server entry. */
private fun dnsServer(tag: String, url: String, detour: String): JSONObject {
    val clean = url.trim()
    val (type, host) = when {
        clean.startsWith("https://") -> "https" to clean.removePrefix("https://").substringBefore("/")
        clean.startsWith("tls://") -> "tls" to clean.removePrefix("tls://")
        clean.startsWith("tcp://") -> "tcp" to clean.removePrefix("tcp://")
        clean.startsWith("udp://") -> "udp" to clean.removePrefix("udp://")
        else -> "udp" to clean
    }
    val server = JSONObject()
        .put("tag", tag)
        .put("type", type)
        .put("server", host)
    // sing-box rejects a detour to an empty direct outbound ("makes no sense"). Direct is the default anyway.
    if (detour != "direct") server.put("detour", detour)
    return server
}
