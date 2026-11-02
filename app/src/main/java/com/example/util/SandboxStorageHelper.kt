package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

data class SandboxedFile(
    val name: String,
    val file: File,
    val sizeBytes: Long,
    val extension: String,
    val lastModified: Long
)

object SandboxStorageHelper {

    private const val SANDBOX_DIR_NAME = "study_sandbox"

    fun getSandboxDir(context: Context): File {
        val dir = File(context.filesDir, SANDBOX_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getSandboxedFiles(context: Context): List<SandboxedFile> {
        val dir = getSandboxDir(context)
        val files = dir.listFiles() ?: return emptyList()
        return files.map { f ->
            SandboxedFile(
                name = f.name,
                file = f,
                sizeBytes = f.length(),
                extension = f.extension.lowercase(),
                lastModified = f.lastModified()
            )
        }.sortedByDescending { it.lastModified }
    }

    fun copyUriToSandbox(context: Context, uri: Uri, customName: String? = null): File? {
        return try {
            val resolvedName = customName ?: getFileName(context, uri) ?: "study_file_${System.currentTimeMillis()}"
            val targetFile = File(getSandboxDir(context), resolvedName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveTextToSandbox(context: Context, fileName: String, content: String): File? {
        return try {
            val targetFile = File(getSandboxDir(context), fileName)
            targetFile.writeText(content)
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteSandboxedFile(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    fun purgeSandbox(context: Context): Int {
        val files = getSandboxDir(context).listFiles() ?: return 0
        var count = 0
        files.forEach {
            if (it.delete()) count++
        }
        return count
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name
    }
}
