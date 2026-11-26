package moe.nyarchlinux.catgirldownloader.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import moe.nyarchlinux.catgirldownloader.LoadedImage
import moe.nyarchlinux.catgirldownloader.MainUiState
import moe.nyarchlinux.catgirldownloader.R
import moe.nyarchlinux.catgirldownloader.api.SourceEntry
import moe.nyarchlinux.catgirldownloader.api.Sources
import moe.nyarchlinux.catgirldownloader.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    settings: Settings,
    snackbar: SnackbarHostState,
    onReload: () -> Unit,
    onSave: () -> Unit,
    onSelectSource: (String) -> Unit,
    onOpenSourceSettings: (SourceEntry) -> Unit,
    onAutoReloadChange: (Boolean) -> Unit,
    onOpenPreferences: () -> Unit,
    onImageError: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var showArtInfo by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    SourceSelector(
                        selected = Sources.byId(settings.sourceId),
                        onSelect = onSelectSource,
                        onOpenSettings = onOpenSourceSettings,
                    )
                },
                actions = {
                    IconButton(onClick = { showArtInfo = true }, enabled = state.image != null) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.about_art))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.preferences)) },
                                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                onClick = { menuOpen = false; onOpenPreferences() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about)) },
                                leadingIcon = { Icon(Icons.Outlined.Pets, contentDescription = null) },
                                onClick = { menuOpen = false; showAbout = true },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            ControlBar(
                loading = state.loading,
                canSave = state.image != null,
                autoReload = settings.autoReloadEnabled,
                onAutoReloadChange = onAutoReloadChange,
                onReload = onReload,
                onSave = onSave,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val image = state.image
            if (image != null) {
                ZoomableImage(image = image, onError = onImageError)
            } else if (state.loading) {
                CircularProgressIndicator()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.no_image), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    val image = state.image
    if (showArtInfo && image != null) {
        ArtInfoDialog(post = image.post, onDismiss = { showArtInfo = false })
    }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

/** Bottom controls: auto reload on the left, a large "new image" button in the middle, save on the right. */
@Composable
private fun ControlBar(
    loading: Boolean,
    canSave: Boolean,
    autoReload: Boolean,
    onAutoReloadChange: (Boolean) -> Unit,
    onReload: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabeledControl(stringResource(R.string.auto_reload_short), Modifier.weight(1f)) {
                FilledIconToggleButton(
                    checked = autoReload,
                    onCheckedChange = onAutoReloadChange,
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        if (autoReload) Icons.Filled.Autorenew else Icons.Outlined.Timer,
                        contentDescription = stringResource(R.string.auto_reload),
                    )
                }
            }

            LargeFloatingActionButton(
                onClick = { if (!loading) onReload() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 3.dp,
                    )
                } else {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.new_image),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            LabeledControl(stringResource(R.string.save), Modifier.weight(1f)) {
                FilledTonalIconButton(
                    onClick = onSave,
                    enabled = canSave,
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun LabeledControl(label: String, modifier: Modifier, button: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        button()
        Spacer(Modifier.size(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Replaces the desktop GtkDropDown with icon, name, description and a settings button. */
@Composable
private fun SourceSelector(
    selected: SourceEntry,
    onSelect: (String) -> Unit,
    onOpenSettings: (SourceEntry) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            SourceIcon(selected, size = 28)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(selected.name), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.choose_source))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Sources.all.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider()
                DropdownMenuItem(
                    modifier = Modifier.widthIn(min = 280.dp),
                    leadingIcon = { SourceIcon(entry, size = 36) },
                    text = {
                        Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(entry.name), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(entry.description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    trailingIcon = when {
                        entry.hasSettings -> {
                            {
                                IconButton(onClick = { expanded = false; onOpenSettings(entry) }) {
                                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.source_settings))
                                }
                            }
                        }
                        entry.id == selected.id -> {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        }
                        else -> null
                    },
                    onClick = { expanded = false; onSelect(entry.id) },
                )
            }
        }
    }
}

@Composable
fun SourceIcon(entry: SourceEntry, size: Int) {
    val modifier = Modifier
        .size(size.dp)
        .clip(CircleShape)
    if (entry.icon != null) {
        Image(painterResource(entry.icon), contentDescription = null, modifier = modifier)
    } else {
        // Like Adw.Avatar with initials, used by sources without an icon.
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(entry.name).take(1),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/**
 * The image sized to its own aspect ratio with rounded corners, plus pinch-to-zoom,
 * panning and double-tap zoom over the whole content area.
 */
@Composable
private fun ZoomableImage(image: LoadedImage, onError: () -> Unit) {
    var scale by remember(image.key) { mutableFloatStateOf(1f) }
    var offset by remember(image.key) { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(o: Offset, s: Float): Offset {
        val maxX = size.width * (s - 1f) / 2f
        val maxY = size.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { size = it }
            .pointerInput(image.key) {
                detectTapGestures(onDoubleTap = { tap ->
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.5f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        offset = clamp((center - tap) * (scale - 1f), scale)
                    }
                })
            }
            .pointerInput(image.key) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    offset = if (scale == 1f) Offset.Zero else clamp(offset + pan, scale)
                }
            },
    ) {
        val ratio = image.aspectRatio
        val shape = RoundedCornerShape(if (scale > 1f) 0.dp else 20.dp)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image.bytes)
                .memoryCacheKey(image.key)
                .build(),
            contentDescription = stringResource(R.string.image_description),
            contentScale = ContentScale.Fit,
            onError = { onError() },
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier.fillMaxSize())
                .shadow(6.dp, shape)
                .clip(shape),
        )
    }
}
