package moe.nyarchlinux.catgirldownloader

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import moe.nyarchlinux.catgirldownloader.api.DanbooruTags
import moe.nyarchlinux.catgirldownloader.api.ImagePost
import moe.nyarchlinux.catgirldownloader.api.NoImageException
import moe.nyarchlinux.catgirldownloader.api.Sources
import moe.nyarchlinux.catgirldownloader.data.NsfwMode
import moe.nyarchlinux.catgirldownloader.data.Settings
import moe.nyarchlinux.catgirldownloader.storage.ImageFormat
import moe.nyarchlinux.catgirldownloader.storage.ImageSaver

class LoadedImage(
    val key: String,
    val sourceId: String,
    val post: ImagePost,
    val bytes: ByteArray,
    val format: ImageFormat,
    /** Width / height of the image, or null if it couldn't be read up front. */
    val aspectRatio: Float?,
) {
    /** Format of the file that gets saved: the full-resolution original when one exists. */
    val saveFormat: ImageFormat
        get() = post.fullImageUrl?.let { ImageFormat.fromExtension(post.fullImageExtension) } ?: format

    val fileName: String get() = Sources.byId(sourceId).source.fileName(post, saveFormat.extension)
}

data class MainUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val image: LoadedImage? = null,
)

data class SaveRequest(val fileName: String, val mimeType: String, val initialUri: Uri?)

sealed interface UiEvent {
    data class Message(@StringRes val text: Int, val arg: String? = null, val retry: Boolean = false) : UiEvent
    data class AskSaveLocation(val request: SaveRequest) : UiEvent
}

