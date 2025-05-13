package com.m8test.idea.plugin.actions

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.m8test.idea.plugin.util.IconUtils
import javax.swing.Icon

class M8TestActionGroup : ActionGroup("M8Test", true) {
    init {
        templatePresentation.description = "M8Test相关操作"
        templatePresentation.icon = loadIcon()
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return arrayOf(DocumentationActionGroup())
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    private fun loadIcon(): Icon {
        return IconUtils.loadScaledIcon("/META-INF/pluginIcon.svg", 16)
    }
}