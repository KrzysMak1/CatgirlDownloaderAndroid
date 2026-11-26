package moe.nyarchlinux.catgirldownloader.api

import moe.nyarchlinux.catgirldownloader.data.NsfwMode
import moe.nyarchlinux.catgirldownloader.data.Settings
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

/** Tags Danbooru censors for anonymous users; kept base64-encoded as in the desktop app. */
object DanbooruTags {
    private val forbidden: Set<String> = listOf("c2hvdGE=", "bG9saQ==")
        .map { String(Base64.getDecoder().decode(it), Charsets.UTF_8) }
        .toSet()

    private val whitespace = Regex("\\s+")

    private fun split(tags: String): List<String> =
        tags.lowercase().split(whitespace).filter { it.isNotEmpty() }

    /** Lower-cases the input and drops forbidden tags (desktop `_on_tags_changed`). */
    fun sanitize(tags: String): String = split(tags).filterNot { it in forbidden }.joinToString(" ")

    fun containsForbidden(tags: String): Boolean = split(tags).any { it in forbidden }

    internal fun isForbiddenPost(tagString: String): Boolean =
        tagString.split(' ').any { it in forbidden }
}

/** Port of `danbooru.DanbooruDownloaderAPI`. */
object DanbooruSource : ImageSource {
    override val id = "danbooru"
    override val filePrefix = "danbooru"

    private const val ENDPOINT = "https://danbooru.donmai.us"
    private const val MAX_RETRIES = 5

    /** Danbooru posts can be videos or ugoira archives; only keep formats we can show. */
    private val displayableExtensions = setOf("png", "jpg", "jpeg", "gif", "webp", "avif")

    private val headers = mapOf(
        "User-Agent" to "CatgirlDownloader/1.0 (+https://github.com/NyarchLinux/CatgirlDownloader)",
    )

    override val imageHeaders: Map<String, String> get() = headers

    internal fun buildTagsQuery(tags: String, nsfw: NsfwMode): String {
        val trimmed = tags.trim()
        val rating = when (nsfw) {
            NsfwMode.BLOCK_NSFW -> "rating:general"
            NsfwMode.ONLY_NSFW -> "rating:explicit"
            NsfwMode.SHOW_EVERYTHING -> null
        }
        return listOfNotNull(trimmed.takeIf { it.isNotEmpty() }, rating).joinToString(" ")
    }

    internal fun requestUrl(tags: String, nsfw: NsfwMode): HttpUrl {
        val builder = "$ENDPOINT/posts.json".toHttpUrl().newBuilder()
            .addQueryParameter("limit", "1")
            .addQueryParameter("random", "true")
        val query = buildTagsQuery(tags, nsfw)
        if (query.isNotEmpty()) builder.addQueryParameter("tags", query)
        return builder.build()
    }

    /** Returns null when the post must be skipped (forbidden tags, no file, unsupported format). */
    internal fun parsePost(post: JSONObject): ImagePost? {
        if (DanbooruTags.isForbiddenPost(post.optString("tag_string"))) return null
        val fileUrl = post.stringOrNull("file_url") ?: return null
        val ext = post.stringOrNull("file_ext")?.lowercase()
        if (ext != null && ext !in displayableExtensions) return null
        val postId = post.stringOrNull("id") ?: return null
        return ImagePost(
            imageUrl = fileUrl,
            id = postId,
            artist = post.stringOrNull("tag_string_artist")?.split(' ')?.firstOrNull { it.isNotEmpty() },
            link = "$ENDPOINT/posts/$postId",
        )
    }

    override suspend fun fetchPost(http: Http, settings: Settings): ImagePost {
        val url = requestUrl(DanbooruTags.sanitize(settings.danbooruTags), settings.nsfwMode)
        repeat(MAX_RETRIES) {
            val posts = JSONArray(http.getText(url, headers))
            if (posts.length() == 0) throw NoImageException("No Danbooru posts match these tags")
            parsePost(posts.getJSONObject(0))?.let { return it }
        }
        throw NoImageException("Could not find a suitable Danbooru post after $MAX_RETRIES attempts")
    }
}
