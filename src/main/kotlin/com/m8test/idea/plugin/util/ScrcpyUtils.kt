package com.m8test.idea.plugin.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.m8test.gradle.util.ConfigUtils
import com.m8test.idea.plugin.settings.M8TestSettings
import java.io.File
import java.io.IOException

object ScrcpyUtils {
    /**
     * 从 M8TestSettings 获取 scrcpy 的可执行文件路径。
     * 这个 getter 现在会检查路径是否已配置并且文件是否真实存在。
     * 如果路径未设置或文件不存在，则返回 null。
     */
    private val scrcpyPath: String?
        get() {
            val path = M8TestSettings.instance.scrcpyPath
            // 检查路径是否非空且对应的文件是否存在
            return if (path.isNotBlank() && File(path).exists()) {
                path
            } else {
                null
            }
        }

    // 这部分与你的原始代码保持一致
    private val state
        get() = ConfigUtils.getDeviceInfo()

    // 这部分与你的原始代码保持一致
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

    /**
     * 启动 scrcpy。
     * 在启动前会检查 scrcpy 路径是否已在设置中配置。
     * 如果未配置，则会弹出一个通知提示用户进行设置。
     * @param onResult 启动结果的回调。
     */
    fun startScrcpy(onResult: (Boolean, String) -> Unit) {
        // 3. 首先获取并验证 scrcpy 路径
        val currentScrcpyPath = scrcpyPath
        if (currentScrcpyPath == null) {
            // 4. 如果路径未配置或无效，调用通知工具类来提示用户
            NotificationUtils.showScrcpyPathNotConfiguredNotification()
            // 5. 通过回调返回失败信息，并立即终止执行
            onResult(false, "Scrcpy 路径未配置或无效，请在设置中指定。")
            return
        }

        // 6. 如果路径有效，才继续执行启动任务
        runAdbTask("启动 scrcpy", "正在启动 scrcpy...", { indicator ->
            try {
                // 7. 使用从配置中获取的有效路径
                val command = listOf(currentScrcpyPath, "-s", deviceAddress)
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