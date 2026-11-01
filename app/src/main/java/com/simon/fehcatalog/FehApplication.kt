package com.simon.fehcatalog

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.simon.fehcatalog.data.AppContainer
import okhttp3.OkHttpClient

class FehApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Los retratos viven en la CDN de Fandom y esa CDN exige un Referer
        // de la wiki. Coil no lo envía por defecto, así que lo añadimos a
        // todas las peticiones de imágenes.
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient {
                    OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            val request = chain.request()
                                .newBuilder()
                                .header("Referer", "https://feheroes.fandom.com/")
                                .build()
                            chain.proceed(request)
                        }
                        .build()
                }
                .build()
        )
    }
}
