package com.m8test.idea.plugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.m8test.idea.plugin.util.WebSocketUtils

class ConnectWebSocketAction : AnAction("连接日志服务", "连接 WebSocket 以查看日志", null) {
    override fun actionPerformed(e: AnActionEvent) {
        WebSocketUtils.connectWebSocketAsync { success, message ->
            // 确保在 EDT 上显示提示
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                if (success) {
                    Messages.showInfoMessage("WebSocket 连接成功", "操作完成")
                } else {
                    Messages.showErrorDialog("WebSocket 连接失败: $message", "错误")
                }
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}