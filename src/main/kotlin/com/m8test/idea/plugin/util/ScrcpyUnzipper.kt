package  com.m8test.idea.plugin.util

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ScrcpyUnzipper {

    private const val ZIP_RESOURCE_PATH = "/M8Test/scrcpy.zip"
    private val OUTPUT_DIR = File(System.getProperty("user.home"), ".m8test/bin")

    private fun getScrcpyDir(): File? {
        return OUTPUT_DIR.listFiles()?.firstOrNull { it.name.startsWith("scrcpy") && it.isDirectory }
    }

    /**
     * 解压资源中的 scrcpy.zip 到 ~/.m8test/bin
     * 如果已解压过则跳过
     */
    fun ensureUnzipped(): File {
        // 如果已经解压，跳过（你也可以用一个标志文件判断）
        if (OUTPUT_DIR.exists()) {
            val scrcpy = getScrcpyDir()
            if (scrcpy != null) {
                LogUtils.info("Scrcpy already extracted at: ${OUTPUT_DIR.absolutePath}")
                return scrcpy
            }
        }

        // 创建目标目录
        OUTPUT_DIR.mkdirs()

        // 从资源中读取 zip
        val zipStream = javaClass.getResourceAsStream(ZIP_RESOURCE_PATH)
            ?: throw IllegalStateException("scrcpy.zip not found in resources at $ZIP_RESOURCE_PATH")

        LogUtils.info("Extracting scrcpy.zip to ${OUTPUT_DIR.absolutePath}")

        ZipInputStream(zipStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(OUTPUT_DIR, entry.name)

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    // 确保父目录存在
                    newFile.parentFile.mkdirs()

                    // 写入文件
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        LogUtils.info("scrcpy.zip extracted to ${OUTPUT_DIR.absolutePath}")
        return getScrcpyDir()!!
    }
}
