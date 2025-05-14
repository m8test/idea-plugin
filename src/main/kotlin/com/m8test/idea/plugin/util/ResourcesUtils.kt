package com.m8test.idea.plugin.util

import com.intellij.openapi.progress.ProgressManager
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ResourcesUtils {
    @Throws(IOException::class)
    fun copyResourceToFile(resourcePath: String, targetFile: File) {
        val inputStream: InputStream = ResourcesUtils::class.java.getResourceAsStream(resourcePath)
            ?: throw IOException("资源文件 $resourcePath 未找到")

        val parentDir = targetFile.parentFile
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw IOException("无法创建目标文件的父目录: ${parentDir.absolutePath}")
        }

        inputStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    // 现有方法（解压资源文件）
    @Throws(IOException::class)
    fun unzipFromResources(resourcePath: String, targetDir: File) {
        val inputStream: InputStream = ResourcesUtils::class.java.getResourceAsStream(resourcePath)
            ?: throw IOException("资源文件 $resourcePath 未找到")

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw IOException("无法创建目标目录: ${targetDir.absolutePath}")
        }

        try {
            val totalSize = ResourcesUtils::class.java.getResource(resourcePath)
                ?.openConnection()?.contentLengthLong ?: -1L
            val estimatedEntryCount = if (totalSize > 0) 10 else 1

            ZipInputStream(inputStream).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                var processedEntries = 0

                while (entry != null) {
                    val currentEntry = entry
                    val entryName = currentEntry.name
                    val entryFile = File(targetDir, entryName)

                    ProgressManager.getInstance().runProcessWithProgressSynchronously({
                        val indicator = ProgressManager.getInstance().progressIndicator
                        indicator.text = "正在解压文件 $entryName 到 ${targetDir.name}..."
                        indicator.isIndeterminate = totalSize < 0 || currentEntry.size <= 0

                        if (currentEntry.isDirectory) {
                            if (!entryFile.exists() && !entryFile.mkdirs()) {
                                throw IOException("无法创建目录: ${entryFile.absolutePath}")
                            }
                        } else {
                            val parentDir = entryFile.parentFile
                            if (!parentDir.exists() && !parentDir.mkdirs()) {
                                throw IOException("无法创建父目录: ${parentDir.absolutePath}")
                            }
                            entryFile.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var totalBytes = 0L
                                while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalBytes += bytesRead
                                    if (totalSize > 0 && currentEntry.size > 0) {
                                        indicator.fraction =
                                            (processedEntries.toDouble() + (totalBytes.toDouble() / currentEntry.size)) / estimatedEntryCount
                                    }
                                }
                            }
                        }
                    }, "解压文件 $entryName", true, null)

                    zipIn.closeEntry()
                    processedEntries++
                    entry = zipIn.nextEntry
                }
            }
        } catch (e: IOException) {
            throw IOException("解压 ZIP 文件 $resourcePath 到 ${targetDir.absolutePath} 失败: ${e.message}", e)
        }
    }

    // 新方法：解压文件系统中的 ZIP 文件
    @Throws(IOException::class)
    fun unzipFromFile(zipFile: File, targetDir: File) {
        if (!zipFile.exists() || !zipFile.isFile) {
            throw IOException("ZIP 文件 ${zipFile.absolutePath} 不存在或不是文件")
        }

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw IOException("无法创建目标目录: ${targetDir.absolutePath}")
        }

        try {
            val totalSize = zipFile.length()
            val estimatedEntryCount = if (totalSize > 0) 10 else 1

            ZipInputStream(zipFile.inputStream()).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                var processedEntries = 0

                while (entry != null) {
                    val currentEntry = entry
                    val entryName = currentEntry.name
                    val entryFile = File(targetDir, entryName)

                    ProgressManager.getInstance().runProcessWithProgressSynchronously({
                        val indicator = ProgressManager.getInstance().progressIndicator
                        indicator.text = "正在解压文件 $entryName 到 ${targetDir.name}..."
                        indicator.isIndeterminate = totalSize <= 0 || currentEntry.size <= 0

                        if (currentEntry.isDirectory) {
                            if (!entryFile.exists() && !entryFile.mkdirs()) {
                                throw IOException("无法创建目录: ${entryFile.absolutePath}")
                            }
                        } else {
                            val parentDir = entryFile.parentFile
                            if (!parentDir.exists() && !parentDir.mkdirs()) {
                                throw IOException("无法创建父目录: ${parentDir.absolutePath}")
                            }
                            entryFile.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var totalBytes = 0L
                                while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalBytes += bytesRead
                                    if (totalSize > 0 && currentEntry.size > 0) {
                                        indicator.fraction =
                                            (processedEntries.toDouble() + (totalBytes.toDouble() / currentEntry.size)) / estimatedEntryCount
                                    }
                                }
                            }
                        }
                    }, "解压文件 $entryName", true, null)

                    zipIn.closeEntry()
                    processedEntries++
                    entry = zipIn.nextEntry
                }
            }
        } catch (e: IOException) {
            throw IOException("解压 ZIP 文件 ${zipFile.absolutePath} 到 ${targetDir.absolutePath} 失败: ${e.message}", e)
        }
    }
}