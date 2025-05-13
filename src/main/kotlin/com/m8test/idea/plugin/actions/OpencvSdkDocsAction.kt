package com.m8test.idea.plugin.actions

import com.m8test.idea.plugin.server.M8TestDocsServer
import com.m8test.idea.plugin.server.OpencvSdkDocsServer

class OpencvSdkDocsAction : M8TestDocsAction("图色Sdk文档", "图色 sdk 文档", null) {
    override fun getServer(): M8TestDocsServer = OpencvSdkDocsServer
}