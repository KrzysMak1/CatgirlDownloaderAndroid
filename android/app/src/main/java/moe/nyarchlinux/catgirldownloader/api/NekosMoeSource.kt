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

    internal fun requestUrl(nsfw: NsfwMode): HttpUrl {
        val builder = "https://nekos.moe/api/v1/random/image".toHttpUrl().newBuilder()
        when (nsfw) {
            NsfwMode.ONLY_NSFW -> builder.addQueryParameter("nsfw", "true")
            NsfwMode.BLOCK_NSFW -> builder.addQueryParameter("nsfw", "false")
            NsfwMode.SHOW_EVERYTHING -> Unit
        }
        return builder.build()
    }

    internal fun parse(json: String): ImagePost {
        val image = JSONObject(json).getJSONArray("images").getJSONObject(0)
        val imageId = image.stringOrNull("id") ?: throw NoImageException("nekos.moe returned no id")
        return ImagePost(
            imageUrl = "https://nekos.moe/image/$imageId",
            id = imageId,
            artist = image.stringOrNull("artist"),
            link = "https://nekos.moe/post/$imageId",
        )
    }

    override suspend fun fetchPost(http: Http, settings: Settings): ImagePost =
        parse(http.getText(requestUrl(settings.nsfwMode)))
}
