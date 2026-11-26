package moe.nyarchlinux.catgirldownloader.api

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import moe.nyarchlinux.catgirldownloader.R

/** Port of `CatgirldownloaderWindow.AVAILABLE_SOURCES`. */
data class SourceEntry(
    val source: ImageSource,
    @StringRes val name: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int?,
    val hasSettings: Boolean = false,
) {
    val id: String get() = source.id
}

object Sources {
    val all: List<SourceEntry> = listOf(
        SourceEntry(NekosMoeSource, R.string.source_catgirl, R.string.source_catgirl_desc, R.drawable.source_catgirl),
        SourceEntry(WaifuSource, R.string.source_waifu, R.string.source_waifu_desc, R.drawable.source_waifu),
        SourceEntry(
            DanbooruSource, R.string.source_danbooru, R.string.source_danbooru_desc, R.drawable.source_danbooru,
            hasSettings = true,
        ),
        SourceEntry(KitsuneSource, R.string.source_kitsune, R.string.source_kitsune_desc, icon = null),
    )

    fun byId(id: String?): SourceEntry = all.firstOrNull { it.id == id } ?: all.first()
}
