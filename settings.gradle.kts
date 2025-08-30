import java.net.URL
import java.nio.file.Files

val m8testGradlePath: String by settings
// 目标文件路径
val targetFile = rootDir.resolve(m8testGradlePath)

// 如果文件不存在则下载
if (!targetFile.exists()) {
    targetFile.parentFile.mkdirs()
    println("Downloading file to $targetFile")
    try {
        val version = "0.1.14"
        val url = URL("https://github.com/m8test/development-environment/releases/download/$version/com.m8test.gradle.plugin-$version.jar")
        url.openStream().use { input -> Files.copy(input, targetFile.toPath()) }
        println("Download complete.")
    } catch (e: Exception) {
        throw GradleException("Failed to download file: ${e.message}", e)
    }
} else {
    println("File already exists at $targetFile")
}

rootProject.name = "m8test-idea-plugin"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

