package moe.nyarchlinux.catgirldownloader.api

/**
 * Tag blacklist shared by all sources (port of upstream PR #18). Tags are compared
 * case-insensitively, with spaces and underscores treated alike, because nekos.moe uses
 * "cat ears" while Danbooru uses "cat_ears".
 */
object Blacklist {
    private val whitespace = Regex("\\s+")

    fun normalize(tag: String): String = tag.trim().lowercase().replace(' ', '_')

    /** Space-separated user input → distinct normalized tags (a leading "-" is ignored). */
    fun parse(raw: String): List<String> =
        raw.split(whitespace)
            .map { normalize(it.removePrefix("-")) }
            .filter { it.isNotEmpty() }
            .distinct()

    /** Blacklisted tags found among [postTags]. */
    fun matches(postTags: Collection<String>, blacklist: List<String>): List<String> {
        if (blacklist.isEmpty()) return emptyList()
        val normalized = postTags.mapTo(HashSet()) { normalize(it) }
        return blacklist.filter { it in normalized }
    }

    /** Search tags that are also blacklisted, so the search can never find anything. */
    fun conflicts(searchTags: String, blacklistRaw: String): List<String> {
        val search = searchTags.split(whitespace)
            .filter { it.isNotEmpty() && !it.startsWith("-") }
            .map { normalize(it) }
            .toSet()
        return parse(blacklistRaw).filter { it in search }
    }
}

/** A Danbooru tag suggested while typing (port of upstream PR #23). */
data class TagSuggestion(val name: String, val postCount: Int)

/** Editing helpers for the space-separated tag field. */
object TagInput {
    /** Character range of the word under [cursor]. */
    fun currentWord(text: String, cursor: Int): IntRange {
        val pos = cursor.coerceIn(0, text.length)
        val start = text.lastIndexOf(' ', pos - 1) + 1
        val end = text.indexOf(' ', pos).let { if (it == -1) text.length else it }
        return start until end
    }

    /** The word under the cursor, lower-cased and without an exclusion "-". */
    fun wordAt(text: String, cursor: Int): String =
        text.substring(currentWord(text, cursor)).lowercase().removePrefix("-")

    /** Replaces the word under the cursor with [tag] (keeping a "-"); returns new text and cursor. */
    fun apply(text: String, cursor: Int, tag: String): Pair<String, Int> {
        val range = currentWord(text, cursor)
        val prefix = if (text.substring(range).startsWith("-")) "-" else ""
        val before = text.substring(0, range.first)
        val after = text.substring(range.last + 1).trimStart(' ')
        val inserted = "$prefix$tag "
        return (before + inserted + after) to (before.length + inserted.length)
    }

    fun count(text: String): Int = text.split(' ', '\n', '\t').count { it.isNotEmpty() }
}
