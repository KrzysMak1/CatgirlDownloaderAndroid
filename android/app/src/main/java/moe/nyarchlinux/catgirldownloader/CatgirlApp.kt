package moe.nyarchlinux.catgirldownloader

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import moe.nyarchlinux.catgirldownloader.api.Http
import moe.nyarchlinux.catgirldownloader.data.SettingsRepository

class CatgirlApp : Application(), ImageLoaderFactory {
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val http: Http by lazy { Http() }

    /** ImageDecoder handles animated GIF/WebP and AVIF on Android 12+. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(ImageDecoderDecoder.Factory()) }
        .crossfade(true)
        .build()
}
