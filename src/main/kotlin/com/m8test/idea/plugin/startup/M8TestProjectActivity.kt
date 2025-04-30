package com.m8test.idea.plugin.startup

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import com.m8test.idea.plugin.util.ScrcpyUnzipper

class M8TestProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            ToolWindowManager.getInstance(project).getToolWindow("M8TestLog")?.show()
            ToolWindowManager.getInstance(project).getToolWindow("M8Test")?.show()
            ApplicationManager.getApplication().executeOnPooledThread {
                ScrcpyUnzipper.ensureUnzipped()
            }
        }
    }
}