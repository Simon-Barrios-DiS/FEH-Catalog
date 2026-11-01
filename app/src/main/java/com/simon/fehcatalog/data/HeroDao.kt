package com.simon.fehcatalog.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HeroDao {

    @Query(
        "SELECT * FROM catalog_heroes " +
            "ORDER BY name COLLATE NOCASE ASC, title COLLATE NOCASE ASC"
    )
    fun observeCatalog(): Flow<List<CatalogHero>>

    @Query("SELECT * FROM catalog_heroes WHERE id = :id")
    fun observeCatalogHero(id: Long): Flow<CatalogHero?>

    @Query("SELECT * FROM collection_entries")
    fun observeCollection(): Flow<List<CollectionEntry>>

    @Query("SELECT * FROM collection_entries WHERE heroId = :id")
    fun observeCollectionEntry(id: Long): Flow<CollectionEntry?>

    @Query("SELECT * FROM collection_entries WHERE heroId = :id")
    suspend fun getCollectionEntry(id: Long): CollectionEntry?

    @Query("SELECT COUNT(*) FROM catalog_heroes")
    suspend fun countCatalog(): Int

    @Upsert
    suspend fun upsertCatalog(heroes: List<CatalogHero>)

    @Upsert
    suspend fun upsertCollection(entry: CollectionEntry)
}
