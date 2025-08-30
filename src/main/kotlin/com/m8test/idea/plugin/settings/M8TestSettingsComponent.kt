package com.m8test.idea.plugin.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 管理设置页面 UI 组件的类。
 */
class M8TestSettingsComponent {

    val panel: JPanel
    val scrcpyPathField = TextFieldWithBrowseButton()

    init {
        // 配置 scrcpy 路径字段的文件选择器
        // 只允许选择单个文件，并且标题和描述可以自定义
        val descriptor = FileChooserDescriptor(
            true,   // chooseFiles
            false,  // chooseFolders
            false,  // chooseJars
            false,  // chooseJarsAsFiles
            false,  // chooseJarContents
            false   // chooseMultiple
        ).withTitle("选择 Scrcpy 可执行文件")

        scrcpyPathField.addBrowseFolderListener(
            "选择 Scrcpy 路径",
            "请指定 scrcpy 可执行文件的完整路径",
            null,
            descriptor
        )

        // 使用 FormBuilder 创建一个简单的表单布局
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Scrcpy 可执行文件路径:"), scrcpyPathField, 1, false)
            .addComponentFillVertically(JPanel(), 0) // 添加一个弹簧组件，将控件推到顶部
            .panel
    }

    /**
     * 返回主面板组件。
     */
    fun getPanel(): JComponent {
        return panel
    }

    /**
     * 返回 scrcpy 路径字段的 JComponent。
     */
    fun getPreferredFocusedComponent(): JComponent {
        return scrcpyPathField
    }

    // Getter 和 Setter 用于在 Configurable 中轻松访问文本字段的值
    var scrcpyPath: String
        get() = scrcpyPathField.text
        set(newText) {
            scrcpyPathField.text = newText
        }
}