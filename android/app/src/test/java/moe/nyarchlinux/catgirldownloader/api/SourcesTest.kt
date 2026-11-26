package moe.nyarchlinux.catgirldownloader.api

import moe.nyarchlinux.catgirldownloader.data.NsfwMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class SourcesTest {
    private val forbiddenA = String(Base64.getDecoder().decode("c2hvdGE="))
    private val forbiddenB = String(Base64.getDecoder().decode("bG9saQ=="))

    @Test
    fun nekosMoeUrlFollowsNsfwMode() {
        assertEquals("nsfw=false", NekosMoeSource.requestUrl(NsfwMode.BLOCK_NSFW).query)
        assertEquals("nsfw=true", NekosMoeSource.requestUrl(NsfwMode.ONLY_NSFW).query)
        assertNull(NekosMoeSource.requestUrl(NsfwMode.SHOW_EVERYTHING).query)
    }

    @Test
    fun nekosMoeParse() {
        val post = NekosMoeSource.parse("""{"images":[{"id":"abc123","artist":"Neko Artist"}]}""")
        assertEquals("https://nekos.moe/image/abc123", post.imageUrl)
        assertEquals("https://nekos.moe/post/abc123", post.link)
        assertEquals("Neko Artist", post.artist)
        assertEquals("nekos.moe_abc123.png", NekosMoeSource.fileName(post, "png"))
        assertEquals("nekos.moe_abc123", NekosMoeSource.fileName(post, null))
    }

    @Test
    fun waifuUrlAndParse() {
        assertEquals("IsNsfw=All", WaifuSource.requestUrl(NsfwMode.SHOW_EVERYTHING).query)
        assertEquals("IsNsfw=True", WaifuSource.requestUrl(NsfwMode.ONLY_NSFW).query)
        assertEquals("IsNsfw=False", WaifuSource.requestUrl(NsfwMode.BLOCK_NSFW).query)
        val post = WaifuSource.parse(
            """{"items":[{"id":42,"url":"https://cdn.example/x.jpg","artists":[{"name":"A"}],"source":null}]}""",
        )
        assertEquals("https://cdn.example/x.jpg", post.imageUrl)
        assertEquals("42", post.id)
        assertEquals("A", post.artist)
        assertNull(post.link)
        assertEquals("waifu.im_42.jpg", WaifuSource.fileName(post, "jpg"))
    }

    @Test
    fun kitsuneParse() {
        val post = KitsuneSource.parse(
            """{"results":[{"url":"https://nekos.best/api/v2/kitsune/0042.png","artist_name":"Fox","source_url":"https://src"}]}""",
        )
        assertEquals("0042", post.id)
        assertEquals("Fox", post.artist)
        assertEquals("https://src", post.link)
        assertEquals("nekos.best_0042.png", KitsuneSource.fileName(post, "png"))
    }

    @Test
    fun danbooruTagsQuery() {
        assertEquals("rating:general", DanbooruSource.buildTagsQuery("", NsfwMode.BLOCK_NSFW))
        assertEquals("cat_ears rating:explicit", DanbooruSource.buildTagsQuery(" cat_ears ", NsfwMode.ONLY_NSFW))
        assertEquals("cat_ears solo", DanbooruSource.buildTagsQuery("cat_ears solo", NsfwMode.SHOW_EVERYTHING))
        assertEquals("", DanbooruSource.buildTagsQuery("", NsfwMode.SHOW_EVERYTHING))
        val url = DanbooruSource.requestUrl("cat_ears", NsfwMode.BLOCK_NSFW)
        assertEquals("cat_ears rating:general", url.queryParameter("tags"))
        assertEquals("1", url.queryParameter("limit"))
        assertEquals("true", url.queryParameter("random"))
        assertNull(DanbooruSource.requestUrl("", NsfwMode.SHOW_EVERYTHING).queryParameter("tags"))
    }

    @Test
    fun danbooruSanitizesForbiddenTags() {
        assertEquals("cat_ears solo", DanbooruTags.sanitize("Cat_Ears  $forbiddenA solo ${forbiddenB.uppercase()}"))
        assertTrue(DanbooruTags.containsForbidden("solo $forbiddenB"))
        assertFalse(DanbooruTags.containsForbidden("solo cat_ears"))
    }

    @Test
    fun danbooruSkipsUnsuitablePosts() {
        fun post(tags: String, ext: String = "png", url: String? = "https://cdn/x.png") = JSONObject()
            .put("id", 7)
            .put("tag_string", tags)
            .put("tag_string_artist", "artist_one artist_two")
            .put("file_ext", ext)
            .apply { if (url != null) put("file_url", url) }

        val ok = DanbooruSource.parsePost(post("1girl cat_ears"))!!
        assertEquals("artist_one", ok.artist)
        assertEquals("https://danbooru.donmai.us/posts/7", ok.link)
        assertEquals("danbooru_7.png", DanbooruSource.fileName(ok, "png"))

        assertNull(ok.fullImageUrl)

        // A smaller sample is shown; the original is kept for saving.
        val sampled = DanbooruSource.parsePost(
            post("1girl").put("large_file_url", "https://cdn/sample/sample-x.jpg"),
        )!!
        assertEquals("https://cdn/sample/sample-x.jpg", sampled.imageUrl)
        assertEquals("https://cdn/x.png", sampled.fullImageUrl)
        assertEquals("png", sampled.fullImageExtension)
        // Sample equal to the original (small images) means there is nothing extra to fetch.
        val same = DanbooruSource.parsePost(post("1girl").put("large_file_url", "https://cdn/x.png"))!!
        assertEquals("https://cdn/x.png", same.imageUrl)
        assertNull(same.fullImageUrl)

        assertNull(DanbooruSource.parsePost(post("1girl $forbiddenA")))
        assertNull(DanbooruSource.parsePost(post("1girl", ext = "mp4")))
        assertNull(DanbooruSource.parsePost(post("1girl", url = null)))
    }

    @Test
    fun unknownSourceFallsBackToFirst() {
        assertEquals("catgirl", Sources.byId("nope").id)
        assertEquals("kitsune", Sources.byId("kitsune").id)
    }
}
