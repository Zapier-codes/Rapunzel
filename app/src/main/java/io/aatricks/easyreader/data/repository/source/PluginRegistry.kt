package io.aatricks.easyreader.data.repository.source

import io.aatricks.easyreader.config.AppConfig
import io.aatricks.easyreader.data.repository.source.SourcePlugin.PluginCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central registry of all available source plugins.
 * Built-in sources are injected via Hilt multibinding.
 * Feature flags and user toggles control which plugins are active.
 */
@Singleton
class PluginRegistry @Inject constructor(
    private val appConfig: AppConfig,
    private val localSource: LocalSourceRepository,
    private val wattpadRepository: WattpadRepository,
    private val royalRoadSource: RoyalRoadSource,
    private val inkittRepository: InkittRepository,
) {
    private val builtInPlugins: List<SourcePlugin> by lazy {
        listOfNotNull(
            localSource.asPlugin(),
            wattpadRepository.asPlugin().takeIf { appConfig.isWattpadEnabled },
            royalRoadSource.asPlugin().takeIf { appConfig.isRoyalRoadEnabled },
            inkittRepository.asPlugin().takeIf { appConfig.isInkittEnabled },
        )
    }

    fun all(): List<SourcePlugin> = builtInPlugins

    fun enabled(): List<SourcePlugin> = builtInPlugins.filter { it.isEnabled }

    fun byCategory(category: PluginCategory): List<SourcePlugin> =
        builtInPlugins.filter { it.category == category && it.isEnabled }

    fun get(id: String): SourcePlugin? = builtInPlugins.find { it.pluginId == id }

    fun getByName(name: String): SourcePlugin? = builtInPlugins.find { it.displayName == name }

    fun searchEnabled(): List<SourcePlugin> = enabled().filter { it.supportsSearch }

    fun authRequired(): List<SourcePlugin> = enabled().filter { it.requiresAuth && !it.isAuthenticated }
}

// Extension to convert existing repositories into SourcePlugin wrappers
private fun LocalSourceRepository.asPlugin(): SourcePlugin = object : SourcePlugin, NovelSource by this {
    override val pluginId = "local"
    override val displayName = "Local Files"
    override val category = PluginCategory.LOCAL
    override val requiresAuth = false
    override val supportsSearch = false
    override val isEnabled = true
}

private fun WattpadRepository.asPlugin(): SourcePlugin = object : SourcePlugin, NovelSource by this {
    override val pluginId = "wattpad"
    override val displayName = "Wattpad"
    override val category = PluginCategory.OFFICIAL
    override val requiresAuth = true
    override val isEnabled = appConfig.isWattpadEnabled
    override val isAuthenticated = false // Updated after login
}

private fun RoyalRoadSource.asPlugin(): SourcePlugin = object : SourcePlugin, NovelSource by this {
    override val pluginId = "royalroad"
    override val displayName = "Royal Road"
    override val category = PluginCategory.WEB
    override val requiresAuth = false
    override val isEnabled = appConfig.isRoyalRoadEnabled
}

private fun InkittRepository.asPlugin(): SourcePlugin = object : SourcePlugin, NovelSource by this {
    override val pluginId = "inkitt"
    override val displayName = "Inkitt"
    override val category = PluginCategory.OFFICIAL
    override val requiresAuth = true
    override val isEnabled = appConfig.isInkittEnabled
    override val isAuthenticated = false // Updated after login
}
