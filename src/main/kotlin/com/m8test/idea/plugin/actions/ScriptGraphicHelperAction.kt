package com.m8test.idea.plugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.m8test.gradle.downloader.binary.ScriptGraphicHelperDownloader

class ScriptGraphicHelperAction : AnAction("图色助手", "启动图色助手", null) {
    override fun actionPerformed(e: AnActionEvent) {
        val bin = ScriptGraphicHelperDownloader.getExecutable()
        ProcessBuilder(bin.absolutePath).start()
        Messages.showInfoMessage("启动图色助手成功", "提示")
    }

    override fun update(e: AnActionEvent) {
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}