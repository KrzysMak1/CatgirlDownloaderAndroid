package moe.nyarchlinux.catgirldownloader.ui

import androidx.compose.foundation.Image
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

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(painterResource(R.drawable.source_catgirl), contentDescription = null, modifier = Modifier.size(72.dp))
        },
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.about_body),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
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
