package moe.nyarchlinux.catgirldownloader.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import moe.nyarchlinux.catgirldownloader.MainViewModel
import moe.nyarchlinux.catgirldownloader.R
import moe.nyarchlinux.catgirldownloader.data.NsfwMode
import moe.nyarchlinux.catgirldownloader.data.Settings

private val NsfwMode.label: Int
    get() = when (this) {
        NsfwMode.SHOW_EVERYTHING -> R.string.nsfw_show_everything
        NsfwMode.ONLY_NSFW -> R.string.nsfw_only
        NsfwMode.BLOCK_NSFW -> R.string.nsfw_block
    }

/** Port of the desktop `PreferencesWindow`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    vm: MainViewModel,
    settings: Settings,
    onBack: () -> Unit,
    onEditDanbooruTags: () -> Unit,
) {
    var nsfwDialog by rememberSaveable { mutableStateOf(false) }
    var intervalDialog by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.setDownloadFolder(uri)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.preferences)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            GroupHeader(R.string.pref_group_content)
            ListItem(
                modifier = Modifier.clickable { nsfwDialog = true },
                leadingContent = { Icon(Icons.Outlined.Shield, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.pref_nsfw)) },
                supportingContent = { Text(stringResource(settings.nsfwMode.label)) },
            )
            ListItem(
                modifier = Modifier.clickable(onClick = onEditDanbooruTags),
                leadingContent = { Icon(Icons.Outlined.Sell, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.pref_danbooru_tags)) },
                supportingContent = {
                    Text(settings.danbooruTags.ifBlank { stringResource(R.string.pref_danbooru_tags_none) })
                },
            )

            ListItem(
                modifier = Modifier.clickable { vm.setPreloadNext(!settings.preloadNext) },
                leadingContent = { Icon(Icons.Outlined.Speed, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.pref_preload)) },
                supportingContent = { Text(stringResource(R.string.pref_preload_desc)) },
                trailingContent = {
                    Switch(checked = settings.preloadNext, onCheckedChange = vm::setPreloadNext)
                },
            )

            ListItem(
                modifier = Modifier.clickable { vm.setOriginalQuality(!settings.originalQuality) },
                leadingContent = { Icon(Icons.Outlined.HighQuality, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.pref_original_quality)) },
                supportingContent = { Text(stringResource(R.string.pref_original_quality_desc)) },
                trailingContent = {
                    Switch(checked = settings.originalQuality, onCheckedChange = vm::setOriginalQuality)
                },
            )

            GroupHeader(R.string.pref_group_reload)
            ListItem(
                modifier = Modifier.clickable { intervalDialog = true },
                leadingContent = { Icon(Icons.Outlined.Timer, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.pref_auto_reload_interval)) },
                supportingContent = {
                    Text(
                        pluralStringResource(
                            R.plurals.seconds,
                            settings.autoReloadIntervalSeconds,
                            settings.autoReloadIntervalSeconds,
                        ),
                    )
                },
            )

            GroupHeader(R.string.pref_group_saving)
            ListItem(
                modifier = Modifier.clickable { pickFolder.launch(null) },
                leadingContent = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.pref_download_folder)) },
                supportingContent = {
                    val label = remember(settings.downloadTreeUri) { vm.downloadFolderLabel(settings) }
                    Text(label)
                },
                trailingContent = if (vm.hasCustomDownloadFolder(settings)) {
                    {
                        IconButton(onClick = { vm.setDownloadFolder(null) }) {
                            Icon(
                                Icons.Filled.RestartAlt,
                                contentDescription = stringResource(R.string.pref_download_folder_reset),
                            )
                        }
                    }
                } else {
                    null
                },
            )
            ListItem(
                modifier = Modifier.clickable { vm.setAutoSave(!settings.autoSaveEnabled) },
                leadingContent = { Icon(Icons.Outlined.SaveAlt, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.pref_auto_save)) },
                supportingContent = { Text(stringResource(R.string.pref_auto_save_desc)) },
                trailingContent = {
                    Switch(checked = settings.autoSaveEnabled, onCheckedChange = vm::setAutoSave)
                },
            )
        }
    }

    if (nsfwDialog) {
        AlertDialog(
            onDismissRequest = { nsfwDialog = false },
            title = { Text(stringResource(R.string.pref_nsfw)) },
            text = {
                Column(Modifier.selectableGroup()) {
                    NsfwMode.entries.forEach { mode ->
                        ListItem(
                            modifier = Modifier.selectable(
                                selected = mode == settings.nsfwMode,
                                role = Role.RadioButton,
                                onClick = { vm.setNsfwMode(mode); nsfwDialog = false },
                            ),
                            leadingContent = { RadioButton(selected = mode == settings.nsfwMode, onClick = null) },
                            headlineContent = { Text(stringResource(mode.label)) },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { nsfwDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (intervalDialog) {
        IntervalDialog(
            initial = settings.autoReloadIntervalSeconds,
            onDismiss = { intervalDialog = false },
            onConfirm = { vm.setAutoReloadInterval(it); intervalDialog = false },
        )
    }
}

@Composable
private fun GroupHeader(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun IntervalDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial.toString()) }
    val value = text.toIntOrNull()
    val valid = value != null && value in Settings.MIN_INTERVAL..Settings.MAX_INTERVAL
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_auto_reload_interval)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(R.string.interval_hint, Settings.MIN_INTERVAL, Settings.MAX_INTERVAL)) },
                isError = !valid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = valid) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
