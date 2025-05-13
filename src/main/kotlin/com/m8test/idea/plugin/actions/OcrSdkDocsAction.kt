package com.m8test.idea.plugin.actions

import com.m8test.idea.plugin.server.M8TestDocsServer
import com.m8test.idea.plugin.server.OcrSdkDocsServer

class OcrSdkDocsAction : M8TestDocsAction("文字识别Sdk文档", "文字识别 sdk 文档", null) {
    override fun getServer(): M8TestDocsServer = OcrSdkDocsServer
}