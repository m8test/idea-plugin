package com.m8test.idea.plugin.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil

object NotificationUtils {

    // 必须与 plugin.xml 中注册的 ID 一致
    private const val NOTIFICATION_GROUP_ID = "M8TestPluginNotifications"

    fun showScrcpyPathNotConfiguredNotification() {
        val notification = Notification(
            NOTIFICATION_GROUP_ID,
            "Scrcpy 路径未配置",
            "请在设置中指定 scrcpy 可执行文件的路径",
            NotificationType.WARNING
        )

        // 添加一个“打开设置”的操作
        notification.addAction(object : AnAction("打开设置...") {
            override fun actionPerformed(e: AnActionEvent) {
                // 打开插件的设置页面, "M8Test" 应该是你在 Configurable 中设置的 displayName
                ShowSettingsUtil.getInstance().showSettingsDialog(e.project, "M8Test")
                notification.expire() // 用户点击后关闭通知
            }
        })

        Notifications.Bus.notify(notification)
    }
}