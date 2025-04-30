package com.m8test.idea.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.m8test.idea.plugin.M8TestBundle
import com.m8test.idea.plugin.util.LogUtils

@Service(Service.Level.PROJECT)
class M8TestProjectService(project: Project) {

    init {
        LogUtils.info(M8TestBundle.message("projectService", project.name))
        LogUtils.warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    fun getRandomNumber() = (1..100).random()
}
