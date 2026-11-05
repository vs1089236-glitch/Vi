package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object MediaStorageHelper {
    /**
     * Copies a photo selected via Android's Photo Picker (PickVisualMedia) into
     * app-private internal storage so product photos and task completion proofs
     * remain readable across app restarts without requiring storage permissions.
     */
    fun persistPickedImage(context: Context, sourceUri: Uri, prefix: String = "img"): String? {
        return try {
            val uploadsDir = File(context.filesDir, "uploads").apply { mkdirs() }
            val destFile = File(uploadsDir, "${prefix}_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