/** Port of the loading / auto-reload / saving logic in the desktop `window.py`. */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CatgirlApp
    private val repo = app.settings

    val settings: StateFlow<Settings> = repo.settings

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null
    private var loadToken = 0
    private var autoReloadJob: Job? = null
    private var inForeground = false
    private var loadCounter = 0L

    /** Next image downloaded in the background, valid only for the settings it was fetched with. */
    private var prefetch: Prefetch? = null

    /** Image the "save as" picker was opened for (auto reload may replace the shown one meanwhile). */
    private var pendingSave: LoadedImage? = null

    private class Prefetch(val key: PrefetchKey, val image: Deferred<LoadedImage>)

    private data class PrefetchKey(val sourceId: String, val nsfw: NsfwMode, val tags: String)

    private fun Settings.prefetchKey() = PrefetchKey(sourceId, nsfwMode, danbooruTags)

    init {
        // Re-arm the auto-reload timer whenever the switch or interval changes (from any screen).
        viewModelScope.launch {
            repo.settings
                .map { it.autoReloadEnabled to it.autoReloadIntervalSeconds }
                .distinctUntilChanged()
                .drop(1)
                .collect { (enabled, _) ->
                    if (enabled && !_state.value.loading) scheduleAutoReload() else if (!enabled) cancelAutoReload()
                }
        }
        // Drop the preloaded image as soon as it no longer matches the source / filters.
        viewModelScope.launch {
            repo.settings
                .map { it.prefetchKey() to it.preloadNext }
                .distinctUntilChanged()
                .drop(1)
                .collect { (key, enabled) ->
                    val current = prefetch
                    if (current != null && (!enabled || current.key != key)) {
                        current.image.cancel()
                        prefetch = null
                    }
                    if (enabled && prefetch == null && !_state.value.loading && _state.value.image != null) {
                        startPrefetch()
                    }
                }
        }
        reload()
    }

    /** Desktop `async_reloadimage`. Ignored while a load runs unless [force] is set. */
    fun reload(force: Boolean = false) {
        if (_state.value.loading && !force) return
        cancelAutoReload()
        loadJob?.cancel()
        val token = ++loadToken
        _state.update { it.copy(loading = true) }
        loadJob = viewModelScope.launch {
            try {
                val settings = repo.settings.value
                val image = takePrefetched(settings) ?: fetchImage(settings)
                _state.update { it.copy(image = image) }
                startPrefetch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Image loading failed", e)
                _events.send(UiEvent.Message(R.string.load_failed, retry = true))
            } finally {
                if (token == loadToken) {
                    _state.update { it.copy(loading = false) }
                    scheduleAutoReload()
                }
            }
        }
    }

    /** Returns the preloaded image if it matches [settings] and downloaded successfully. */
    private suspend fun takePrefetched(settings: Settings): LoadedImage? {
        val current = prefetch ?: return null
        prefetch = null
        if (current.key != settings.prefetchKey()) {
            current.image.cancel()
            return null
        }
        return try {
            current.image.await()
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            null
        } catch (e: Exception) {
            Log.w(TAG, "Preloading failed, fetching again", e)
            null
        }
    }

    private fun startPrefetch() {
        prefetch?.image?.cancel()
        prefetch = null
        val settings = repo.settings.value
        if (!settings.preloadNext) return
        prefetch = Prefetch(settings.prefetchKey(), viewModelScope.async { fetchImage(settings) })
    }

    private suspend fun fetchImage(settings: Settings): LoadedImage {
        val entry = Sources.byId(settings.sourceId)
        val post = entry.source.fetchPost(app.http, settings)
        val download = app.http.getBytes(post.imageUrl.toHttpUrl(), entry.source.imageHeaders)
        val format = ImageFormat.detect(download.bytes, download.contentType, post.imageUrl)
            ?: throw NoImageException("Unsupported image format at ${post.imageUrl}")
        return LoadedImage(
            "${entry.id}:${post.id}:${loadCounter++}", entry.id, post, download.bytes, format,
            aspectRatio(download.bytes),
        )
    }

    private fun aspectRatio(bytes: ByteArray): Float? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return if (options.outWidth > 0 && options.outHeight > 0) {
            options.outWidth.toFloat() / options.outHeight
        } else {
            null
        }
    }

    fun onImageDisplayFailed() {
        _events.trySend(UiEvent.Message(R.string.display_failed, retry = true))
    }

    fun selectSource(id: String) {
        if (id == repo.settings.value.sourceId) return
        repo.update { it.copy(sourceId = id) }
        reload(force = true)
    }

    fun setAutoReload(enabled: Boolean) = repo.update { it.copy(autoReloadEnabled = enabled) }

    fun setAutoReloadInterval(seconds: Int) = repo.update { it.copy(autoReloadIntervalSeconds = seconds) }

    fun setNsfwMode(mode: NsfwMode) = repo.update { it.copy(nsfwMode = mode) }

    fun setAutoSave(enabled: Boolean) = repo.update { it.copy(autoSaveEnabled = enabled) }

    fun setPreloadNext(enabled: Boolean) = repo.update { it.copy(preloadNext = enabled) }

    /** Stores sanitized tags; returns true if forbidden tags had to be removed. */
    fun setDanbooruTags(raw: String): Boolean {
        val sanitized = DanbooruTags.sanitize(raw)
        val changed = sanitized != repo.settings.value.danbooruTags
        repo.update { it.copy(danbooruTags = sanitized) }
        if (changed && repo.settings.value.sourceId == "danbooru") reload(force = true)
        return DanbooruTags.containsForbidden(raw)
    }

    fun setDownloadFolder(treeUri: Uri?) {
        val context = getApplication<Application>()
        val previous = repo.settings.value.downloadTreeUri?.let(Uri::parse)
        if (treeUri != null) ImageSaver.takeTreeAccess(context, treeUri)
        if (previous != null && previous != treeUri) ImageSaver.releaseTreeAccess(context, previous)
        repo.update { it.copy(downloadTreeUri = treeUri?.toString()) }
    }

    /** Label for the configured folder, falling back to the default when access was revoked. */
    fun downloadFolderLabel(settings: Settings): String {
        val context = getApplication<Application>()
        val tree = settings.downloadTreeUri?.let(Uri::parse)
        return if (tree != null && ImageSaver.hasTreeAccess(context, tree)) {
            ImageSaver.treeLabel(context, tree)
        } else {
            ImageSaver.defaultFolderLabel
        }
    }

    fun hasCustomDownloadFolder(settings: Settings): Boolean = settings.downloadTreeUri != null

    /** Desktop `on_save_clicked`: save straight away with auto-save, otherwise ask where. */
    fun onSaveClicked() {
        val image = _state.value.image ?: return
        val settings = repo.settings.value
        val tree = settings.downloadTreeUri?.let(Uri::parse)
            ?.takeIf { ImageSaver.hasTreeAccess(getApplication(), it) }
        if (settings.autoSaveEnabled) {
            if (_state.value.saving) return
            viewModelScope.launch {
                _state.update { it.copy(saving = true) }
                val result = runCatching {
                    val (bytes, format) = bytesForSaving(image)
                    val name = Sources.byId(image.sourceId).source.fileName(image.post, format.extension)
                    ImageSaver.saveToFolder(getApplication(), tree, name, format.mimeType, bytes)
                }
                _state.update { it.copy(saving = false) }
                _events.send(
                    result.fold(
                        onSuccess = { UiEvent.Message(R.string.saved_to, it) },
                        onFailure = {
                            Log.w(TAG, "Saving failed", it)
                            UiEvent.Message(R.string.save_failed)
                        },
                    ),
                )
            }
        } else {
            pendingSave = image
            val request = SaveRequest(image.fileName, image.saveFormat.mimeType, tree?.let(ImageSaver::treeDocumentUri))
            _events.trySend(UiEvent.AskSaveLocation(request))
        }
    }

    fun saveToUri(uri: Uri) {
        val image = pendingSave ?: _state.value.image ?: return
        pendingSave = null
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val result = runCatching { ImageSaver.writeToUri(getApplication(), uri, bytesForSaving(image).first) }
            _state.update { it.copy(saving = false) }
            result.exceptionOrNull()?.let { Log.w(TAG, "Saving failed", it) }
            _events.send(UiEvent.Message(if (result.isSuccess) R.string.saved else R.string.save_failed))
        }
    }

    /**
     * Bytes to save: the full-resolution original when only a preview was shown,
     * falling back to the shown preview if the original can't be downloaded.
     */
    private suspend fun bytesForSaving(image: LoadedImage): Pair<ByteArray, ImageFormat> {
        val fullUrl = image.post.fullImageUrl ?: return image.bytes to image.format
        return try {
            val download = app.http.getBytes(fullUrl.toHttpUrl(), Sources.byId(image.sourceId).source.imageHeaders)
            val format = ImageFormat.detect(download.bytes, download.contentType, fullUrl) ?: image.saveFormat
            download.bytes to format
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Original download failed, saving the preview", e)
            image.bytes to image.format
        }
    }

    /** Auto reload only runs while the app is visible, so it doesn't use data in the background. */
    fun setForeground(foreground: Boolean) {
        inForeground = foreground
        if (foreground) {
            if (!_state.value.loading) scheduleAutoReload()
        } else {
            cancelAutoReload()
        }
    }

    private fun scheduleAutoReload() {
        cancelAutoReload()
        val settings = repo.settings.value
        if (!settings.autoReloadEnabled || !inForeground) return
        autoReloadJob = viewModelScope.launch {
            delay(settings.autoReloadIntervalSeconds * 1000L)
            reload()
        }
    }

    private fun cancelAutoReload() {
        autoReloadJob?.cancel()
        autoReloadJob = null
    }

    private companion object {
        const val TAG = "CatgirlDownloader"
    }
}
