package com.example.filemanager

import java.io.File

class FileRepository {

    fun listFiles(dir: File, sortType: SortType, query: String?): List<File> {
        var files = dir.listFiles()?.toList() ?: emptyList()
        if (!query.isNullOrBlank()) {
            files = files.filter { it.name.contains(query, ignoreCase = true) }
        }
        return when (sortType) {
            SortType.NAME -> files.sortedWith(
                compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() }
            )
            SortType.SIZE -> files.sortedWith(
                compareByDescending<File> { it.isDirectory }.thenByDescending { it.length() }
            )
            SortType.DATE -> files.sortedWith(
                compareByDescending<File> { it.isDirectory }.thenByDescending { it.lastModified() }
            )
        }
    }

    fun createFolder(parent: File, name: String): Boolean = File(parent, name).mkdir()

    fun rename(file: File, newName: String): Boolean =
        file.renameTo(File(file.parentFile, newName))

    fun delete(file: File): Boolean =
        if (file.isDirectory) file.deleteRecursively() else file.delete()

    fun copy(src: File, destDir: File): Boolean {
        val target = File(destDir, src.name)
        return try {
            if (src.isDirectory) src.copyRecursively(target, overwrite = true)
            else src.copyTo(target, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun move(src: File, destDir: File): Boolean =
        if (copy(src, destDir)) delete(src) else false
}