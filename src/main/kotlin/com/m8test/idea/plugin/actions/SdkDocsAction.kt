package com.m8test.idea.plugin.actions

import com.m8test.idea.plugin.server.M8TestDocsServer
import com.m8test.idea.plugin.server.SdkDocsServer

class SdkDocsAction : M8TestDocsAction("Sdk文档", "M8Test sdk 文档", null) {
    override fun getServer(): M8TestDocsServer = SdkDocsServer
}