package dev.rffrr.mateli.profiles

import android.content.Context
import dev.rffrr.mateli.storage.DataDir
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** One saved server. [outbound] is a complete sing-box outbound whose tag is "proxy". */
data class Profile(
    val id: String,
    val name: String,
    val type: String,
    val outbound: JSONObject
)

/** Profiles are stored in a JSON file in app storage. Nothing leaves the device. */
class ProfileStore(context: Context) {
    private val app = context.applicationContext
    private val file: File get() = DataDir.file(app, "profiles.json")
    private val selectedFile: File get() = DataDir.file(app, "selected_profile.txt")

    fun list(): List<Profile> = try {
        val arr = JSONArray(file.readText())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Profile(
                id = o.getString("id"),
                name = o.getString("name"),
                type = o.getString("type"),
                outbound = o.getJSONObject("outbound")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun add(profile: Profile) {
        saveAll(list() + profile)
    }

    fun remove(id: String) {
        saveAll(list().filterNot { it.id == id })
        if (selectedId() == id) select(null)
    }

    fun replace(profile: Profile) {
        saveAll(list().map { if (it.id == profile.id) profile else it })
    }

    fun rename(id: String, name: String) {
        saveAll(list().map { if (it.id == id) it.copy(name = name) else it })
    }

    fun selectedId(): String? = try {
        selectedFile.readText().trim().takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        null
    }

    fun select(id: String?) {
        if (id == null) {
            selectedFile.delete()
        } else {
            selectedFile.writeText(id)
        }
    }

    fun selected(): Profile? {
        val id = selectedId() ?: return null
        return list().firstOrNull { it.id == id }
    }

    private fun saveAll(profiles: List<Profile>) {
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("type", p.type)
                    .put("outbound", p.outbound)
            )
        }
        file.writeText(arr.toString())
    }

    fun newId(): String = UUID.randomUUID().toString()
}
