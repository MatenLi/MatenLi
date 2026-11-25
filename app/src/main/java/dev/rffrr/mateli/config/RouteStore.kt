package dev.rffrr.mateli.config

import android.content.Context
import dev.rffrr.mateli.storage.DataDir
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * One routing rule. [kind] is domain_suffix, domain_keyword, ip_cidr or port.
 * [outbound] is proxy, direct or block.
 */
data class RouteRule(
    val id: String,
    val kind: String,
    val value: String,
    val outbound: String
)

/** Route rules live in a JSON file in app storage. */
class RouteStore(context: Context) {
    private val app = context.applicationContext
    private val file: File get() = DataDir.file(app, "route_rules.json")

    fun list(): List<RouteRule> = if (!file.exists()) defaultRules() else try {
        val arr = JSONArray(file.readText())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RouteRule(
                id = o.getString("id"),
                kind = o.getString("kind"),
                value = o.getString("value"),
                outbound = o.getString("outbound")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun defaultRules(): List<RouteRule> = listOf(
        RouteRule(id = "default-lan", kind = "ip_cidr", value = "10.0.0.0/8,172.16.0.0/12,192.168.0.0/16", outbound = "direct"),
        RouteRule(id = "default-local", kind = "domain_suffix", value = "local,lan", outbound = "direct")
    )

    fun save(rules: List<RouteRule>) {
        val arr = JSONArray()
        rules.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("kind", r.kind)
                    .put("value", r.value)
                    .put("outbound", r.outbound)
            )
        }
        file.writeText(arr.toString())
    }

    fun add(kind: String, value: String, outbound: String) {
        save(list() + RouteRule(UUID.randomUUID().toString(), kind, value, outbound))
    }

    fun remove(id: String) {
        save(list().filterNot { it.id == id })
    }
}
