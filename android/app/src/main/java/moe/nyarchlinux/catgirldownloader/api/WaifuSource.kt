package moe.nyarchlinux.catgirldownloader.api

import moe.nyarchlinux.catgirldownloader.data.NsfwMode
import moe.nyarchlinux.catgirldownloader.data.Settings
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

/** Port of `waifu.WaifuDownloaderAPI`. */
object WaifuSource : ImageSource {
    override val id = "waifu"
    override val filePrefix = "waifu.im"

    private const val MAX_REQUESTS = 3

    internal fun requestUrl(nsfw: NsfwMode, excludedTags: List<String> = emptyList()): HttpUrl {
        val value = when (nsfw) {
            NsfwMode.SHOW_EVERYTHING -> "All"
            NsfwMode.ONLY_NSFW -> "True"
            NsfwMode.BLOCK_NSFW -> "False"
        }
        val builder = "https://waifu-api-031n.onrender.com/images".toHttpUrl().newBuilder()
            .addQueryParameter("IsNsfw", value)
        excludedTags.forEach { builder.addQueryParameter("ExcludedTags", it) }
        return builder.build()
    }

    /** Tags of an item, whether the API sends them as strings or as {"name": ...} objects. */
    private fun tagsOf(item: JSONObject): List<String> {
        val arr = item.optJSONArray("tags") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.stringOrNull("name") ?: arr.optString(i).takeIf { it.isNotBlank() && !it.startsWith("{") }
        }
    }

    /** The image, or null if it has a blacklisted tag. */
    internal fun parse(json: String, blacklist: List<String> = emptyList()): ImagePost? {
        val item = JSONObject(json).getJSONArray("items").getJSONObject(0)
        if (Blacklist.matches(tagsOf(item), blacklist).isNotEmpty()) return null
        val url = item.stringOrNull("url") ?: throw NoImageException("waifu API returned no url")
        val artist = item.optJSONArray("artists")
            ?.optJSONObject(0)
            ?.stringOrNull("name")
        return ImagePost(
            imageUrl = url,
            id = item.stringOrNull("id") ?: "unknown",
            artist = artist,
            link = item.stringOrNull("source"),
        )
    }

    override suspend fun fetchPost(http: Http, settings: Settings): ImagePost {
        val blacklist = Blacklist.parse(settings.blacklistTags)
        val url = requestUrl(settings.nsfwMode, blacklist)
        repeat(MAX_REQUESTS) {
            parse(http.getText(url), blacklist)?.let { return it }
        }
        throw NoImageException("No waifu image without blacklisted tags")
    }
}
