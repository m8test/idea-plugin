package com.m8test.idea.plugin.actions

import com.m8test.idea.plugin.server.DevDocsServer
import com.m8test.idea.plugin.server.M8TestDocsServer

class DevDocsAction : M8TestDocsAction("开发文档", "M8Test 开发文档", null) {
    override fun getServer(): M8TestDocsServer = DevDocsServer
}