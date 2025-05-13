package com.m8test.idea.plugin.template

import com.intellij.openapi.module.ModuleType
import com.m8test.gradle.util.PathUtils
import com.m8test.idea.plugin.util.IconUtils
import javax.swing.Icon

class M8TestModuleType(private val language: String) : ModuleType<M8TestModuleBuilder>("M8Test$language") {
    override fun createModuleBuilder(): M8TestModuleBuilder {
        return M8TestModuleBuilder(language)
    }

    fun getLanguage() = language

    fun getTemplateDir() = PathUtils.getM8TestPath("templates/$language")

    override fun getName(): String {
        return "M8Test模板"
    }

    override fun getDescription(): String {
        return "M8Test脚本项目模板"
    }

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return IconUtils.loadScaledIcon("META-INF/pluginIcon.svg", 18)
    }
}

// 定义 INSTANCE 作为单独的对象
object M8TestModuleTypeInstances {
    private val instances = mutableSetOf<M8TestModuleType>()
    fun getInstance(language: String): M8TestModuleType? {
        return instances.firstOrNull { it.getLanguage() == language }
    }

    fun addInstance(moduleType: M8TestModuleType): Boolean {
        val instance = getInstance(moduleType.getLanguage())
        if (instance != null) error("Module type ${moduleType.getLanguage()} already exists")
        return instances.add(moduleType)
    }
}