package com.simon.fehcatalog.ui.catalog

import com.simon.fehcatalog.data.HeroWithCollection
import com.simon.fehcatalog.model.WeaponType
import com.simon.fehcatalog.model.searchKey

/** Criterios de orden disponibles en el catálogo. */
enum class CatalogSort(val label: String) {
    ALPHABETICAL("Alfabético"),
    WEAPON_TYPE("Tipo"),
    GAME("Juego"),
    MOVEMENT("Movimiento"),
    COLOR("Color"),
    VERSION("Versión")
}

/**
 * Comparador del catálogo. Los favoritos se mantienen siempre al principio y
 * el resto se ordena con el criterio elegido, usando el nombre como desempate.
 */
internal fun catalogComparator(
    sort: CatalogSort,
    ascending: Boolean
): Comparator<HeroWithCollection> {
    val primary: Comparator<HeroWithCollection> = when (sort) {
        CatalogSort.ALPHABETICAL ->
            direction(ascending) { it.hero.name.searchKey() }

        CatalogSort.WEAPON_TYPE ->
            direction(ascending) { WeaponType.sortIndex(it.hero.weaponType) }

        CatalogSort.GAME ->
            direction(ascending) { it.hero.gameSort }

        CatalogSort.MOVEMENT ->
            direction(ascending) { it.hero.movement.ordinal }

        CatalogSort.COLOR ->
            direction(ascending) { it.hero.weaponColor.ordinal }

        CatalogSort.VERSION ->
            direction(ascending) { versionKey(it.hero.gameVersion) }
    }

    val secondary = if (sort == CatalogSort.ALPHABETICAL) {
        compareBy<HeroWithCollection> { it.hero.title.searchKey() }
    } else {
        compareBy<HeroWithCollection> { it.hero.name.searchKey() }
            .thenBy { it.hero.title.searchKey() }
    }

    return compareByDescending<HeroWithCollection> { it.isFavorite }
        .then(primary)
        .then(secondary)
}

/** Compara como número: `10.11` es mayor que `9.10` y que `10.2`. */
internal fun versionKey(value: String?): Long {
    if (value.isNullOrBlank()) return Long.MAX_VALUE
    val parts = value.split('.')
    val major = parts.getOrNull(0)?.toIntOrNull() ?: return Long.MAX_VALUE
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return major.toLong() * 1000L + minor
}

private fun <T : Comparable<T>> direction(
    ascending: Boolean,
    selector: (HeroWithCollection) -> T
): Comparator<HeroWithCollection> =
    if (ascending) compareBy(selector) else compareByDescending(selector)
