package com.m8test.idea.plugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.m8test.gradle.util.PathUtils
import com.m8test.idea.plugin.util.ResourcesUtils
import java.awt.Dimension
import java.io.File
import java.io.IOException
import javax.swing.JComponent

class InstallM8TestAction : AnAction("安装环境", "安装开发环境", null) {
    @Volatile
    private var isRunning = false

    override fun actionPerformed(e: AnActionEvent) {
        if (isRunning) {
            Messages.showInfoMessage("正在安装环境，请等待完成！", "操作进行中")
            return
        }

        // 创建对话框
        val dialog = InputPathsDialog()
        if (!dialog.showAndGet()) {
            return // 用户点击“取消”或关闭对话框
        }

        // 获取用户输入
        val gradleZipPath = dialog.gradleZipPath
        val m8testZipPath = dialog.m8testZipPath

        // 验证输入
        val gradleZipFile = File(gradleZipPath)
        val m8testZipFile = File(m8testZipPath)
        if (!gradleZipFile.exists() || !gradleZipFile.isFile || !gradleZipPath.endsWith(".zip")) {
            Messages.showErrorDialog("Gradle 压缩包路径无效或不是 ZIP 文件", "输入错误")
            return
        }
        if (!m8testZipFile.exists() || !m8testZipFile.isFile || !m8testZipPath.endsWith(".zip")) {
            Messages.showErrorDialog("m8test 压缩包路径无效或不是 ZIP 文件", "输入错误")
            return
        }

        isRunning = true
        update(e)

        // 目标目录
        val gradleTargetDir = File(System.getProperty("user.home"), ".gradle/wrapper/dists")
        val m8testTargetDir = PathUtils.getM8TestPath("")

        // 后台任务
        object : Task.Backgroundable(e.project, "安装环境", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    // 解压 Gradle ZIP
                    indicator.text = "正在解压 Gradle 压缩包 ${gradleZipFile.name}..."
                    ResourcesUtils.unzipFromFile(gradleZipFile, gradleTargetDir)
                    // 解压 m8test ZIP
                    indicator.text = "正在解压 m8test 压缩包 ${m8testZipFile.name}..."
                    ResourcesUtils.unzipFromFile(m8testZipFile, m8testTargetDir)
                } catch (e: IOException) {
                    throw RuntimeException("安装环境失败: ${e.message}", e)
                }
            }

            override fun onSuccess() {
                isRunning = false
                update(e)
                Messages.showInfoMessage(
                    "环境安装成功！\nGradle 解压到 ${gradleTargetDir.canonicalPath}\nm8test 解压到 ${m8testTargetDir.canonicalPath}",
                    "安装完成"
                )
            }

            override fun onThrowable(error: Throwable) {
                isRunning = false
                update(e)
                Messages.showErrorDialog("安装环境失败: ${error.message}", "错误")
            }

            override fun onCancel() {
                isRunning = false
                update(e)
                Messages.showInfoMessage("环境安装已取消", "操作取消")
            }
        }.queue()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !isRunning
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}

// 对话框类
class InputPathsDialog : DialogWrapper(true) {
    // 默认路径
    private var _gradleZipPath = File(File(PathManager.getHomePath(), "m8test"), "gradle.zip").canonicalPath
    private var _m8testZipPath = File(File(PathManager.getHomePath(), "m8test"), "m8test.zip").canonicalPath

    // 公开访问器
    val gradleZipPath: String get() = _gradleZipPath
    val m8testZipPath: String get() = _m8testZipPath

    init {
        title = "输入压缩包路径"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row { label("Gradle 压缩包路径:") }
            row {
                textField()
                    .bindText(::_gradleZipPath)
                    .columns(50)
                    .align(AlignX.FILL) // 修复：使用 align(AlignX.FILL) 替代 horizontalAlign
                    .applyToComponent { preferredSize = Dimension(400, preferredSize.height) }
            }
            row { label("m8test 压缩包路径:") }
            row {
                textField()
                    .bindText(::_m8testZipPath)
                    .columns(50)
                    .align(AlignX.FILL) // 修复：使用 align(AlignX.FILL)
                    .applyToComponent { preferredSize = Dimension(400, preferredSize.height) }
            }
        }
    }

    override fun doValidate(): ValidationInfo? {
        if (_gradleZipPath.isEmpty()) {
            return ValidationInfo("请输入 Gradle 压缩包路径")
        }
        if (_m8testZipPath.isEmpty()) {
            return ValidationInfo("请输入 m8test 压缩包路径")
        }
        return null
    }
}