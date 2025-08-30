package com.m8test.idea.plugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.m8test.idea.plugin.settings.M8TestSettings",
    storages = [Storage("m8testSettings.xml")]
)
class M8TestSettings : PersistentStateComponent<M8TestSettings> {

    var scrcpyPath: String = ""

    override fun getState(): M8TestSettings? {
        return this
    }

    override fun loadState(state: M8TestSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: M8TestSettings
            get() = ApplicationManager.getApplication().getService(M8TestSettings::class.java)
    }
}