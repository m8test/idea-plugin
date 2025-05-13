package com.m8test.idea.plugin.actions

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class DocumentationActionGroup : ActionGroup("文档", true) {
    init {
        templatePresentation.description = "M8Test相关文档"
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return arrayOf(SdkDocsAction(), DevDocsAction())
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}