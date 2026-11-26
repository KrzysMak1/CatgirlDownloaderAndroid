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

    internal fun requestUrl(nsfw: NsfwMode): HttpUrl {
        val value = when (nsfw) {
            NsfwMode.SHOW_EVERYTHING -> "All"
            NsfwMode.ONLY_NSFW -> "True"
            NsfwMode.BLOCK_NSFW -> "False"
        }
        return "https://waifu-api-031n.onrender.com/images".toHttpUrl().newBuilder()
            .addQueryParameter("IsNsfw", value)
            .build()
    }

    internal fun parse(json: String): ImagePost {
        val item = JSONObject(json).getJSONArray("items").getJSONObject(0)
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

    override suspend fun fetchPost(http: Http, settings: Settings): ImagePost =
        parse(http.getText(requestUrl(settings.nsfwMode)))
}
