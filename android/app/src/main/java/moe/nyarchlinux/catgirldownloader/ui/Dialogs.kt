package moe.nyarchlinux.catgirldownloader.ui

import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import moe.nyarchlinux.catgirldownloader.BuildConfig
import moe.nyarchlinux.catgirldownloader.R
import moe.nyarchlinux.catgirldownloader.api.ImagePost

private const val CENSORED_TAGS_HELP = "https://danbooru.donmai.us/wiki_pages/help:censored_tags"

/** Desktop "About Art" window: artist and source link of the current image. */
@Composable
fun ArtInfoDialog(post: ImagePost, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_art)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledValue(stringResource(R.string.artist), post.artist ?: stringResource(R.string.unknown))
                LabeledValue(stringResource(R.string.source_link), post.link ?: stringResource(R.string.unknown))
            }
        },
        confirmButton = {
            val link = post.link
            if (link != null && (link.startsWith("https://") || link.startsWith("http://"))) {
                TextButton(onClick = { runCatching { uriHandler.openUri(link) } }) {
                    Text(stringResource(R.string.open_source))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private const val PORT_REPO = "https://github.com/KrzysMak1/CatgirlDownloaderAndroid"
private const val ORIGINAL_REPO = "https://github.com/NyarchLinux/CatgirlDownloader"

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painterResource(R.drawable.source_catgirl),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.app_name), textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AboutSection(R.string.about_port_title, R.string.about_port_body)
                AboutSection(R.string.about_original_title, R.string.about_original_body)
                AboutSection(R.string.about_images_title, R.string.about_images_body)
                Text(
                    stringResource(R.string.about_license),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { runCatching { uriHandler.openUri(PORT_REPO) } }) {
                        Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.about_port_source))
                    }
                    TextButton(onClick = { runCatching { uriHandler.openUri(ORIGINAL_REPO) } }) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.about_original_source))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun AboutSection(title: Int, body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Desktop Danbooru preferences window: free-text tag search. */
@Composable
fun DanbooruSettingsDialog(initialTags: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var tags by rememberSaveable { mutableStateOf(initialTags) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.danbooru_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.danbooru_tags_desc), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(stringResource(R.string.danbooru_tags_title)) },
                    placeholder = { Text(stringResource(R.string.danbooru_tags_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(tags) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ForbiddenTagsDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.danbooru_settings)) },
        text = { Text(stringResource(R.string.danbooru_forbidden_body)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
        dismissButton = {
            TextButton(onClick = { runCatching { uriHandler.openUri(CENSORED_TAGS_HELP) } }) {
                Text(stringResource(R.string.danbooru_forbidden_link))
            }
        },
    )
}
