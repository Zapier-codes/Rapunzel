package io.aatricks.easyreader.ui.screens.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.aatricks.easyreader.data.repository.source.PluginRegistry
import io.aatricks.easyreader.data.repository.source.SourcePlugin
import javax.inject.Inject

@HiltViewModel
class PluginSettingsViewModel @Inject constructor(
    registry: PluginRegistry,
) : ViewModel() {
    val plugins: List<SourcePlugin> = registry.all()
}
