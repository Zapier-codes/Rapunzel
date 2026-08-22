package io.aatricks.easyreader.data.repository.source

import io.aatricks.easyreader.config.AppConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry of all available source plugins.
 * Built-in plugins are injected via Hilt multibinding.
 * Feature flags control which plugins are active.
 */
@Singleton
class PluginRegistry @Inject constructor(
    private val appConfig: AppConfig,
    private val localSource: LocalSourceRepository,
    private val wattpadRepository: WattpadRepository,
) {
    private val builtInSources: List<NovelSource> by lazy {
        listOfNotNull(
            localSource,
            wattpadRepository.takeIf { appConfig.isWattpadEnabled },
        )
    }

    fun all(): List<NovelSource> = builtInSources

    fun enabled(): List<NovelSource> = builtInSources.filter {
        when (it.id) {
            "wattpad" -> appConfig.isWattpadEnabled
            else -> true
        }
    }

    fun get(id: String): NovelSource? = builtInSources.find { it.id == id }

    fun getByName(name: String): NovelSource? = builtInSources.find { it.name == name }
}
