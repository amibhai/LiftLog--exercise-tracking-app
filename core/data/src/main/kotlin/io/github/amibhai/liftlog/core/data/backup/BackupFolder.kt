package io.github.amibhai.liftlog.core.data.backup

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.amibhai.liftlog.core.domain.backup.backupsToDelete
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

private const val JSON_TYPE = "application/json"
private const val ACCESS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

/** A folder chosen through the Storage Access Framework, such as one that syncs to the cloud (SPEC §9). */
internal class BackupFolder @Inject constructor(@ApplicationContext context: Context) {

    private val resolver: ContentResolver = context.contentResolver

    private class Entry(val uri: Uri, val name: String)

    /** Keeps the access across restarts, and gives up the one to [previous]. */
    fun keepAccess(tree: Uri, previous: Uri?) {
        resolver.takePersistableUriPermission(tree, ACCESS)
        if (previous != null && previous != tree) {
            val isHeld = resolver.persistedUriPermissions.any { it.uri == previous }
            if (isHeld) resolver.releasePersistableUriPermission(previous, ACCESS)
        }
    }

    fun nameOf(tree: Uri): String? =
        query(rootOf(tree), DocumentsContract.Document.COLUMN_DISPLAY_NAME) { it.getString(0) }.firstOrNull()

    /** Creates the document [name] and fills it. A document that could not be written is removed again. */
    suspend fun write(tree: Uri, name: String, content: suspend (OutputStream) -> Unit) {
        val document = DocumentsContract.createDocument(resolver, rootOf(tree), JSON_TYPE, name)
            ?: throw IOException("Cannot create $name")
        var isWritten = false
        try {
            val output = resolver.openOutputStream(document) ?: throw IOException("Cannot write $name")
            output.use { content(it) }
            isWritten = true
        } finally {
            if (!isWritten) delete(document)
        }
    }

    /** Removes the oldest backups. */
    fun prune(tree: Uri, keep: Int) {
        val entries = entries(tree)
        val old = backupsToDelete(entries.map { it.name }, keep).toSet()
        entries.filter { it.name in old }.forEach { delete(it.uri) }
    }

    private fun rootOf(tree: Uri): Uri =
        DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

    private fun entries(tree: Uri): List<Entry> {
        val children =
            DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        return query(
            children,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        ) { Entry(DocumentsContract.buildDocumentUriUsingTree(tree, it.getString(0)), it.getString(1).orEmpty()) }
    }

    private fun <T> query(uri: Uri, vararg columns: String, map: (Cursor) -> T): List<T> =
        resolver.query(uri, columns, null, null, null)?.use { cursor ->
            buildList { while (cursor.moveToNext()) add(map(cursor)) }
        }.orEmpty()

    private fun delete(document: Uri) {
        try {
            DocumentsContract.deleteDocument(resolver, document)
        } catch (_: FileNotFoundException) {
            // Gone already.
        }
    }
}
