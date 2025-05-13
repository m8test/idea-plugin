package com.m8test.idea.plugin.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.m8test.idea.plugin.server.M8TestDocsServer
import com.m8test.idea.plugin.util.LogUtils
import javax.swing.Icon

abstract class M8TestDocsAction(text: String, description: String, icon: Icon?) : AnAction(text, description, icon) {
    abstract fun getServer(): M8TestDocsServer
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        try {
            val url = getServer().getUrl(project)
            BrowserUtil.browse(url)
            LogUtils.info("Opened server URL in browser: $url")
        } catch (e: Exception) {
            LogUtils.error("Failed to open server URL: ${e.message}", e)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}