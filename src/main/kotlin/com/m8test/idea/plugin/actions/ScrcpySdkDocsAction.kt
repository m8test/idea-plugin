package com.m8test.idea.plugin.actions

import com.m8test.idea.plugin.server.M8TestDocsServer
import com.m8test.idea.plugin.server.ScrcpySdkDocsServer

class ScrcpySdkDocsAction : M8TestDocsAction("Adb自动化Sdk文档", "Adb自动化 sdk 文档", null) {
    override fun getServer(): M8TestDocsServer = ScrcpySdkDocsServer
}