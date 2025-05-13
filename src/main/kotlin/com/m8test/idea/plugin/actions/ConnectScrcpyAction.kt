package com.m8test.idea.plugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.m8test.idea.plugin.util.ScrcpyUtils

class ConnectScrcpyAction : AnAction("启动投屏", "启动scrcpy投屏", null) {
    override fun actionPerformed(e: AnActionEvent) {
        ScrcpyUtils.startScrcpy { success, message ->
            // 确保在 EDT 上显示提示
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                if (success) {
                    Messages.showInfoMessage("投屏成功", "操作完成")
                } else {
                    Messages.showErrorDialog("投屏失败: $message", "错误")
                }
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}