package com.simon.fehcatalog.data

import android.content.Context

/**
 * Contenedor de dependencias manual. Al ser una app pequeña no necesitamos Hilt;
 * si el proyecto crece, este es el punto único que habría que reemplazar.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: FehDatabase by lazy { FehDatabase.getInstance(appContext) }

    private val catalogSeeder: CatalogSeeder by lazy {
        CatalogSeeder(appContext, database.heroDao())
    }

    val heroRepository: HeroRepository by lazy {
        HeroRepository(database.heroDao(), catalogSeeder)
    }
}
