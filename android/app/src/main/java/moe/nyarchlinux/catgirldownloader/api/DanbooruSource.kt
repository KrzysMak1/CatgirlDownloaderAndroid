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

    /** A tag name containing a forbidden word, e.g. as one part of "x_y" (upstream PR #23). */
    internal fun isForbiddenName(name: String): Boolean =
        name.lowercase().split('_').any { it in forbidden }
}

/** Port of `danbooru.DanbooruDownloaderAPI`. */
object DanbooruSource : ImageSource {
    override val id = "danbooru"
    override val filePrefix = "danbooru"

    private const val ENDPOINT = "https://danbooru.donmai.us"
    /** Random posts fetched per request; the first suitable one is used. */
    private const val BATCH_SIZE = 20

    /** Danbooru allows anonymous users to search for at most this many tags. */
    const val MAX_ANONYMOUS_TAGS = 2
    private const val SUGGESTION_LIMIT = 10
    private const val MAX_REQUESTS = 3

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
            .addQueryParameter("limit", BATCH_SIZE.toString())
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
        // The sample (~850px) is far smaller than the original and loads much faster;
        // the original is only downloaded when the image is saved.
        val sampleUrl = post.stringOrNull("large_file_url")
            ?.takeIf { url -> displayableExtensions.any { url.substringBefore('?').endsWith(".$it", ignoreCase = true) } }
        val useSample = sampleUrl != null && sampleUrl != fileUrl
        return ImagePost(
            imageUrl = if (useSample) sampleUrl!! else fileUrl,
            fullImageUrl = if (useSample) fileUrl else null,
            fullImageExtension = if (useSample) ext else null,
            id = postId,
            artist = post.stringOrNull("tag_string_artist")?.split(' ')?.firstOrNull { it.isNotEmpty() },
            link = "$ENDPOINT/posts/$postId",
        )
    }

    internal fun firstSuitable(posts: JSONArray, blacklist: List<String> = emptyList()): ImagePost? =
        (0 until posts.length()).firstNotNullOfOrNull { i ->
            posts.optJSONObject(i)
                // Filtered here rather than with "-tag" in the query: excluded tags count
                // towards Danbooru's 2-tag limit for anonymous users.
                ?.takeIf { Blacklist.matches(it.optString("tag_string").split(' '), blacklist).isEmpty() }
                ?.let(::parsePost)
        }

    internal fun suggestionsUrl(word: String): HttpUrl =
        "$ENDPOINT/tags.json".toHttpUrl().newBuilder()
            .addQueryParameter("search[name_matches]", "$word*")
            .addQueryParameter("search[order]", "count")
            .addQueryParameter("limit", SUGGESTION_LIMIT.toString())
            .build()

    internal fun parseSuggestions(json: String): List<TagSuggestion> {
        val arr = JSONArray(json)
        return (0 until arr.length()).mapNotNull { i ->
            val tag = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = tag.stringOrNull("name") ?: return@mapNotNull null
            if (DanbooruTags.isForbiddenName(name)) null else TagSuggestion(name, tag.optInt("post_count"))
        }
    }

    /** Most used Danbooru tags starting with [word], for autocomplete while typing. */
    suspend fun suggestTags(http: Http, word: String): List<TagSuggestion> {
        if (word.isBlank() || DanbooruTags.isForbiddenName(word)) return emptyList()
        return parseSuggestions(http.getText(suggestionsUrl(word), headers))
    }

    override suspend fun fetchPost(http: Http, settings: Settings): ImagePost {
        val url = requestUrl(DanbooruTags.sanitize(settings.danbooruTags), settings.nsfwMode)
        repeat(MAX_REQUESTS) {
            val posts = JSONArray(http.getText(url, headers))
            if (posts.length() == 0) throw NoImageException("No Danbooru posts match these tags")
            firstSuitable(posts, Blacklist.parse(settings.blacklistTags))?.let { return it }
        }
        throw NoImageException("No displayable Danbooru image found for these tags")
    }
}
