package com.simon.fehcatalog.data

import android.content.Context
import android.util.Log
import com.simon.fehcatalog.model.FireEmblemGame
import com.simon.fehcatalog.model.MovementType
import com.simon.fehcatalog.model.UnitType
import com.simon.fehcatalog.model.WeaponColor
import com.simon.fehcatalog.model.WeaponType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Importa el catálogo base desde `assets/heroes.json`.
 *
 * El archivo se genera con tools/generate_catalog.py. La versión es un hash
 * del contenido: si cambia el JSON, la próxima apertura vuelve a hacer upsert.
 * El upsert solo toca `catalog_heroes`; la colección del usuario se conserva.
 */
class CatalogSeeder(
    context: Context,
    private val dao: HeroDao
) {
    private val appContext = context.applicationContext
    private val prefs =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    suspend fun seedIfNeeded(force: Boolean = false) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val seed = readSeed()
            val storedVersion = prefs.getString(KEY_VERSION, null)
            if (!force && storedVersion == seed.version && dao.countCatalog() > 0) {
                return@withContext
            }

            seed.heroes.chunked(200).forEach { batch ->
                dao.upsertCatalog(batch)
            }
            prefs.edit().putString(KEY_VERSION, seed.version).apply()
            Log.i(TAG, "Catálogo importado: ${seed.heroes.size} héroes (${seed.version})")
        }
    }

    private fun readSeed(): Seed {
        val raw = appContext.assets.open(ASSET_NAME)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val root = JSONObject(raw)
        val version = root.getString("version")
        val array = root.getJSONArray("heroes")
        val heroes = ArrayList<CatalogHero>(array.length())
        for (index in 0 until array.length()) {
            val objectValue = array.getJSONObject(index)
            heroes += try {
                parseHero(objectValue)
            } catch (error: Exception) {
                throw IllegalStateException(
                    "Héroe inválido en el catálogo: ${objectValue.optString("page")}",
                    error
                )
            }
        }
        check(heroes.isNotEmpty()) { "El catálogo base está vacío" }
        return Seed(version, heroes)
    }

    private fun parseHero(obj: JSONObject): CatalogHero {
        val games = obj.getJSONArray("originGames")
            .toStringSet()
            .mapNotNull(FireEmblemGame::fromName)
            .toSet()
        check(games.isNotEmpty()) { "sin juegos de origen" }

        val types = obj.getJSONArray("unitTypes")
            .toStringSet()
            .mapNotNull(UnitType::fromName)
            .toSet()
        check(types.isNotEmpty()) { "sin tipos de unidad" }

        return CatalogHero(
            id = obj.getLong("id"),
            page = obj.getString("page"),
            name = obj.getString("name"),
            title = obj.optString("title", ""),
            imageUrl = obj.optNullableString("imageUrl"),
            movement = MovementType.valueOf(obj.getString("movement")),
            weaponColor = WeaponColor.valueOf(obj.getString("weaponColor")),
            weaponType = WeaponType.valueOf(obj.getString("weaponType")),
            originGames = games,
            gameSort = obj.getInt("gameSort"),
            unitTypes = types,
            bannerType = obj.optNullableString("bannerType"),
            releaseDate = obj.optNullableString("releaseDate"),
            gameVersion = obj.optNullableString("gameVersion"),
            revival = obj.optBoolean("revival", false)
        )
    }

    private fun JSONArray.toStringSet(): Set<String> =
        buildSet {
            for (index in 0 until length()) {
                add(getString(index))
            }
        }

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private data class Seed(
        val version: String,
        val heroes: List<CatalogHero>
    )

    private companion object {
        const val ASSET_NAME = "heroes.json"
        const val PREFS_NAME = "catalog_seed"
        const val KEY_VERSION = "version"
        const val TAG = "CatalogSeeder"
    }
}
