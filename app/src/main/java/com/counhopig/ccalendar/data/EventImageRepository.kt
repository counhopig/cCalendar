package com.counhopig.ccalendar.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID

class EventImageRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("event_images", Context.MODE_PRIVATE)

    fun getImageRefs(eventId: Long): List<String> {
        return prefs.getString(key(eventId), "")
            .orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { ref -> isContentUri(ref) || File(ref).exists() }
            .distinct()
            .toList()
    }

    fun saveImageRefs(eventId: Long, refs: List<String>) {
        val previousRefs = getImageRefs(eventId)
        val savedRefs = refs.mapNotNull { normalizeImageRef(it) }.distinct()

        prefs.edit()
            .putString(key(eventId), savedRefs.joinToString("\n"))
            .apply()

        previousRefs
            .filterNot { it in savedRefs }
            .forEach { deleteStoredFile(it) }
    }

    fun clearImageRefs(eventId: Long) {
        getImageRefs(eventId).forEach { deleteStoredFile(it) }
        prefs.edit().remove(key(eventId)).apply()
    }

    private fun normalizeImageRef(ref: String): String? {
        val trimmed = ref.trim()
        if (trimmed.isBlank()) return null

        return when {
            isContentUri(trimmed) || isFileUri(trimmed) -> copyImageToInternalStorage(context, Uri.parse(trimmed))
            File(trimmed).exists() -> trimmed
            else -> null
        }
    }

    private fun key(eventId: Long): String = eventId.toString()

    private fun deleteStoredFile(ref: String) {
        if (!isContentUri(ref) && !isFileUri(ref)) {
            runCatching {
                val imageDir = File(context.filesDir, "event_images").canonicalFile
                val targetFile = File(ref).canonicalFile
                if (targetFile.path.startsWith(imageDir.path + File.separator)) {
                    targetFile.delete()
                }
            }
        }
    }

    companion object {
        fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
            return try {
                val imageDir = File(context.filesDir, "event_images").apply { mkdirs() }
                val extension = context.contentResolver.getType(uri)
                    ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                    ?.takeIf { it.isNotBlank() }
                    ?: "jpg"
                val outputFile = File(imageDir, "${UUID.randomUUID()}.$extension")

                val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                inputStream.use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                outputFile.absolutePath.takeIf { outputFile.length() > 0L }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private fun isContentUri(ref: String): Boolean = ref.startsWith("content://", ignoreCase = true)

        private fun isFileUri(ref: String): Boolean = ref.startsWith("file://", ignoreCase = true)
    }
}
