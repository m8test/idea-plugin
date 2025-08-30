package com.m8test.idea.plugin.settings

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * 插件的设置页面实现。
 * 它将 UI 组件 (M8TestSettingsComponent) 与配置数据 (M8TestSettings) 关联起来。
 */
class M8TestConfigurable : Configurable {

    private var settingsComponent: M8TestSettingsComponent? = null
    private val settings: M8TestSettings = M8TestSettings.instance

    // 在 plugin.xml 中定义的显示名称
    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "M8Test"
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return settingsComponent?.getPreferredFocusedComponent()
    }

    // 创建设置页面的 UI
    override fun createComponent(): JComponent? {
        settingsComponent = M8TestSettingsComponent()
        return settingsComponent?.getPanel()
    }

    // 检查 UI 上的设置是否与保存的设置不同
    override fun isModified(): Boolean {
        return settingsComponent?.scrcpyPath != settings.scrcpyPath
    }

    // 当用户点击“应用”或“确定”时调用
    override fun apply() {
        settings.scrcpyPath = settingsComponent?.scrcpyPath ?: ""
    }

    // 当用户打开设置页面或点击“重置”时调用
    override fun reset() {
        settingsComponent?.scrcpyPath = settings.scrcpyPath
    }

    // 当设置页面关闭时调用，用于释放资源
    override fun disposeUIResources() {
        settingsComponent = null
    }
}