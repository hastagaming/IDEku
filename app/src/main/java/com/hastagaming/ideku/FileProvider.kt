package com.hastagaming.ideku

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

class FileProvider : DocumentsProvider() {

    // Sesuaikan dengan yang ada di AndroidManifest.xml
    private val AUTHORITY = "com.hastagaming.ideku.documents"

    private val DEFAULT_ROOT_PROJECTION: Array<String> = arrayOf(
        DocumentsContract.Root.COLUMN_ROOT_ID,
        DocumentsContract.Root.COLUMN_MIME_TYPES,
        DocumentsContract.Root.COLUMN_FLAGS,
        DocumentsContract.Root.COLUMN_ICON,
        DocumentsContract.Root.COLUMN_TITLE,
        DocumentsContract.Root.COLUMN_SUMMARY,
        DocumentsContract.Root.COLUMN_DOCUMENT_ID
    )

    private val DEFAULT_DOCUMENT_PROJECTION: Array<String> = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_FLAGS,
        DocumentsContract.Document.COLUMN_SIZE
    )

    override fun onCreate(): Boolean = true

    // 1. Mendaftarkan folder 'home' ke sidebar Android
    override fun queryRoots(projection: Array<out String>?): Cursor {
        val flags = DocumentsContract.Root.FLAG_SUPPORTS_CREATE or 
                    DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD or
                    DocumentsContract.Root.FLAG_LOCAL_ONLY

        val matrix = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        val homeDir = File(context?.filesDir, "home")
        
        if (!homeDir.exists()) homeDir.mkdirs()

        matrix.newRow().apply {
            add(DocumentsContract.Root.COLUMN_ROOT_ID, "nasa_ide_root")
            add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, getDocIdForFile(homeDir))
            add(DocumentsContract.Root.COLUMN_TITLE, "Nasa-IDE") // Nama di sidebar
            add(DocumentsContract.Root.COLUMN_SUMMARY, "Internal Isolated Home")
            add(DocumentsContract.Root.COLUMN_FLAGS, flags)
            add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
            add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher)
        }
        return matrix
    }

    // 2. Mengambil informasi detail tentang file/folder tertentu
    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        val matrix = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val file = getFileForDocId(documentId)
        includeFile(matrix, documentId, file)
        return matrix
    }

    // 3. Menampilkan daftar file di dalam folder (saat user mengklik folder)
    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val matrix = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(parentDocumentId)
        
        parent.listFiles()?.forEach { file ->
            includeFile(matrix, null, file)
        }
        return matrix
    }

    // 4. Membuka file agar aplikasi lain (seperti Text Editor eksternal) bisa baca/tulis
    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = getFileForDocId(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(file, accessMode)
    }

    // --- HELPER METHODS ---

    private fun getDocIdForFile(file: File): String {
        return file.absolutePath
    }

    private fun getFileForDocId(documentId: String?): File {
        return File(documentId ?: throw FileNotFoundException("ID Kosong"))
    }

    private fun includeFile(matrix: MatrixCursor, documentId: String?, file: File) {
        val docId = documentId ?: getDocIdForFile(file)
        var flags = 0
        
        if (file.canWrite()) {
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_WRITE or
                    DocumentsContract.Document.FLAG_SUPPORTS_DELETE or
                    DocumentsContract.Document.FLAG_SUPPORTS_RENAME
        }

        val displayName = if (file.absolutePath == File(context?.filesDir, "home").absolutePath) {
            "Nasa-IDE"
        } else {
            file.name
        }

        matrix.newRow().apply {
            add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docId)
            add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName)
            add(DocumentsContract.Document.COLUMN_SIZE, file.length())
            add(DocumentsContract.Document.COLUMN_MIME_TYPE, getMimeType(file))
            add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified())
            add(DocumentsContract.Document.COLUMN_FLAGS, flags)
        }
    }

    private fun getMimeType(file: File): String {
        if (file.isDirectory) return DocumentsContract.Document.MIME_TYPE_DIR
        val lastDot = file.name.lastIndexOf('.')
        if (lastDot >= 0) {
            val extension = file.name.substring(lastDot + 1).lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) return mime
        }
        return "application/octet-stream"
    }
}