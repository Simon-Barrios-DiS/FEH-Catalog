package com.simon.fehcatalog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CatalogHero::class, CollectionEntry::class],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FehDatabase : RoomDatabase() {

    abstract fun heroDao(): HeroDao

    companion object {
        private const val DATABASE_NAME = "feh-catalog.db"

        @Volatile
        private var instance: FehDatabase? = null

        /**
         * La versión 1 guardaba héroes libres con imagen local. El rediseño
         * reemplaza ese catálogo por uno importado desde la wiki y separa la
         * colección del usuario. Se descarta la tabla vieja: sus datos no se
         * pueden mapear de forma fiable a los héroes base.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `heroes`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `catalog_heroes` (
                        `id` INTEGER NOT NULL,
                        `page` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `imageUrl` TEXT,
                        `movement` TEXT NOT NULL,
                        `weaponColor` TEXT NOT NULL,
                        `weaponType` TEXT NOT NULL,
                        `originGames` TEXT NOT NULL,
                        `gameSort` INTEGER NOT NULL,
                        `unitTypes` TEXT NOT NULL,
                        `bannerType` TEXT,
                        `releaseDate` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_catalog_heroes_page` " +
                        "ON `catalog_heroes` (`page`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_catalog_heroes_name` " +
                        "ON `catalog_heroes` (`name`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_catalog_heroes_gameSort` " +
                        "ON `catalog_heroes` (`gameSort`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `collection_entries` (
                        `heroId` INTEGER NOT NULL,
                        `owned` INTEGER NOT NULL,
                        `copies` INTEGER NOT NULL,
                        `kept` INTEGER NOT NULL,
                        `merges` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`heroId`),
                        FOREIGN KEY(`heroId`) REFERENCES `catalog_heroes`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * La versión 3 agrega la versión de FEH en la que debutó cada héroe.
         * Es una columna opcional para no romper catálogos generados sin el
         * Excel de versiones.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `catalog_heroes` ADD COLUMN `gameVersion` TEXT"
                )
            }
        }

        /**
         * La versión 4 reemplaza el favorito booleano por uno de los ocho
         * tipos de favorito de FEH. Los favoritos existentes se convierten en
         * el tipo 1 (rosa); el resto queda en 0 (sin favorito).
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `collection_entries_new` (
                        `heroId` INTEGER NOT NULL,
                        `owned` INTEGER NOT NULL,
                        `copies` INTEGER NOT NULL,
                        `kept` INTEGER NOT NULL,
                        `merges` INTEGER NOT NULL,
                        `favoriteSlot` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`heroId`),
                        FOREIGN KEY(`heroId`) REFERENCES `catalog_heroes`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `collection_entries_new`
                        (`heroId`, `owned`, `copies`, `kept`, `merges`,
                         `favoriteSlot`, `notes`, `updatedAt`)
                    SELECT
                        `heroId`, `owned`, `copies`, `kept`, `merges`,
                        CASE WHEN `isFavorite` THEN 1 ELSE 0 END,
                        `notes`, `updatedAt`
                    FROM `collection_entries`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `collection_entries`")
                db.execSQL(
                    "ALTER TABLE `collection_entries_new` RENAME TO `collection_entries`"
                )
            }
        }

        /**
         * La versión 5 agrega la etiqueta Special Rate (columna histórica
         * `revival`) al catálogo. Las filas existentes empiezan en 0.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `catalog_heroes` " +
                        "ADD COLUMN `revival` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Puente para bases v5: su esquema ya coincide con v7. Las bases v6
         * pasan por [MIGRATION_6_7].
         */
        private val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Sin cambios de esquema.
            }
        }

        /**
         * La versión 7 elimina una columna temporal de `catalog_heroes`. Se
         * recrea la tabla para no depender de `DROP COLUMN`, que no existe en
         * las versiones antiguas de SQLite soportadas por la app.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `catalog_heroes_new` (
                        `id` INTEGER NOT NULL,
                        `page` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `imageUrl` TEXT,
                        `movement` TEXT NOT NULL,
                        `weaponColor` TEXT NOT NULL,
                        `weaponType` TEXT NOT NULL,
                        `originGames` TEXT NOT NULL,
                        `gameSort` INTEGER NOT NULL,
                        `unitTypes` TEXT NOT NULL,
                        `bannerType` TEXT,
                        `releaseDate` TEXT,
                        `gameVersion` TEXT,
                        `revival` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `catalog_heroes_new`
                        (`id`, `page`, `name`, `title`, `imageUrl`, `movement`,
                         `weaponColor`, `weaponType`, `originGames`, `gameSort`,
                         `unitTypes`, `bannerType`, `releaseDate`, `gameVersion`,
                         `revival`)
                    SELECT
                        `id`, `page`, `name`, `title`, `imageUrl`, `movement`,
                        `weaponColor`, `weaponType`, `originGames`, `gameSort`,
                        `unitTypes`, `bannerType`, `releaseDate`, `gameVersion`,
                        `revival`
                    FROM `catalog_heroes`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `catalog_heroes`")
                db.execSQL(
                    "ALTER TABLE `catalog_heroes_new` RENAME TO `catalog_heroes`"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_catalog_heroes_page` " +
                        "ON `catalog_heroes` (`page`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_catalog_heroes_name` " +
                        "ON `catalog_heroes` (`name`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_catalog_heroes_gameSort` " +
                        "ON `catalog_heroes` (`gameSort`)"
                )
            }
        }

        fun getInstance(context: Context): FehDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FehDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_7,
                        MIGRATION_6_7
                    )
                    .build()
                    .also { instance = it }
            }
    }
}
