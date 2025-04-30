package com.m8test.idea.plugin.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.m8test.idea.plugin.config.M8TestSettings
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object AdbUtils {
    private val adbPath: String by lazy {
        val scrcpyDir = ScrcpyUnzipper.ensureUnzipped()
        File(scrcpyDir, "adb.exe").absolutePath
    }

    private val scrcpyPath: String by lazy {
        val scrcpyDir = ScrcpyUnzipper.ensureUnzipped()
        File(scrcpyDir, "scrcpy.exe").absolutePath
    }

    private val state
        get() = M8TestSettings.instance.state

    private val deviceAddress: String
        get() {
            return "${state.deviceIp}:${state.adbPort}"
        }

    /**
     * 执行 ADB 端口转发。
     * @return Pair(success, message)。
     */
    fun forwardPortAsync(onResult: (Boolean, String) -> Unit) {
        val localPort: Int = state.debugPort
        val remotePort: Int = state.debugPort
        runAdbTask("设置端口转发", "正在设置端口转发...", { indicator ->
            val (connected, msg) = ensureDeviceConnected()
            if (!connected) return@runAdbTask false to msg

            val result = executeAdbCommand("forward", "tcp:$localPort", "tcp:$remotePort", useSerial = true)
            result
        }, onResult)
    }

    /**
     * 执行 ADB 命令并返回结果。
     * @param args 传递给 ADB 的命令参数。
     * @param timeoutSeconds 命令执行的超时时间（秒）。
     * @return Pair(success, output/error message)。
     */
    private fun executeAdbCommand(
        vararg args: String,
        useSerial: Boolean,
        timeoutSeconds: Long = 30
    ): Pair<Boolean, String> {
        val command = mutableListOf(adbPath).apply {
            if (useSerial) {
                add("-s")
                add(deviceAddress)
            }
        } + args
        try {
            val process = ProcessBuilder(command).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }

            val exited = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!exited) {
                process.destroy()
                LogUtils.warn("ADB command timed out: ${command.joinToString(" ")}")
                return false to "ADB 命令超时"
            }

            LogUtils.info("ADB Command for $deviceAddress: ${command.joinToString(" ")}")
            LogUtils.info("ADB Output: $output")
            LogUtils.info("ADB Error: $error")

            return if (process.exitValue() == 0) {
                val lowerOutput = output.lowercase()
                if (lowerOutput.contains("failed") || lowerOutput.contains("unable")) {
                    false to output.trim()
                } else {
                    true to output.trim()
                }
            } else {
                LogUtils.warn("ADB command failed: ${command.joinToString(" ")}\nError: $error")
                false to error.trim().ifEmpty { "ADB 命令执行失败" }
            }
        } catch (e: IOException) {
            LogUtils.error("Failed to execute ADB command: ${command.joinToString(" ")}", e)
            return false to "执行 ADB 命令失败: ${e.message}"
        }
    }

    /**
     * 运行带有进度指示器的后台任务。
     * @param title 任务标题，显示在 UI 中。
     * @param indicatorText 进度指示器文本。
     * @param task 要执行的任务，返回 Pair<Boolean, String>。
     * @param onResult 处理结果的回调，接收 success 和 message。
     */
    private fun runAdbTask(
        title: String,
        indicatorText: String,
        task: (ProgressIndicator) -> Pair<Boolean, String>,
        onResult: (Boolean, String) -> Unit
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(null, title, true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = indicatorText
                val (success, message) = task(indicator)
                ApplicationManager.getApplication().invokeLater {
                    onResult(success, message)
                }
            }
        })
    }

    /**
     * 获取项目根目录，适配非协程环境。
     * @param onResult 回调函数，接收结果 (success, rootPath or error message)。
     */
    private fun getProjectRootAsync(onResult: (Boolean, String) -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val projectRoot = runBlocking { HttpUtils.getProjectRoot() }
                onResult(true, projectRoot)
            } catch (e: Exception) {
                LogUtils.error("Failed to get project root: ${e.message}", e)
                onResult(false, "获取项目根目录失败: ${e.message}")
            }
        }
    }

    fun connectDeviceAsync(onResult: (Boolean, String) -> Unit) {
        runAdbTask("连接 ADB 设备", "正在连接设备...", {
            executeAdbCommand("connect", deviceAddress, useSerial = false)
        }, onResult)
    }

    fun disconnectDeviceAsync(onResult: (Boolean, String) -> Unit) {
        runAdbTask("断开 ADB 设备", "正在断开设备...", {
            executeAdbCommand("disconnect", deviceAddress, useSerial = false)
        }, onResult)
    }

    /**
     * 推送本地项目目录下的所有文件到远程项目根目录。
     * @param localProjectDir 本地项目根目录（例如 C:/myproject）。
     * @param onResult 回调函数，接收结果 (success, message)。
     */
    fun pushDirectoryAsync(localProjectDir: String, onResult: (Boolean, String) -> Unit) {
        runAdbTask("推送项目目录到设备", "正在推送项目目录...", { indicator ->
            val projectDir = File(localProjectDir)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                return@runAdbTask false to "本地项目目录不存在或不是目录: $localProjectDir"
            }

            val (connected, msg) = ensureDeviceConnected()
            if (!connected) return@runAdbTask false to msg

            var result: Pair<Boolean, String>? = null
            val latch = CountDownLatch(1)
            getProjectRootAsync { success, message ->
                if (!success) {
                    result = false to message
                    latch.countDown()
                    return@getProjectRootAsync
                }
                val remoteRoot = message.replace("//", "/")
                try {
                    // 使用 adb push 直接推送整个目录
                    indicator.text = "正在推送目录 $localProjectDir 到 $remoteRoot..."
                    result = executeAdbCommand("push", "$localProjectDir/.", remoteRoot, useSerial = true)
                    if (result?.first == true) {
                        LogUtils.info("Pushed directory $localProjectDir to $remoteRoot on $deviceAddress")
                        result = true to "项目目录推送成功"
                    }
                } catch (e: Exception) {
                    LogUtils.error("Failed to push directory: ${e.message}", e)
                    result = false to "推送项目目录失败: ${e.message}"
                }
                latch.countDown()
            }
            latch.await(30, TimeUnit.SECONDS)
            result ?: (false to "获取项目根目录超时")
        }, onResult)
    }

    private fun reloadProjectFromDisk(project: Project) {
        val projectDir: VirtualFile? = project.baseDir
        if (projectDir != null && projectDir.exists()) {
            VfsUtil.markDirtyAndRefresh(false, true, true, projectDir)
        }
    }

    /**
     * 从远程项目根目录拉取所有文件到本地项目目录，并刷新 IntelliJ 项目视图。
     * @param project IntelliJ 项目对象，用于刷新文件系统。
     * @param localProjectDir 本地项目根目录（例如 C:/myproject）。
     * @param onResult 回调函数，接收结果 (success, message)。
     */
    fun pullDirectoryAsync(project: Project, localProjectDir: String, onResult: (Boolean, String) -> Unit) {
        runAdbTask("从设备拉取项目目录", "正在拉取项目目录...", { indicator ->
            val projectDir = File(localProjectDir)
            if (!projectDir.exists()) {
                projectDir.mkdirs()
            }

            val (connected, msg) = ensureDeviceConnected()
            if (!connected) return@runAdbTask false to msg

            var result: Pair<Boolean, String>? = null
            val latch = CountDownLatch(1)
            getProjectRootAsync { success, message ->
                if (!success) {
                    result = false to message
                    latch.countDown()
                    return@getProjectRootAsync
                }
                val remoteRoot = message.replace("//", "/")
                try {
                    // 使用 adb pull 拉取远程目录的内容（使用 /. 避免创建子目录）
                    indicator.text = "正在从 $remoteRoot 拉取目录内容到 $localProjectDir..."
                    result = executeAdbCommand("pull", "$remoteRoot/.", localProjectDir, useSerial = true)
                    if (result?.first == true) {
                        LogUtils.info("Pulled directory contents from $remoteRoot to $localProjectDir on $deviceAddress")
                        // 刷新 IntelliJ 项目目录
                        ApplicationManager.getApplication().invokeLater {
                            try {
                                reloadProjectFromDisk(project)
                                val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectDir)
                                if (virtualFile != null) {
                                    virtualFile.refresh(false, true) // 异步刷新，递归
                                    LogUtils.info("Refreshed project directory: $localProjectDir")
                                } else {
                                    LogUtils.warn("Failed to find VirtualFile for directory: $localProjectDir")
                                }
                            } catch (e: Exception) {
                                LogUtils.error("Failed to refresh project directory: ${e.message}", e)
                            }
                        }
                        result = true to "项目目录拉取成功"
                    }
                } catch (e: Exception) {
                    LogUtils.error("Failed to pull directory: ${e.message}", e)
                    result = false to "拉取项目目录失败: ${e.message}"
                }
                latch.countDown()
            }
            latch.await(30, TimeUnit.SECONDS)
            result ?: (false to "获取项目根目录超时")
        }, onResult)
    }

    fun isAdbAvailable(): Boolean {
        val (success, _) = executeAdbCommand("version", useSerial = false)
        return success
    }

    fun getDevices(): Pair<Boolean, List<String>> {
        val (success, output) = executeAdbCommand("devices", useSerial = false)
        if (!success) return false to emptyList()

        val devices = output.lines()
            .drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull { it.split("\t").firstOrNull() }

        return true to devices
    }

    private fun ensureDeviceConnected(): Pair<Boolean, String> {
        val (success, devices) = getDevices()
        if (!success) {
            return false to "无法获取设备列表"
        }
        return if (devices.contains(deviceAddress)) {
            true to "设备已连接: $deviceAddress"
        } else {
            executeAdbCommand("connect", deviceAddress, useSerial = false)
        }
    }

    fun startScrcpy(onResult: (Boolean, String) -> Unit) {
        runAdbTask("启动 scrcpy", "正在启动 scrcpy...", { indicator ->
            val (connected, msg) = ensureDeviceConnected()
            if (!connected) {
                return@runAdbTask false to "无法连接到设备: $msg"
            }
            try {
                val command = listOf(scrcpyPath, "-s", deviceAddress)
                val process = ProcessBuilder(command).start()
                // 不等待 scrcpy 退出，因为它是一个长期运行的进程
                Thread.sleep(1000) // 短暂等待以检查进程是否成功启动
                if (process.isAlive) {
                    true to "scrcpy 启动成功"
                } else {
                    val error = process.errorStream.bufferedReader().use { it.readText() }
                    false to "scrcpy 启动失败: $error"
                }
            } catch (e: IOException) {
                false to "启动 scrcpy 失败: ${e.message}"
            }
        }, onResult)
    }
}