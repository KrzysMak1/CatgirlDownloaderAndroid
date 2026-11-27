package moe.nyarchlinux.catgirldownloader.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Port of `types.NSFWOption`. Order matches the desktop dropdown. */
enum class NsfwMode {
    SHOW_EVERYTHING,
    ONLY_NSFW,
    BLOCK_NSFW,
}

/** Port of the desktop `config.json` preferences. */
data class Settings(
    val sourceId: String = DEFAULT_SOURCE,
    val nsfwMode: NsfwMode = NsfwMode.BLOCK_NSFW,
    val autoReloadEnabled: Boolean = false,
    val autoReloadIntervalSeconds: Int = DEFAULT_INTERVAL,
    val danbooruTags: String = "",
    /** Storage Access Framework tree URI chosen by the user, or null for Pictures/CatgirlDownloader. */
    val downloadTreeUri: String? = null,
    val autoSaveEnabled: Boolean = false,
    /** Download the next image in the background so "new image" shows it instantly. */
    val preloadNext: Boolean = true,
    /** Load full-resolution originals instead of lighter previews (only Danbooru has previews). */
    val originalQuality: Boolean = false,
    /** Space-separated tags; images with any of them are skipped (all sources that report tags). */
    val blacklistTags: String = "",
    /** File name (in the app's private files) of a custom background behind the image, or null. */
    val backgroundFile: String? = null,
    /** How strongly the custom background is darkened, 0 (not at all) to [MAX_BACKGROUND_DIM]. */
    val backgroundDim: Float = DEFAULT_BACKGROUND_DIM,
) {
    companion object {
        const val DEFAULT_SOURCE = "catgirl"
        const val DEFAULT_INTERVAL = 5
        const val MIN_INTERVAL = 1
        const val MAX_INTERVAL = 3600
        const val DEFAULT_BACKGROUND_DIM = 0.4f
        const val MAX_BACKGROUND_DIM = 0.9f
    }
}

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    val settings: StateFlow<Settings> = state.asStateFlow()

    fun update(transform: (Settings) -> Settings) {
        synchronized(this) {
            val updated = transform(state.value).let {
                it.copy(
                    autoReloadIntervalSeconds = it.autoReloadIntervalSeconds
                        .coerceIn(Settings.MIN_INTERVAL, Settings.MAX_INTERVAL),
                    backgroundDim = it.backgroundDim.coerceIn(0f, Settings.MAX_BACKGROUND_DIM),
                )
            }
            write(updated)
            state.value = updated
        }
    }

    private fun read(): Settings {
        val defaults = Settings()
        return Settings(
            sourceId = prefs.getString(KEY_SOURCE, null) ?: defaults.sourceId,
            nsfwMode = prefs.getString(KEY_NSFW, null)
                ?.let { name -> NsfwMode.entries.firstOrNull { it.name == name } }
                ?: defaults.nsfwMode,
            autoReloadEnabled = prefs.getBoolean(KEY_AUTO_RELOAD, defaults.autoReloadEnabled),
            autoReloadIntervalSeconds = prefs.getInt(KEY_INTERVAL, defaults.autoReloadIntervalSeconds)
                .coerceIn(Settings.MIN_INTERVAL, Settings.MAX_INTERVAL),
            danbooruTags = prefs.getString(KEY_DANBOORU_TAGS, null) ?: defaults.danbooruTags,
            downloadTreeUri = prefs.getString(KEY_DOWNLOAD_TREE, null),
            autoSaveEnabled = prefs.getBoolean(KEY_AUTO_SAVE, defaults.autoSaveEnabled),
            preloadNext = prefs.getBoolean(KEY_PRELOAD, defaults.preloadNext),
            originalQuality = prefs.getBoolean(KEY_ORIGINAL_QUALITY, defaults.originalQuality),
            blacklistTags = prefs.getString(KEY_BLACKLIST, null) ?: defaults.blacklistTags,
            backgroundFile = prefs.getString(KEY_BACKGROUND, null),
            backgroundDim = prefs.getFloat(KEY_BACKGROUND_DIM, defaults.backgroundDim)
                .coerceIn(0f, Settings.MAX_BACKGROUND_DIM),
        )
    }

    private fun write(s: Settings) {
        prefs.edit()
            .putString(KEY_SOURCE, s.sourceId)
            .putString(KEY_NSFW, s.nsfwMode.name)
            .putBoolean(KEY_AUTO_RELOAD, s.autoReloadEnabled)
            .putInt(KEY_INTERVAL, s.autoReloadIntervalSeconds)
            .putString(KEY_DANBOORU_TAGS, s.danbooruTags)
            .putString(KEY_DOWNLOAD_TREE, s.downloadTreeUri)
            .putBoolean(KEY_AUTO_SAVE, s.autoSaveEnabled)
            .putBoolean(KEY_PRELOAD, s.preloadNext)
            .putBoolean(KEY_ORIGINAL_QUALITY, s.originalQuality)
            .putString(KEY_BLACKLIST, s.blacklistTags)
            .putString(KEY_BACKGROUND, s.backgroundFile)
            .putFloat(KEY_BACKGROUND_DIM, s.backgroundDim)
            .apply()
    }

    private companion object {
        const val KEY_SOURCE = "source"
        const val KEY_NSFW = "nsfw_mode"
        const val KEY_AUTO_RELOAD = "auto_reload_enabled"
        const val KEY_INTERVAL = "auto_reload_interval"
        const val KEY_DANBOORU_TAGS = "danbooru_tags"
        const val KEY_DOWNLOAD_TREE = "download_tree_uri"
        const val KEY_AUTO_SAVE = "auto_save_enabled"
        const val KEY_PRELOAD = "preload_next"
        const val KEY_ORIGINAL_QUALITY = "original_quality"
        const val KEY_BLACKLIST = "blacklist_tags"
        const val KEY_BACKGROUND = "background_file"
        const val KEY_BACKGROUND_DIM = "background_dim"
    }
}
