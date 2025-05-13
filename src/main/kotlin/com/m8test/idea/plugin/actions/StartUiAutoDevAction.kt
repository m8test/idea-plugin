package com.m8test.idea.plugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.m8test.idea.plugin.util.LogUtils
import com.m8test.idea.plugin.util.UiAutoDevUtils
import java.net.URI

class StartUiAutoDevAction : AnAction("UI布局分析", "启动 UIAutoDev HTTP 服务器或打开管理页面", null) {
    @Volatile
    private var isRunning = false

    override fun actionPerformed(e: AnActionEvent) {
        if (isRunning) {
            Messages.showInfoMessage("正在启动 UIAutoDev 服务，请等待完成！", "操作进行中")
            return
        }

        if (UiAutoDevUtils.isServerRunning()) {
            // 服务器已运行，打开浏览器
            try {
                val url = "https://uiauto.devsleep.com/"
                java.awt.Desktop.getDesktop().browse(URI(url))
                Messages.showInfoMessage("UIAutoDev 服务器已在运行，已打开管理页面: $url", "操作完成")
                LogUtils.info("打开 UIAutoDev 管理页面: $url")
            } catch (ex: Exception) {
                Messages.showErrorDialog("无法打开浏览器: ${ex.message}", "错误")
                LogUtils.error("打开浏览器失败: ${ex.message}")
            }
            return
        }

        // 服务器未运行，启动命令
        isRunning = true
        update(e)
        com.intellij.openapi.progress.ProgressManager.getInstance().run(
            object : Task.Backgroundable(e.project, "启动 UIAutoDev 服务", true) {
                override fun run(indicator: ProgressIndicator) {
                    val (success, message) = UiAutoDevUtils.startServer(indicator)
                    com.intellij.util.ui.UIUtil.invokeLaterIfNeeded {
                        if (success) {
                            Messages.showInfoMessage(message, "操作完成")
                            // 启动成功后自动打开管理页面
                            try {
                                val url = "https://uiauto.devsleep.com/"
                                java.awt.Desktop.getDesktop().browse(URI(url))
                                LogUtils.info("打开 UIAutoDev 管理页面: $url")
                            } catch (ex: Exception) {
                                Messages.showErrorDialog("无法打开浏览器: ${ex.message}", "错误")
                                LogUtils.error("打开浏览器失败: ${ex.message}")
                            }
                        } else {
                            Messages.showErrorDialog(message, "错误")
                        }
                        isRunning = false
                        update(e)
                    }
                }
            }
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !isRunning
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}