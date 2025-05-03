package com.m8test.idea.plugin.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.IconUtil
import com.m8test.idea.plugin.config.M8TestSettings
import com.m8test.idea.plugin.util.AdbUtils
import com.m8test.idea.plugin.util.HttpUtils
import com.m8test.idea.plugin.util.LogUtils
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import javax.swing.*

class M8TestToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowPanel = JPanel(BorderLayout())

        // 使用 JBTabbedPane 替代 JTabbedPane
        val tabbedPane = JBTabbedPane()

        // 获取项目根目录
        val projectDir = project.basePath ?: run {
            Messages.showErrorDialog("无法获取项目目录", "错误")
            return
        }

        val myToolWindow = MyToolWindow(project, projectDir)
        tabbedPane.addTab("配置", myToolWindow.createConfigPanel())
        tabbedPane.addTab("操作", myToolWindow.createActionPanel())

        // 使用 JBScrollPane 来实现垂直滚动
        val scrollPane = JBScrollPane(tabbedPane)
        toolWindowPanel.add(scrollPane, BorderLayout.CENTER)

        val content = ContentFactory.getInstance().createContent(toolWindowPanel, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(private val project: Project, private val projectDir: String) : Disposable {
        private val settings = ApplicationManager.getApplication().service<M8TestSettings>()

        private var ipField = JTextField()
        private var portField = JTextField()
        private var debugPortField = JTextField()
        private var enableAdbCheckbox = JCheckBox()

        fun createConfigPanel(): JPanel {
            return panel {
                row("设备 IP:") {
                    ipField = JTextField(settings.state.deviceIp)
                    cell(ipField)
                }
                row("Adb 端口:") {
                    portField = JTextField(settings.state.adbPort.toString())
                    cell(portField)
                }
                row("调试端口:") {
                    debugPortField = JTextField(settings.state.debugPort.toString())
                    cell(debugPortField)
                }
                row {
                    enableAdbCheckbox = JCheckBox("启用 ADB 端口转发", settings.state.enableAdbForwarding)
                    cell(enableAdbCheckbox)
                }
                row {
                    val icon = loadScaledIcon("/icons/save.svg", 24)
                    val button = button("保存配置") {
                        if (saveSettings()) {
                            LogUtils.info("配置已保存。")
                            refreshUI()
                        }
                    }
                    button.component.icon = icon
                }
                row {
                    val refreshIcon = loadScaledIcon("/icons/refresh.svg", 24)
                    button("刷新配置") {
                        refreshUI()
                        LogUtils.info("配置已刷新。")
                    }.component.icon = refreshIcon
                }
            }
        }

        fun createActionPanel(): JPanel {
            return panel {
                row {
                    val connectIcon = loadScaledIcon("/icons/connect.svg", 24)
                    val button = button("连接设备") {
                        if (!saveSettings()) return@button

                        if (!AdbUtils.isAdbAvailable()) {
                            Messages.showErrorDialog("ADB 不可用，请确保 scrcpy 已正确配置。", "ADB 错误")
                            return@button
                        }
                        AdbUtils.connectDeviceAsync { success, message ->
                            if (success) {
                                LogUtils.info("成功连接到设备：$message")
                            } else {
                                Messages.showErrorDialog("连接设备失败：$message", "ADB 连接错误")
                            }
                        }
                    }
                    button.component.icon = connectIcon
                }
                row {
                    val forwardIcon = loadScaledIcon("/icons/forward.svg", 24)
                    val button = button("执行端口转发") {
                        if (!saveSettings()) return@button

                        if (!AdbUtils.isAdbAvailable()) {
                            Messages.showErrorDialog("ADB 不可用，请确保 scrcpy 已正确配置。", "ADB 错误")
                            return@button
                        }

                        val confirm = Messages.showYesNoDialog(
                            "确定要执行端口转发吗？\n这将转发本地端口 ${settings.state.debugPort} 到远程设备端口。",
                            "确认端口转发",
                            Messages.getQuestionIcon()
                        )
                        if (confirm != Messages.YES) return@button

                        AdbUtils.forwardPortAsync { success, message ->
                            if (success) {
                                LogUtils.info("端口转发成功：$message")
                            } else {
                                Messages.showErrorDialog("端口转发失败：$message", "ADB 错误")
                            }
                        }
                    }
                    button.component.icon = forwardIcon
                }

                row {
                    val disconnectIcon = loadScaledIcon("/icons/disconnect.svg", 24)
                    val button = button("断开设备") {
                        if (!saveSettings()) return@button

                        if (!AdbUtils.isAdbAvailable()) {
                            Messages.showErrorDialog("ADB 不可用。", "ADB 错误")
                            return@button
                        }
                        AdbUtils.disconnectDeviceAsync { success, message ->
                            if (success) {
                                LogUtils.info("成功断开设备：$message")
                            } else {
                                Messages.showErrorDialog("断开设备失败：$message", "ADB 错误")
                            }
                        }
                    }
                    button.component.icon = disconnectIcon
                }

                row {
                    val scrcpyIcon = loadScaledIcon("/icons/launch.svg", 24)
                    val button = button("启动 scrcpy") {
                        if (!saveSettings()) return@button

                        if (!AdbUtils.isAdbAvailable()) {
                            Messages.showErrorDialog("ADB 不可用，请确保 scrcpy 已正确配置。", "ADB 错误")
                            return@button
                        }
                        AdbUtils.startScrcpy { success, message ->
                            if (success) {
                                LogUtils.info("scrcpy 启动成功：$message")
                            } else {
                                Messages.showErrorDialog("启动 scrcpy 失败：$message", "scrcpy 启动错误")
                            }
                        }
                    }
                    button.component.icon = scrcpyIcon
                }

                row {
                    val pushIcon = loadScaledIcon("/icons/upload.svg", 24)
                    val button = button("推送项目文件") {
                        if (!saveSettings()) return@button

                        if (!AdbUtils.isAdbAvailable()) {
                            Messages.showErrorDialog("ADB 不可用，请确保 scrcpy 已正确配置。", "ADB 错误")
                            return@button
                        }

                        val confirm = Messages.showYesNoDialog(
                            "确定要推送项目目录到设备？这可能覆盖远程文件。",
                            "确认推送",
                            Messages.getQuestionIcon()
                        )
                        if (confirm != Messages.YES) return@button

                        AdbUtils.pushDirectoryAsync(projectDir) { success, message ->
                            if (success) {
                                LogUtils.info("项目文件推送成功：$message")
                            } else {
                                Messages.showErrorDialog("项目文件推送失败：$message", "ADB 错误")
                            }
                        }
                    }
                    button.component.icon = pushIcon
                }

                row {
                    val pullIcon = loadScaledIcon("/icons/download.svg", 24)
                    val button = button("拉取项目文件") {
                        if (!saveSettings()) return@button

                        if (!AdbUtils.isAdbAvailable()) {
                            Messages.showErrorDialog("ADB 不可用，请确保 scrcpy 已正确配置。", "ADB 错误")
                            return@button
                        }

                        val confirm = Messages.showYesNoDialog(
                            "确定要从设备拉取项目目录？这可能覆盖本地文件。",
                            "确认拉取",
                            Messages.getQuestionIcon()
                        )
                        if (confirm != Messages.YES) return@button

                        AdbUtils.pullDirectoryAsync(project, projectDir) { success, message ->
                            if (success) {
                                LogUtils.info("项目文件拉取成功：$message")
                            } else {
                                Messages.showErrorDialog("项目文件拉取失败：$message", "ADB 错误")
                            }
                        }
                    }
                    button.component.icon = pullIcon
                }

                row {
                    val pushRunIcon = loadScaledIcon("/icons/upload-run.svg", 24)
                    val button = button("推送并运行项目") {
                        if (!saveSettings()) return@button

                        if (!AdbUtils.isAdbAvailable()) {
                            Messages.showErrorDialog("ADB 不可用，请确保 scrcpy 已正确配置。", "ADB 错误")
                            return@button
                        }

                        val confirm = Messages.showYesNoDialog(
                            "确定要推送项目目录并运行项目？这可能覆盖远程文件。",
                            "确认推送并运行",
                            Messages.getQuestionIcon()
                        )
                        if (confirm != Messages.YES) return@button

                        val argument = Messages.showInputDialog(
                            "请输入运行项目的参数：",
                            "推送并运行项目",
                            Messages.getQuestionIcon(),
                            "::run",
                            null
                        )
                        if (argument == null) return@button

                        AdbUtils.pushDirectoryAsync(projectDir) { success, message ->
                            if (!success) {
                                ApplicationManager.getApplication().invokeLater {
                                    Messages.showErrorDialog("项目文件推送失败：$message", "ADB 错误")
                                }
                                return@pushDirectoryAsync
                            }

                            ApplicationManager.getApplication().executeOnPooledThread {
                                try {
                                    runBlocking {
                                        HttpUtils.startProject(argument)
                                    }
                                    ApplicationManager.getApplication().invokeLater {
                                        LogUtils.info("项目推送并运行成功")
                                    }
                                } catch (e: Exception) {
                                    ApplicationManager.getApplication().invokeLater {
                                        Messages.showErrorDialog(
                                            "运行项目失败: ${e.message}",
                                            "运行错误"
                                        )
                                    }
                                }
                            }
                        }
                    }
                    button.component.icon = pushRunIcon
                }

                row {
                    val pullRunIcon = loadScaledIcon("/icons/download-run.svg", 24)
                    val button = button("拉取并运行项目") {
                        if (!saveSettings()) return@button

                        if (!AdbUtils.isAdbAvailable()) {
                            Messages.showErrorDialog("ADB 不可用，请确保 scrcpy 已正确配置。", "ADB 错误")
                            return@button
                        }

                        val confirm = Messages.showYesNoDialog(
                            "确定要从设备拉取项目目录并运行项目？这可能覆盖本地文件。",
                            "确认拉取并运行",
                            Messages.getQuestionIcon()
                        )
                        if (confirm != Messages.YES) return@button

                        val argument = Messages.showInputDialog(
                            "请输入运行项目的参数：",
                            "拉取并运行项目",
                            Messages.getQuestionIcon(),
                            "::run",
                            null
                        )
                        if (argument == null) return@button

                        AdbUtils.pullDirectoryAsync(project, projectDir) { success, message ->
                            if (!success) {
                                ApplicationManager.getApplication().invokeLater {
                                    Messages.showErrorDialog("项目文件拉取失败：$message", "ADB 错误")
                                }
                                return@pullDirectoryAsync
                            }

                            ApplicationManager.getApplication().executeOnPooledThread {
                                try {
                                    runBlocking {
                                        HttpUtils.startProject(argument)
                                    }
                                    ApplicationManager.getApplication().invokeLater {
                                        LogUtils.info("项目拉取并运行成功")
                                    }
                                } catch (e: Exception) {
                                    ApplicationManager.getApplication().invokeLater {
                                        Messages.showErrorDialog(
                                            "运行项目失败: ${e.message}",
                                            "运行错误"
                                        )
                                    }
                                }
                            }
                        }
                    }
                    button.component.icon = pullRunIcon
                }

                row {
                    val webSocketIcon = loadScaledIcon("/icons/websocket.svg", 24)
                    val button = button("连接 WebSocket 日志") {
                        if (!saveSettings()) return@button

                        val confirm = Messages.showYesNoDialog(
                            "确定要连接 WebSocket 日志？",
                            "确认连接",
                            Messages.getQuestionIcon()
                        )
                        if (confirm != Messages.YES) return@button

                        HttpUtils.connectWebSocketAsync { success, message ->
                            ApplicationManager.getApplication().invokeLater {
                                if (success) {
                                    LogUtils.info("WebSocket 连接成功")
                                } else {
                                    Messages.showErrorDialog(
                                        "WebSocket 连接失败: $message\n请检查设备 IP、调试端口和网络连接。",
                                        "WebSocket 错误"
                                    )
                                }
                            }
                        }
                    }
                    button.component.icon = webSocketIcon
                }

                row {
                    val runIcon = loadScaledIcon("/icons/run.svg", 24)
                    val button = button("运行项目") {
                        if (!saveSettings()) return@button
                        val argument = Messages.showInputDialog(
                            "请输入运行项目的参数：",
                            "运行项目",
                            Messages.getQuestionIcon(),
                            "::run",  // 默认值参数
                            null // 选择器，通常为 null
                        )
                        if (argument == null) return@button // User cancelled

                        ApplicationManager.getApplication().executeOnPooledThread {
                            try {
                                runBlocking {
                                    HttpUtils.startProject(argument)
                                }
                                ApplicationManager.getApplication().invokeLater {
                                    LogUtils.info("项目运行命令已发送")
                                }
                            } catch (e: Exception) {
                                ApplicationManager.getApplication().invokeLater {
                                    Messages.showErrorDialog(
                                        "运行项目失败: ${e.message}",
                                        "运行错误"
                                    )
                                }
                            }
                        }
                    }
                    button.component.icon = runIcon
                }

                row {
                    val stopIcon = loadScaledIcon("/icons/interrupt.svg", 24)
                    val button = button("中断项目") {
                        if (!saveSettings()) return@button

                        val confirm = Messages.showYesNoDialog(
                            "确定要中断项目吗？",
                            "确认中断",
                            Messages.getQuestionIcon()
                        )
                        if (confirm != Messages.YES) return@button

                        ApplicationManager.getApplication().executeOnPooledThread {
                            try {
                                runBlocking {
                                    HttpUtils.interruptProject()
                                }
                                ApplicationManager.getApplication().invokeLater {
                                    LogUtils.info("项目中断命令已发送")
                                }
                            } catch (e: Exception) {
                                ApplicationManager.getApplication().invokeLater {
                                    Messages.showErrorDialog(
                                        "中断项目失败: ${e.message}",
                                        "中断错误"
                                    )
                                }
                            }
                        }
                    }
                    button.component.icon = stopIcon
                }
            }
        }

        private fun refreshUI() {
            ipField.text = settings.state.deviceIp
            portField.text = settings.state.adbPort.toString()
            debugPortField.text = settings.state.debugPort.toString()
            enableAdbCheckbox.isSelected = settings.state.enableAdbForwarding
        }

        private fun saveSettings(): Boolean {
            val ip = ipField.text.trim()
            val port = portField.text.toIntOrNull()
            val debugPort = debugPortField.text.toIntOrNull()
            val enableAdb = enableAdbCheckbox.isSelected

            if (!isValidIp(ip)) {
                Messages.showErrorDialog("请输入有效的 IP 地址。", "配置错误")
                return false
            }
            if (port == null || port !in 1..65535) {
                Messages.showErrorDialog("请输入有效的端口号 (1-65535)。", "配置错误")
                return false
            }
            if (debugPort == null || debugPort !in 1..65535) {
                Messages.showErrorDialog("请输入有效的调试端口号 (1-65535)。", "配置错误")
                return false
            }

            settings.state.deviceIp = ip
            settings.state.adbPort = port
            settings.state.debugPort = debugPort
            settings.state.enableAdbForwarding = enableAdb
            return true
        }

        private fun isValidIp(ip: String): Boolean {
            val regex = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$".toRegex()
            if (!ip.matches(regex)) return false
            return ip.split(".").all { it.toIntOrNull()?.let { n -> n in 0..255 } == true }
        }

        // 加载并缩放图标
        private fun loadScaledIcon(path: String, size: Int): Icon {
            try {
                val originalIcon = IconLoader.getIcon(path, MyToolWindow::class.java)
                return IconUtil.scale(originalIcon, null, size.toFloat() / originalIcon.iconWidth)
            } catch (e: Exception) {
                Messages.showErrorDialog("无法加载图标: $path", "图标加载错误")
                return ImageIcon()
            }
        }

        override fun dispose() {
            runBlocking {
                HttpUtils.close()
            }
        }
    }
}