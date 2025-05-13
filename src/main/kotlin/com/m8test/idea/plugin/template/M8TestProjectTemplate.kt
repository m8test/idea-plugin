package com.m8test.idea.plugin.template

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.platform.ProjectTemplate
import com.m8test.idea.plugin.util.IconUtils
import javax.swing.Icon

abstract class M8TestProjectTemplate(
    private val name: String,
    private val description: String,
    private val iconPath: String
) : ProjectTemplate {
    init {
        M8TestModuleTypeInstances.addInstance(M8TestModuleType(name))
    }

    override fun getName(): String = name
    override fun getDescription(): String = description
    override fun getIcon(): Icon = IconUtils.loadScaledIcon(iconPath, 24)

    override fun createModuleBuilder(): AbstractModuleBuilder {
        return M8TestModuleBuilder(name)
    }

    @Deprecated("Deprecated in Java")
    override fun validateSettings(): ValidationInfo? = null

    companion object {
        val templates = arrayOf(
            GroovyProjectTemplate(),
            JavaProjectTemplate(),
            JavascriptProjectTemplate(),
            KotlinProjectTemplate(),
            LuaProjectTemplate(),
            PhpProjectTemplate(),
            PythonProjectTemplate(),
            RubyProjectTemplate()
        )
    }
}