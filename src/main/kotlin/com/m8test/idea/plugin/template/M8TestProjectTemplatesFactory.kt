package com.m8test.idea.plugin.template

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.platform.ProjectTemplate
import com.intellij.platform.ProjectTemplatesFactory
import com.m8test.idea.plugin.util.IconUtils
import javax.swing.Icon

class M8TestProjectTemplatesFactory : ProjectTemplatesFactory() {
    override fun getGroups(): Array<String> {
        return arrayOf("M8Test")
    }

    override fun getGroupIcon(group: String?): Icon {
        return IconUtils.loadScaledIcon("/META-INF/pluginIcon.svg", 24)
    }

    override fun createTemplates(group: String?, context: WizardContext): Array<out ProjectTemplate> {
        return M8TestProjectTemplate.templates
    }
}