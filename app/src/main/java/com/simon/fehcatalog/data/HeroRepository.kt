package com.simon.fehcatalog.data

import com.simon.fehcatalog.model.normalizeCopies
import com.simon.fehcatalog.model.normalizeFavoriteSlot
import com.simon.fehcatalog.model.normalizeMerges
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class HeroRepository(
    private val dao: HeroDao,
    private val seeder: CatalogSeeder
) {

    fun observeHeroes(): Flow<List<HeroWithCollection>> =
        combine(dao.observeCatalog(), dao.observeCollection()) { heroes, entries ->
            val byHeroId = entries.associateBy { it.heroId }
            heroes.map { hero -> hero.withCollection(byHeroId[hero.id]) }
        }

    fun observeHero(id: Long): Flow<HeroWithCollection?> =
        combine(
            dao.observeCatalogHero(id),
            dao.observeCollectionEntry(id)
        ) { hero, entry ->
            hero?.withCollection(entry)
        }

    suspend fun ensureCatalogSeeded(force: Boolean = false) {
        seeder.seedIfNeeded(force)
    }

    suspend fun setOwned(heroId: Long, owned: Boolean) {
        val current = currentEntry(heroId)
        val updated = if (owned) {
            current.copy(
                owned = true,
                kept = true,
                copies = current.copies.coerceAtLeast(1),
                updatedAt = now()
            )
        } else {
            current.copy(
                owned = false,
                kept = false,
                copies = 0,
                merges = 0,
                updatedAt = now()
            )
        }
        dao.upsertCollection(updated)
    }

    suspend fun setCopies(heroId: Long, copies: Int) {
        val current = currentEntry(heroId)
        dao.upsertCollection(
            current.copy(
                owned = true,
                kept = true,
                copies = normalizeCopies(copies).coerceAtLeast(1),
                updatedAt = now()
            )
        )
    }

    suspend fun setKept(heroId: Long, kept: Boolean) {
        val current = currentEntry(heroId)
        val updated = if (kept) {
            current.copy(
                owned = true,
                kept = true,
                copies = current.copies.coerceAtLeast(1),
                updatedAt = now()
            )
        } else {
            current.copy(
                kept = false,
                copies = 0,
                merges = 0,
                updatedAt = now()
            )
        }
        dao.upsertCollection(updated)
    }

    suspend fun setMerges(heroId: Long, merges: Int) {
        val current = currentEntry(heroId)
        dao.upsertCollection(
            current.copy(
                merges = normalizeMerges(merges),
                updatedAt = now()
            )
        )
    }

    suspend fun setFavorite(heroId: Long, favoriteSlot: Int) {
        val current = currentEntry(heroId)
        dao.upsertCollection(
            current.copy(
                favoriteSlot = normalizeFavoriteSlot(favoriteSlot),
                updatedAt = now()
            )
        )
    }

    suspend fun setNotes(heroId: Long, notes: String) {
        val current = currentEntry(heroId)
        dao.upsertCollection(
            current.copy(
                notes = notes.trim(),
                updatedAt = now()
            )
        )
    }

    private suspend fun currentEntry(heroId: Long): CollectionEntry =
        dao.getCollectionEntry(heroId) ?: CollectionEntry.empty(heroId)

    private fun now(): Long = System.currentTimeMillis()
}
