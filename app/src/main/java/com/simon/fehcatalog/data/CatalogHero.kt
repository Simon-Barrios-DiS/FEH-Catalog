package com.simon.fehcatalog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.simon.fehcatalog.model.FireEmblemGame
import com.simon.fehcatalog.model.MovementType
import com.simon.fehcatalog.model.UnitType
import com.simon.fehcatalog.model.WeaponColor
import com.simon.fehcatalog.model.WeaponType

/**
 * Héroe del catálogo base. Es información inmutable que se importa desde la
 * wiki; no se crea ni se edita desde la interfaz. Los datos del usuario
 * (posesión, copias, favoritos, etc.) viven en [CollectionEntry].
 */
@Entity(
    tableName = "catalog_heroes",
    indices = [
        Index(value = ["page"], unique = true),
        Index(value = ["name"]),
        Index(value = ["gameSort"])
    ]
)
data class CatalogHero(
    @PrimaryKey val id: Long,
    val page: String,
    val name: String,
    val title: String,
    val imageUrl: String?,
    val movement: MovementType,
    val weaponColor: WeaponColor,
    val weaponType: WeaponType,
    val originGames: Set<FireEmblemGame>,
    val gameSort: Int,
    val unitTypes: Set<UnitType>,
    /**
     * Origen del héroe: tema seasonal corto (por ejemplo, `Verano`) o categoría
     * de invocación (`New Heroes`, `Legendary`, `Mythic`, `Emblem`). Es null
     * cuando el héroe no pertenece a un banner de invocación (GHB, Tempest,
     * Story, etc.).
     */
    val bannerType: String? = null,
    val releaseDate: String? = null,
    /**
     * Versión de Fire Emblem Heroes en la que debutó el héroe, tal como la
     * registra `Versiones.xlsx` (por ejemplo, `1.0`, `9.11` o `10.10`).
     */
    val gameVersion: String? = null,
    /**
     * Etiqueta Special Rate (históricamente `revival`): unidades cuya condición
     * de obtención cambia ligeramente. Se conserva el nombre de columna para no
     * romper bases de datos existentes.
     */
    val revival: Boolean = false
) {
    /** "Abel: The Panther" (o solo el nombre si no tiene título). */
    val displayName: String
        get() = if (title.isBlank()) name else "$name: $title"
}
