package com.metro.settings.data.sounds

import com.metro.system.MetroSoundCategory
import com.metro.system.MetroSoundRole
import org.json.JSONObject
import java.util.Locale

/** Parsed `manifest.json` from the bundled Metro sound pack. */
data class MetroSoundPack(
    val packId: String,
    val name: String,
    val version: Int,
    /** role key (e.g. `MESSAGE`, `ALT_RINGTONE_1`) → asset path. */
    val roles: Map<String, String>,
)

/** One concrete sound asset in the pack, normalized for the registry/installer. */
data class MetroSoundAsset(
    /** Stable id derived from the filename, e.g. `metro_beacon`. */
    val id: String,
    /** User-facing title, e.g. `Metro Beacon`. */
    val title: String,
    /** Asset path relative to `metro_sounds/`, e.g. `ringtones/metro_beacon.ogg`. */
    val assetPath: String,
    val category: MetroSoundCategory,
    /** Semantic role, when this asset is a primary role (alternate/UI assets map to null). */
    val role: MetroSoundRole?,
)

data class ParsedSoundPack(
    val pack: MetroSoundPack,
    val assets: List<MetroSoundAsset>,
    /** Non-fatal problems (unknown role keys, unknown categories, duplicate ids). */
    val warnings: List<String>,
)

/**
 * Parses and validates the sound-pack manifest. Pure (no Android APIs) so it is unit-testable.
 *
 * Validation is defensive: unknown role keys / categories are reported as warnings and skipped,
 * rather than failing the whole pack.
 */
object MetroSoundManifest {

    val REQUIRED_ROLES: List<MetroSoundRole> = listOf(
        MetroSoundRole.PHONE_RINGTONE,
        MetroSoundRole.MESSAGE,
        MetroSoundRole.MAIL,
        MetroSoundRole.CALENDAR,
        MetroSoundRole.REMINDER,
        MetroSoundRole.SYSTEM_NOTIFICATION,
        MetroSoundRole.ALARM,
        MetroSoundRole.TIMER,
    )

    fun parse(json: String): ParsedSoundPack {
        val root = JSONObject(json)
        val pack = MetroSoundPack(
            packId = root.optString("packId"),
            name = root.optString("name"),
            version = root.optInt("version", 1),
            roles = emptyMap(),
        )
        val rolesJson = root.optJSONObject("roles") ?: JSONObject()
        val warnings = ArrayList<String>()
        val assets = LinkedHashMap<String, MetroSoundAsset>()

        val keys = rolesJson.keys()
        while (keys.hasNext()) {
            val roleKey = keys.next()
            val path = rolesJson.optString(roleKey).trim()
            if (path.isEmpty()) {
                warnings += "role $roleKey has no asset"
                continue
            }
            val category = categoryForPath(path)
            if (category == null) {
                warnings += "role $roleKey has unknown category: $path"
                continue
            }
            val id = idForPath(path)
            if (id.isEmpty()) {
                warnings += "role $roleKey has invalid asset path: $path"
                continue
            }
            val role = MetroSoundRole.fromKey(roleKey)
            val existing = assets[id]
            if (existing != null) {
                // Same asset referenced by two roles: keep the first, record the primary role if new.
                if (existing.role == null && role != null) {
                    assets[id] = existing.copy(role = role)
                }
                warnings += "duplicate asset id $id (role $roleKey)"
            } else {
                assets[id] = MetroSoundAsset(
                    id = id,
                    title = titleForId(id),
                    assetPath = path,
                    category = category,
                    role = role,
                )
            }
        }

        return ParsedSoundPack(
            pack = pack.copy(roles = rolesJson.let { map ->
                buildMap {
                    val itKeys = map.keys()
                    while (itKeys.hasNext()) {
                        val k = itKeys.next()
                        put(k, map.optString(k))
                    }
                }
            }),
            assets = assets.values.toList(),
            warnings = warnings,
        )
    }

    /** True when every [REQUIRED_ROLES] entry resolves to an asset. */
    fun missingRequiredRoles(pack: ParsedSoundPack): List<MetroSoundRole> {
        val present = pack.assets.mapNotNull { it.role }.toSet()
        return REQUIRED_ROLES.filterNot { it in present }
    }

    fun idForPath(path: String): String =
        path.substringAfterLast('/').substringBeforeLast('.').trim()

    fun categoryForPath(path: String): MetroSoundCategory? {
        val dir = path.trim('/').substringBefore('/')
        return when (dir.lowercase(Locale.US)) {
            "ringtones" -> MetroSoundCategory.RINGTONE
            "notifications" -> MetroSoundCategory.NOTIFICATION
            "alarms" -> MetroSoundCategory.ALARM
            "ui" -> MetroSoundCategory.UI
            else -> null
        }
    }

    /** `metro_beacon` → `Metro Beacon`. */
    fun titleForId(id: String): String =
        id.split('_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }
}
