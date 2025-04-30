package com.m8test.idea.plugin.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.Topic
import com.m8test.idea.plugin.config.M8TestConfigurable.M8TestSettingsListener
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

val CONFIG_TOPIC = Topic.create("M8TestSettingsChanged", M8TestSettingsListener::class.java)

class M8TestConfigurable : Configurable {

    private var panel: JPanel? = null
    private var ipField: JTextField? = null
    private var portField: JTextField? = null
    private var debugPortField: JTextField? = null
    private var enableAdbCheckbox: JCheckBox? = null

    private val settings = M8TestSettings.instance
    private val messageBus: MessageBus = ApplicationManager.getApplication().messageBus

    interface M8TestSettingsListener {
        fun onSettingsChanged()
    }

    override fun createComponent(): JComponent {
        panel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            insets = Insets(5, 5, 5, 5)
            fill = GridBagConstraints.HORIZONTAL
        }

        // 设备 IP
        constraints.gridx = 0
        constraints.gridy = 0
        panel?.add(JLabel("设备 IP:"), constraints)

        ipField = JTextField(20)
        constraints.gridx = 1
        panel?.add(ipField, constraints)

        // 设备端口
        constraints.gridx = 0
        constraints.gridy = 1
        panel?.add(JLabel("Adb 端口:"), constraints)

        portField = JTextField(5)
        constraints.gridx = 1
        panel?.add(portField, constraints)

        // 调试端口
        constraints.gridx = 0
        constraints.gridy = 2
        panel?.add(JLabel("调试端口:"), constraints)

        debugPortField = JTextField(5)
        constraints.gridx = 1
        panel?.add(debugPortField, constraints)

        // ADB 转发开关
        constraints.gridx = 0
        constraints.gridy = 3
        constraints.gridwidth = 2
        enableAdbCheckbox = JCheckBox("启用 ADB 端口转发")
        panel?.add(enableAdbCheckbox, constraints)

        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        return ipField?.text != settings.deviceIp ||
                portField?.text?.toIntOrNull() != settings.adbPort ||
                debugPortField?.text?.toIntOrNull() != settings.debugPort ||
                enableAdbCheckbox?.isSelected != settings.enableAdbForwarding
    }

    override fun apply() {
        val ip = ipField?.text?.trim() ?: ""
        val port = portField?.text?.toIntOrNull()
        val debugPort = debugPortField?.text?.toIntOrNull()
        val enableAdb = enableAdbCheckbox?.isSelected ?: false

        if (ip.isEmpty() || !isValidIp(ip)) {
            Messages.showErrorDialog("请输入有效的 IP 地址。", "无效输入")
            return
        }

        if (port == null || port !in 1..65535) {
            Messages.showErrorDialog("请输入有效的端口号 (1-65535)。", "无效输入")
            return
        }

        if (debugPort == null || debugPort !in 1..65535) {
            Messages.showErrorDialog("请输入有效的调试端口号 (1-65535)。", "无效输入")
            return
        }

        settings.deviceIp = ip
        settings.adbPort = port
        settings.debugPort = debugPort
        settings.enableAdbForwarding = enableAdb

        messageBus.syncPublisher(CONFIG_TOPIC).onSettingsChanged()
    }

    override fun reset() {
        ipField?.text = settings.deviceIp
        portField?.text = settings.adbPort.toString()
        debugPortField?.text = settings.debugPort.toString()
        enableAdbCheckbox?.isSelected = settings.enableAdbForwarding
    }

    override fun getDisplayName(): String {
        return "ADB 设置"
    }

    override fun disposeUIResources() {
        panel = null
        ipField = null
        portField = null
        debugPortField = null
        enableAdbCheckbox = null
    }

    private fun isValidIp(ip: String): Boolean {
        val regex = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$".toRegex()
        if (!ip.matches(regex)) return false
        return ip.split(".").all { it.toIntOrNull()?.let { num -> num in 0..255 } == true }
    }
}
