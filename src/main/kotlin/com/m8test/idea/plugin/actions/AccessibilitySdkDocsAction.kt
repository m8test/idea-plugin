package com.m8test.idea.plugin.actions

import com.m8test.idea.plugin.server.AccessibilitySdkDocsServer
import com.m8test.idea.plugin.server.M8TestDocsServer

class AccessibilitySdkDocsAction : M8TestDocsAction("无障碍Sdk文档", "无障碍 sdk 文档", null) {
    override fun getServer(): M8TestDocsServer = AccessibilitySdkDocsServer
}