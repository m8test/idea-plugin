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
        val url = URL("https://github-registry-files.githubusercontent.com/952370499/11dc6680-3027-11f0-826b-4658e9f57c61?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAVCODYLSA53PQK4ZA%2F20250513%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20250513T115033Z&X-Amz-Expires=300&X-Amz-Signature=1e59d2f5b7ac680c832e0c5067ce0f0900961aa9fa491dce62a8cacdd620a102&X-Amz-SignedHeaders=host&response-content-disposition=filename%3Dgradle-plugin-0.1.0.jar&response-content-type=application%2Foctet-stream")
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

