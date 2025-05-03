package com.m8test.idea.plugin.config

import com.intellij.openapi.components.*

@Service
@State(name = "M8TestSettings", storages = [Storage("m8testSettings.xml")])
class M8TestSettings : PersistentStateComponent<M8TestSettingsState> {
    private var state = M8TestSettingsState()
    override fun getState(): M8TestSettingsState = state
    override fun loadState(state: M8TestSettingsState) {
        this.state = state
    }

    companion object {
        val instance: M8TestSettings
            get() = service()
    }
}

data class M8TestSettingsState(
    var m8testProjectRoot: String = "",
    var deviceIp: String = "192.168.1.100",
    var adbPort: Int = 5555,
    var enableAdbForwarding: Boolean = false,
    var debugPort: Int = 8080
)