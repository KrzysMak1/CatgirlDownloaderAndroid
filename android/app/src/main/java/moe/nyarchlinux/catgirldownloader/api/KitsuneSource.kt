package moe.nyarchlinux.catgirldownloader.api

import moe.nyarchlinux.catgirldownloader.data.Settings
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

/**
 * Port of `kitsune.KitsuneDownloaderAPI` (nekos.best). nekos.best only serves SFW content,
 * so the NSFW mode is intentionally ignored.
 *
 * The desktop app notes that nekos.best's Cloudflare rejects custom and browser User-Agents,
 * so like the original this sends the HTTP library's default User-Agent.
 */
object KitsuneSource : ImageSource {
    override val id = "kitsune"
    override val filePrefix = "nekos.best"

    private val endpoint = "https://nekos.best/api/v2/kitsune".toHttpUrl()

    internal fun parse(json: String): ImagePost {
        val result = JSONObject(json).getJSONArray("results").getJSONObject(0)
        val url = result.stringOrNull("url") ?: throw NoImageException("nekos.best returned no url")
        return ImagePost(
            imageUrl = url,
            id = url.substringAfterLast('/').substringBeforeLast('.'),
            artist = result.stringOrNull("artist_name"),
            link = result.stringOrNull("source_url"),
        )
    }

    override suspend fun fetchPost(http: Http, settings: Settings): ImagePost =
        parse(http.getText(endpoint))
}
