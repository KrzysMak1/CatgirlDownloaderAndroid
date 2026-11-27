package moe.nyarchlinux.catgirldownloader.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.nyarchlinux.catgirldownloader.MainViewModel
import moe.nyarchlinux.catgirldownloader.R
import moe.nyarchlinux.catgirldownloader.SaveRequest
import moe.nyarchlinux.catgirldownloader.UiEvent

/** "Save as" picker that pre-fills name/type and opens in the configured download folder. */
private object CreateImageDocument : ActivityResultContract<SaveRequest, Uri?>() {
    override fun createIntent(context: Context, input: SaveRequest): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.fileName)
            .apply { input.initialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) } }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

@Composable
fun CatgirlDownloaderApp(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showPreferences by rememberSaveable { mutableStateOf(false) }
    var danbooruDialog by rememberSaveable { mutableStateOf(false) }
    var forbiddenDialog by rememberSaveable { mutableStateOf(false) }
    var blacklistDialog by rememberSaveable { mutableStateOf(false) }
    // Comma-separated tags in both the Danbooru search and the blacklist; empty = no warning.
    var conflictTags by rememberSaveable { mutableStateOf("") }

    LifecycleStartEffect(vm) {
        vm.setForeground(true)
        onStopOrDispose { vm.setForeground(false) }
    }

    val saveAs = rememberLauncherForActivityResult(CreateImageDocument) { uri ->
        uri?.let(vm::saveToUri)
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is UiEvent.AskSaveLocation -> saveAs.launch(event.request)
                is UiEvent.Message -> {
                    val text = if (event.arg != null) context.getString(event.text, event.arg)
                    else context.getString(event.text)
                    val result = snackbar.showSnackbar(
                        message = text,
                        actionLabel = if (event.retry) context.getString(R.string.retry) else null,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) vm.reload()
                }
            }
        }
    }

    val background = remember(settings.backgroundFile) { vm.backgroundFile(settings) }

    BackHandler(enabled = showPreferences) { showPreferences = false }

    AnimatedContent(
        targetState = showPreferences,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen",
    ) { prefs ->
        if (prefs) {
            PreferencesScreen(
                vm = vm,
                settings = settings,
                onBack = { showPreferences = false },
                onEditDanbooruTags = { danbooruDialog = true },
                onEditBlacklist = { blacklistDialog = true },
            )
        } else {
            MainScreen(
                state = state,
                settings = settings,
                snackbar = snackbar,
                onReload = { vm.reload() },
                onSave = vm::onSaveClicked,
                onSelectSource = vm::selectSource,
                onOpenSourceSettings = { danbooruDialog = true },
                onAutoReloadChange = vm::setAutoReload,
                onOpenPreferences = { showPreferences = true },
                onImageError = vm::onImageDisplayFailed,
                background = background,
                backgroundDim = settings.backgroundDim,
            )
        }
    }

    if (danbooruDialog) {
        DanbooruSettingsDialog(
            initialTags = settings.danbooruTags,
            suggest = vm::suggestDanbooruTags,
            onDismiss = { danbooruDialog = false },
            onSave = { tags ->
                danbooruDialog = false
                if (vm.setDanbooruTags(tags)) forbiddenDialog = true
                conflictTags = vm.danbooruBlacklistConflicts(vm.settings.value.danbooruTags).joinToString(", ")
            },
        )
    }
    if (blacklistDialog) {
        BlacklistDialog(
            initialTags = settings.blacklistTags,
            onDismiss = { blacklistDialog = false },
            onSave = { tags ->
                blacklistDialog = false
                conflictTags = vm.setBlacklist(tags).joinToString(", ")
            },
        )
    }
    if (conflictTags.isNotEmpty() && !forbiddenDialog) {
        TagConflictDialog(tags = conflictTags, onDismiss = { conflictTags = "" })
    }
    if (forbiddenDialog) {
        ForbiddenTagsDialog(onDismiss = { forbiddenDialog = false })
    }
}
