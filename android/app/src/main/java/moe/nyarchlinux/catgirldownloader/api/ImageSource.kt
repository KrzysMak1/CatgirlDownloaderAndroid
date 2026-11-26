package moe.nyarchlinux.catgirldownloader.api

import moe.nyarchlinux.catgirldownloader.data.Settings
import org.json.JSONObject
import java.io.IOException

/** What a source returns for one random image (replaces the desktop `info` dict). */
data class ImagePost(
    val imageUrl: String,
    val id: String,
    val artist: String?,
    val link: String?,
)

class NoImageException(message: String) : IOException(message)

/** Port of `api_base.BaseDownloaderAPI`. */
interface ImageSource {
    val id: String

    /** Prefix used in suggested file names, e.g. `nekos.moe` → `nekos.moe_<id>.png`. */
    val filePrefix: String

    /** Headers needed when downloading the image file itself. */
    val imageHeaders: Map<String, String> get() = emptyMap()

    suspend fun fetchPost(http: Http, settings: Settings): ImagePost

    fun fileName(post: ImagePost, extension: String?): String =
        if (extension.isNullOrEmpty()) "${filePrefix}_${post.id}" else "${filePrefix}_${post.id}.$extension"
}

/** Like optString, but returns null for missing, JSON null and blank values. */
internal fun JSONObject.stringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
