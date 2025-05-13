package com.m8test.idea.plugin.util

import com.intellij.openapi.progress.ProgressIndicator
import com.m8test.gradle.util.PathUtils
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

object UiAutoDevUtils {
    private val runningProcess = AtomicReference<Process?>(null)

    /**
     * 检查 UIAutoDev 服务器是否正在运行（通过进程活跃性）。
     */
    fun isServerRunning(): Boolean {
        val process = runningProcess.get()
        return process?.isAlive() == true
    }

    /**
     * 启动 UIAutoDev 服务器（python -m uiautodev）。
     * @return Pair<成功与否, 消息>
     */
    fun startServer(indicator: ProgressIndicator? = null): Pair<Boolean, String> {
        if (isServerRunning()) {
            return true to "UIAutoDev 服务器已在运行"
        }

        try {
            indicator?.text = "正在启动 UIAutoDev 服务器..."
            val command = listOf(File(PathUtils.getBinPath(), "python/python.exe").canonicalPath, "-m", "uiautodev")
            val processBuilder = ProcessBuilder(command)
                .directory(File(System.getProperty("user.home"))) // 工作目录为用户主目录
                .redirectErrorStream(true) // 合并标准错误和输出

            // 启动进程
            val process = processBuilder.start()
            runningProcess.set(process)

            // 异步读取输出
            Thread {
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        LogUtils.info("UIAutoDev 输出: $line")
                        indicator?.text2 = line
                    }
                }
            }.start()

            // 等待短暂时间检查进程是否存活
            Thread.sleep(2000)
            if (!process.isAlive) {
                val errorMsg = "UIAutoDev 服务器启动失败: 进程意外终止"
                LogUtils.error(errorMsg)
                runningProcess.set(null)
                return false to errorMsg
            }

            LogUtils.info("UIAutoDev 服务器启动成功")
            return true to "UIAutoDev 服务器启动成功"
        } catch (e: IOException) {
            val errorMsg = "UIAutoDev 服务器启动失败: ${e.message}"
            LogUtils.error(errorMsg)
            runningProcess.set(null)
            return false to errorMsg
        }
    }

    /**
     * 停止 UIAutoDev 服务器（可选，用于后续扩展）。
     */
    fun stopServer(): Pair<Boolean, String> {
        val process = runningProcess.getAndSet(null) ?: return true to "UIAutoDev 服务器未运行"
        try {
            process.destroy()
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            if (process.isAlive) {
                process.destroyForcibly()
            }
            LogUtils.info("UIAutoDev 服务器已停止")
            return true to "UIAutoDev 服务器已停止"
        } catch (e: Exception) {
            LogUtils.error("停止 UIAutoDev 服务器失败: ${e.message}")
            return false to "停止 UIAutoDev 服务器失败: ${e.message}"
        }
    }
}