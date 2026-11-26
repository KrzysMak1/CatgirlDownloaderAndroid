package moe.nyarchlinux.catgirldownloader.api

import moe.nyarchlinux.catgirldownloader.data.NsfwMode
import moe.nyarchlinux.catgirldownloader.data.Settings
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

/** Port of `catgirl.CatgirlDownloaderAPI` (nekos.moe). */
object NekosMoeSource : ImageSource {
    override val id = "catgirl"
    override val filePrefix = "nekos.moe"

    /** Random images per request when a blacklist is set, so one request usually suffices. */
    private const val BLACKLIST_BATCH = 10
    private const val MAX_REQUESTS = 3

    internal fun requestUrl(nsfw: NsfwMode, count: Int = 1): HttpUrl {
        val builder = "https://nekos.moe/api/v1/random/image".toHttpUrl().newBuilder()
        when (nsfw) {
            NsfwMode.ONLY_NSFW -> builder.addQueryParameter("nsfw", "true")
            NsfwMode.BLOCK_NSFW -> builder.addQueryParameter("nsfw", "false")
            NsfwMode.SHOW_EVERYTHING -> Unit
        }
        builder.addQueryParameter("count", count.toString())
        return builder.build()
    }

    /** First image without blacklisted tags, or null if every image in the response has some. */
    internal fun parse(json: String, blacklist: List<String> = emptyList()): ImagePost? {
        val images = JSONObject(json).getJSONArray("images")
        for (i in 0 until images.length()) {
            val image = images.optJSONObject(i) ?: continue
            val tags = image.optJSONArray("tags")?.let { arr -> (0 until arr.length()).map { arr.optString(it) } }
            if (Blacklist.matches(tags.orEmpty(), blacklist).isNotEmpty()) continue
            val imageId = image.stringOrNull("id") ?: continue
            return ImagePost(
                imageUrl = "https://nekos.moe/image/$imageId",
                id = imageId,
                artist = image.stringOrNull("artist"),
                link = "https://nekos.moe/post/$imageId",
            )
        }
        return null
    }

    override suspend fun fetchPost(http: Http, settings: Settings): ImagePost {
        val blacklist = Blacklist.parse(settings.blacklistTags)
        val url = requestUrl(settings.nsfwMode, if (blacklist.isEmpty()) 1 else BLACKLIST_BATCH)
        repeat(MAX_REQUESTS) {
            parse(http.getText(url), blacklist)?.let { return it }
        }
        throw NoImageException("No nekos.moe image without blacklisted tags")
    }
}
