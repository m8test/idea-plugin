package com.m8test.idea.plugin.util

import com.m8test.idea.plugin.toolWindow.LogToolWindowFactory

object LogUtils {
    private lateinit var logPanel: LogToolWindowFactory.LogPanel
    fun setLogPanel(panel: LogToolWindowFactory.LogPanel) {
        this.logPanel = panel
    }

    fun appendScriptLog(entry: HttpUtils.Entry) {
        logPanel.appendScriptLog(entry)
    }

    private fun appendLog(level: String, log: String) {
        logPanel.appendPluginLog(level, log)
    }

    fun verbose(message: String) {
        appendLog(LEVEL_VERBOSE, message)
    }

    fun debug(message: String) {
        appendLog(LEVEL_DEBUG, message)
    }

    fun info(message: String) {
        appendLog(LEVEL_INFO, message)
    }

    fun warn(message: String) {
        appendLog(LEVEL_WARN, message)
    }

    fun error(message: String) {
        appendLog(LEVEL_ERROR, message)
    }

    fun error(message: String, e: Throwable) {
        appendLog(LEVEL_ERROR, message)
    }

    fun assert(message: String) {
        appendLog(LEVEL_ASSERT, message)
    }

    const val LEVEL_DEBUG = "DEBUG"
    const val LEVEL_VERBOSE = "VERBOSE"
    const val LEVEL_INFO = "INFO"
    const val LEVEL_WARN = "WARN"
    const val LEVEL_ERROR = "ERROR"
    const val LEVEL_ASSERT = "ASSERT"
}