package com.m8test.idea.plugin.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.m8test.idea.plugin.server.M8TestDocsServer
import com.m8test.idea.plugin.util.LogUtils

@Service(Service.Level.PROJECT)
class StaticServerComponent(project: Project) : Disposable {
    override fun dispose() {
        LogUtils.info("Disposing StaticServerComponent")
        M8TestDocsServer.servers.forEach {
            it.stopServer()
        }
    }
}