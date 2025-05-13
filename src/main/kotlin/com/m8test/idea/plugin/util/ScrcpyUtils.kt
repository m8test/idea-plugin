package com.m8test.idea.plugin.util


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.m8test.gradle.downloader.ScrcpyDownloader
import com.m8test.gradle.util.ConfigUtils
import java.io.IOException

object ScrcpyUtils {
    private val scrcpyPath: String
        get() = ScrcpyDownloader.getExecutable().canonicalPath

    private val state
        get() = ConfigUtils.getDeviceInfo()

    private val deviceAddress: String
        get() = state.adbDeviceSerial


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

    fun startScrcpy(onResult: (Boolean, String) -> Unit) {
        runAdbTask("启动 scrcpy", "正在启动 scrcpy...", { indicator ->
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