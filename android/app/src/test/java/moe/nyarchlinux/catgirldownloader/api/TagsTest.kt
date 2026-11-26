package moe.nyarchlinux.catgirldownloader.api

import moe.nyarchlinux.catgirldownloader.data.NsfwMode
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class TagsTest {
    private val forbidden = String(Base64.getDecoder().decode("bG9saQ=="))

    // --- Blacklist (PR #18) ---

    @Test
    fun blacklistParsesAndNormalizes() {
        assertEquals(listOf("cat_ears", "red_hair"), Blacklist.parse("  Cat_Ears  -red_hair\ncat_ears "))
        assertEquals(emptyList<String>(), Blacklist.parse("   "))
    }

    @Test
    fun blacklistMatchesSpacesAndUnderscoresAlike() {
        val list = Blacklist.parse("cat_ears glasses")
        assertEquals(listOf("cat_ears"), Blacklist.matches(listOf("Cat Ears", "smile"), list))
        assertEquals(listOf("glasses"), Blacklist.matches(listOf("glasses"), list))
        assertTrue(Blacklist.matches(listOf("smile"), list).isEmpty())
        assertTrue(Blacklist.matches(listOf("glasses"), emptyList()).isEmpty())
    }

    @Test
    fun blacklistConflictsWithSearchTags() {
        assertEquals(listOf("cat_ears"), Blacklist.conflicts("cat_ears solo", "Cat_Ears glasses"))
        // An excluded search tag ("-glasses") does not conflict with blacklisting it.
        assertTrue(Blacklist.conflicts("-glasses", "glasses").isEmpty())
    }

    @Test
    fun nekosMoeSkipsBlacklistedImages() {
        val json = """{"images":[
            {"id":"a","artist":"x","tags":["cat ears","glasses"]},
            {"id":"b","artist":"y","tags":["cat ears","smile"]}]}"""
        assertEquals("b", NekosMoeSource.parse(json, Blacklist.parse("glasses"))?.id)
        assertNull(NekosMoeSource.parse(json, Blacklist.parse("cat_ears")))
        assertEquals("a", NekosMoeSource.parse(json, emptyList())?.id)
        assertEquals("1", NekosMoeSource.requestUrl(NsfwMode.BLOCK_NSFW, count = 1).queryParameter("count"))
        assertEquals("10", NekosMoeSource.requestUrl(NsfwMode.BLOCK_NSFW, count = 10).queryParameter("count"))
    }

    @Test
    fun waifuExcludesAndSkipsBlacklistedTags() {
        val url = WaifuSource.requestUrl(NsfwMode.BLOCK_NSFW, listOf("glasses", "red_hair"))
        assertEquals(listOf("glasses", "red_hair"), url.queryParameterValues("ExcludedTags"))
        val json = """{"items":[{"id":1,"url":"https://x/1.png","tags":[{"name":"glasses"}]}]}"""
        assertNull(WaifuSource.parse(json, Blacklist.parse("glasses")))
        val plain = """{"items":[{"id":2,"url":"https://x/2.png","tags":["smile"]}]}"""
        assertEquals("2", WaifuSource.parse(plain, Blacklist.parse("glasses"))?.id)
    }

    @Test
    fun danbooruSkipsBlacklistedPosts() {
        fun post(id: Int, tags: String) = JSONObject().put("id", id).put("tag_string", tags)
            .put("file_url", "https://cdn/$id.png").put("file_ext", "png")
        val batch = JSONArray().put(post(1, "solo glasses")).put(post(2, "solo smile"))
        assertEquals("2", DanbooruSource.firstSuitable(batch, Blacklist.parse("glasses"))?.id)
        assertEquals("1", DanbooruSource.firstSuitable(batch, emptyList())?.id)
        assertEquals("20", DanbooruSource.requestUrl("", NsfwMode.BLOCK_NSFW).queryParameter("limit"))
    }

    // --- Danbooru tag autocomplete (PR #23) ---

    @Test
    fun suggestionsUrlAndParsing() {
        val url = DanbooruSource.suggestionsUrl("cat_e")
        assertEquals("cat_e*", url.queryParameter("search[name_matches]"))
        assertEquals("count", url.queryParameter("search[order]"))
        val json = """[{"name":"cat_ears","post_count":812345},
            {"name":"${forbidden}_x","post_count":5},{"name":"","post_count":1}]"""
        assertEquals(listOf(TagSuggestion("cat_ears", 812345)), DanbooruSource.parseSuggestions(json))
    }

    @Test
    fun currentWordAndApplyingSuggestions() {
        assertEquals(0 until 5, TagInput.currentWord("cat_e", 5))
        assertEquals(5 until 9, TagInput.currentWord("solo cat_ smile", 8))
        assertEquals("cat_e", TagInput.wordAt("solo cat_e", 10))
        assertEquals("cat", TagInput.wordAt("solo -cat", 9))

        val (text, cursor) = TagInput.apply("solo cat_e", 10, "cat_ears")
        assertEquals("solo cat_ears ", text)
        assertEquals(14, cursor)

        // Keeps the minus of an excluded tag and the text after the word.
        val (text2, cursor2) = TagInput.apply("-gla smile", 4, "glasses")
        assertEquals("-glasses smile", text2)
        assertEquals(9, cursor2)
    }

    @Test
    fun countsTags() {
        assertEquals(0, TagInput.count("   "))
        assertEquals(3, TagInput.count(" a  -b c "))
    }

    @Test
    fun readsServerMessageFromErrors() {
        val url = "https://danbooru.donmai.us/posts.json".toHttpUrl()
        val e = HttpStatusException(422, url, null, serverMessage("""{"success":false,"message":"You cannot search for more than 2 tags at a time"}"""))
        assertEquals("You cannot search for more than 2 tags at a time", describeError(e))
        assertNull(serverMessage("<html>oops</html>"))
        assertFalse(isRetryable(e))
    }
}
