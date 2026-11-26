package moe.nyarchlinux.catgirldownloader.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Saving downloaded images. Android 10+ scoped storage replaces the desktop's free file paths:
 * - default folder: Pictures/CatgirlDownloader through MediaStore (no permission needed);
 * - custom folder: a Storage Access Framework tree the user picked in Preferences;
 * - "save as": a URI returned by the system file picker.
 */
object ImageSaver {
    const val DEFAULT_SUBDIR = "CatgirlDownloader"
    val defaultFolderLabel: String get() = "${Environment.DIRECTORY_PICTURES}/$DEFAULT_SUBDIR"

    /** Returns the saved file's display name. */
    suspend fun saveToFolder(
        context: Context,
        treeUri: Uri?,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): String = withContext(Dispatchers.IO) {
        val tree = treeUri
            ?.takeIf { hasTreeAccess(context, it) }
            ?.let { DocumentFile.fromTreeUri(context, it) }
            ?.takeIf { it.isDirectory && it.canWrite() }
        if (tree != null) saveToTree(context, tree, fileName, mimeType, bytes)
        else saveToMediaStore(context, fileName, mimeType, bytes)
    }

    suspend fun writeToUri(context: Context, uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Cannot open $uri")
        stream.use { it.write(bytes) }
    }

    fun hasTreeAccess(context: Context, treeUri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isWritePermission && it.isReadPermission
        }

    fun takeTreeAccess(context: Context, treeUri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    fun releaseTreeAccess(context: Context, treeUri: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    /** Human-readable label for a picked tree, e.g. `Pictures/Neko` or `Download`. */
    fun treeLabel(context: Context, treeUri: Uri): String {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
        val path = docId?.substringAfter(':', "")?.takeIf { it.isNotEmpty() }
        return path ?: DocumentFile.fromTreeUri(context, treeUri)?.name ?: treeUri.toString()
    }

    /** Document URI of the tree root, used to open the "save as" picker in that folder. */
    fun treeDocumentUri(treeUri: Uri): Uri? = runCatching {
        DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
    }.getOrNull()

    /** Desktop `unique_path`: `name.png`, then `name-1.png`, `name-2.png`, ... */
    fun uniqueName(fileName: String, exists: (String) -> Boolean): String {
        if (!exists(fileName)) return fileName
        val dot = fileName.lastIndexOf('.')
        val stem = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        return generateSequence(1) { it + 1 }.map { "$stem-$it$ext" }.first { !exists(it) }
    }

    private fun saveToTree(
        context: Context,
        dir: DocumentFile,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): String {
        val existing = dir.listFiles().mapNotNull { it.name }.toSet()
        val name = uniqueName(fileName) { it in existing }
        val file = dir.createFile(mimeType, name) ?: throw IOException("Cannot create $name")
        try {
            val stream = context.contentResolver.openOutputStream(file.uri)
                ?: throw IOException("Cannot open ${file.uri}")
            stream.use { it.write(bytes) }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        return file.name ?: name
    }

    private fun saveToMediaStore(context: Context, fileName: String, mimeType: String, bytes: ByteArray): String {
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$DEFAULT_SUBDIR")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore insert failed")
        try {
            val stream = resolver.openOutputStream(uri) ?: throw IOException("Cannot open $uri")
            stream.use { it.write(bytes) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        // MediaStore renames duplicates (e.g. "name (1).png"), so report the name it actually used.
        return resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: fileName
    }
}
