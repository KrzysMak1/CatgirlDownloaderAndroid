package moe.nyarchlinux.catgirldownloader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import moe.nyarchlinux.catgirldownloader.R
import moe.nyarchlinux.catgirldownloader.api.DanbooruSource
import moe.nyarchlinux.catgirldownloader.api.TagInput
import moe.nyarchlinux.catgirldownloader.api.TagSuggestion
import java.text.NumberFormat

/** Wait this long after the last keystroke before asking Danbooru for suggestions. */
private const val SUGGESTION_DELAY_MS = 300L

/**
 * Danbooru search tags with autocomplete, help text and a tag counter
 * (port of upstream PR #23).
 */
@Composable
fun DanbooruSettingsDialog(
    initialTags: String,
    suggest: suspend (String) -> List<TagSuggestion>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialTags, TextRange(initialTags.length)))
    }
    var suggestions by remember { mutableStateOf(emptyList<TagSuggestion>()) }
    val word = TagInput.wordAt(field.text, field.selection.start)

    LaunchedEffect(word) {
        if (word.isEmpty()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(SUGGESTION_DELAY_MS) // restarted on every keystroke, so this debounces typing
        suggestions = suggest(word)
    }

    val count = TagInput.count(field.text)
    val tooMany = count > DanbooruSource.MAX_ANONYMOUS_TAGS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.danbooru_settings)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.danbooru_tags_help),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = field,
                    onValueChange = { field = it },
                    label = { Text(stringResource(R.string.danbooru_tags_title)) },
                    placeholder = { Text(stringResource(R.string.danbooru_tags_placeholder)) },
                    singleLine = true,
                    isError = tooMany,
                    supportingText = {
                        Row(Modifier.fillMaxWidth()) {
                            if (tooMany) {
                                Text(stringResource(R.string.danbooru_too_many_tags), modifier = Modifier.weight(1f))
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            Text(stringResource(R.string.danbooru_tag_counter, count, DanbooruSource.MAX_ANONYMOUS_TAGS))
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (suggestions.isNotEmpty()) {
                    SuggestionList(suggestions) { tag ->
                        val (text, cursor) = TagInput.apply(field.text, field.selection.start, tag)
                        field = TextFieldValue(text, TextRange(cursor))
                        suggestions = emptyList()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(field.text) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SuggestionList(suggestions: List<TagSuggestion>, onPick: (String) -> Unit) {
    val numbers = remember { NumberFormat.getIntegerInstance() }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column {
            suggestions.forEachIndexed { index, suggestion ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(suggestion.name) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        suggestion.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        pluralStringResource(
                            R.plurals.image_count,
                            suggestion.postCount,
                            numbers.format(suggestion.postCount),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Global tag blacklist (port of upstream PR #18). */
@Composable
fun BlacklistDialog(initialTags: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var tags by rememberSaveable { mutableStateOf(initialTags) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_blacklist)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.blacklist_help), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    placeholder = { Text(stringResource(R.string.blacklist_placeholder)) },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(tags) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Shown when a tag is both searched for and blacklisted, so nothing could ever be found. */
@Composable
fun TagConflictDialog(tags: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tag_conflict_title)) },
        text = { Text(stringResource(R.string.tag_conflict_body, tags)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}
