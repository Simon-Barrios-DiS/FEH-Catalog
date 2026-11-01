package com.simon.fehcatalog.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.simon.fehcatalog.model.FavoriteType

/**
 * Datos personales de un héroe del catálogo. Solo se crea una fila cuando el
 * usuario interactúa con el héroe; si no existe, se asume todo en falso/cero.
 *
 * - [owned]: el jugador obtuvo el héroe (aunque ya no lo conserve).
 * - [copies]: copias actuales; 0 si ya no lo conserva.
 * - [kept]: todavía conserva al menos una copia.
 * - [merges]: merges actuales, de +0 a +10.
 * - [favoriteSlot]: 0 = sin favorito; 1..8 = uno de los ocho colores.
 */
@Entity(
    tableName = "collection_entries",
    foreignKeys = [
        ForeignKey(
            entity = CatalogHero::class,
            parentColumns = ["id"],
            childColumns = ["heroId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CollectionEntry(
    @PrimaryKey val heroId: Long,
    val owned: Boolean = false,
    val copies: Int = 0,
    val kept: Boolean = false,
    val merges: Int = 0,
    val favoriteSlot: Int = 0,
    val notes: String = "",
    val updatedAt: Long = 0L
) {
    val favoriteType: FavoriteType? get() = FavoriteType.fromSlot(favoriteSlot)
    val isFavorite: Boolean get() = favoriteSlot > 0

    companion object {
        fun empty(heroId: Long): CollectionEntry = CollectionEntry(heroId = heroId)
    }
}

/** Héroe del catálogo junto con la colección del usuario. */
data class HeroWithCollection(
    val hero: CatalogHero,
    val collection: CollectionEntry
) {
    val id: Long get() = hero.id
    val isFavorite: Boolean get() = collection.isFavorite
    val favoriteType: FavoriteType? get() = collection.favoriteType
    val owned: Boolean get() = collection.owned
    val kept: Boolean get() = collection.kept
    val copies: Int get() = collection.copies
    val merges: Int get() = collection.merges
}

fun CatalogHero.withCollection(entry: CollectionEntry?): HeroWithCollection =
    HeroWithCollection(this, entry ?: CollectionEntry.empty(id))
