package dev.rffrr.mateli.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.rffrr.mateli.storage.DataDir
import org.json.JSONObject

/**
 * Settings in settings.json inside the data folder. One shared instance per process, so every
 * screen sees the same values. Each change is written to disk at once.
 */
class Prefs private constructor(private val context: Context) {
    private val values = HashMap<String, Any>()

    init {
        reload()
    }

    @Synchronized
    fun reload() {
        values.clear()
        val json = runCatching { JSONObject(DataDir.file(context, FILE).readText()) }.getOrNull() ?: return
        json.keys().forEach { key -> json.opt(key)?.let { values[key] = it } }
    }

    @Synchronized
    fun bool(key: String, default: Boolean): Boolean = (values[key] as? Boolean) ?: default

    @Synchronized
    fun putBool(key: String, value: Boolean) = put(key, value)

    @Synchronized
    fun string(key: String, default: String): String = (values[key] as? String) ?: default

    @Synchronized
    fun putString(key: String, value: String) = put(key, value)

    private fun put(key: String, value: Any) {
        values[key] = value
        save()
    }

    private fun save() {
        val json = JSONObject()
        values.forEach { (key, value) -> json.put(key, value) }
        runCatching { DataDir.file(context, FILE).writeText(json.toString()) }
    }

    companion object {
        const val FILE = "settings.json"

        @Volatile
        private var instance: Prefs? = null

        fun get(context: Context): Prefs = instance ?: synchronized(this) {
            instance ?: Prefs(context.applicationContext).also { instance = it }
        }
    }
}

@Composable
fun rememberPrefs(): Prefs {
    val context = LocalContext.current
    return remember { Prefs.get(context) }
}

/** Keys follow NekoBox's preference keys where the setting is the same. */
object PrefKeys {
    const val AUTO_CONNECT = "isAutoConnect"
    const val NIGHT_THEME = "nightTheme"
    const val SERVICE_MODE = "serviceMode"
    const val TUN_IMPLEMENTATION = "tunImplementation"
    const val MTU = "mtu"
    const val SPEED_INTERVAL = "speedInterval"
    const val PROFILE_TRAFFIC_STATS = "profileTrafficStatistics"
    const val SHOW_DIRECT_SPEED = "showDirectSpeed"
    const val SHOW_GROUP_IN_NOTIFICATION = "showGroupInNotification"
    const val ALWAYS_SHOW_ADDRESS = "alwaysShowAddress"
    const val METERED = "meteredNetwork"
    const val ACQUIRE_WAKE_LOCK = "acquireWakeLock"
    const val LOG_LEVEL = "logLevel"
    const val CUSTOM_CONFIG = "globalCustomConfig"
    const val PROXY_APPS = "proxyApps"
    const val BYPASS_LAN = "bypassLan"
    const val BYPASS_LAN_IN_CORE = "bypassLanInCore"
    const val TRAFFIC_SNIFFING = "trafficSniffing"
    const val RESOLVE_DESTINATION = "resolveDestination"
    const val IPV6_MODE = "ipv6Mode"
    const val RULES_PROVIDER = "rulesProvider"
    const val REMOTE_DNS = "remoteDns"
    const val DOMAIN_STRATEGY_REMOTE = "domain_strategy_for_remote"
    const val DIRECT_DNS = "directDns"
    const val DOMAIN_STRATEGY_DIRECT = "domain_strategy_for_direct"
    const val DOMAIN_STRATEGY_SERVER = "domain_strategy_for_server"
    const val ENABLE_DNS_ROUTING = "enableDnsRouting"
    const val ENABLE_FAKEDNS = "enableFakeDns"
    const val MIXED_PORT = "mixedPort"
    const val APPEND_HTTP_PROXY = "appendHttpProxy"
    const val ALLOW_ACCESS = "allowAccess"
    const val CONNECTION_TEST_URL = "connectionTestURL"
    const val ENABLE_CLASH_API = "enableClashAPI"
    const val NETWORK_CHANGE_RESET = "networkChangeResetConnections"
    const val WAKE_RESET = "wakeResetConnections"
    const val GLOBAL_ALLOW_INSECURE = "globalAllowInsecure"
    const val ALLOW_INSECURE_ON_REQUEST = "allowInsecureOnRequest"
    const val APP_TLS_VERSION = "appTLSVersion"
    const val APP_THEME = "appTheme"
    const val CUSTOM_ACCENT = "customAccent"
    const val SHOW_BOTTOM_BAR = "showBottomBar"
    const val COUNTRY_LOOKUP = "countryLookup"
    const val IP2LOCATION_KEY = "ip2locationKey"
    const val TUN_ADDRESS4 = "tunAddress4"
    const val TUN_ADDRESS6 = "tunAddress6"
    const val FAKEDNS_RANGE4 = "fakednsRange4"
    const val FAKEDNS_RANGE6 = "fakednsRange6"
    const val PER_APP_MODE = "perAppMode"
    const val PER_APP_PACKAGES = "perAppPackages"
    const val MIRROR_ENABLED = "mirrorEnabled"
    const val MIRROR_BASE = "mirrorBase"
    const val MIRROR_UA = "mirrorUa"
    const val MIRROR_OS = "mirrorOs"
    const val MIRROR_SUA = "mirrorSua"
    const val MIRROR_HWID = "mirrorHwid"
    const val MIRROR_FMT = "mirrorFmt"
}
