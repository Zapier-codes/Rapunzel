package io.aatricks.easyreader.data.repository.source

/**
 * Extended source contract for the plugin architecture.
 * All built-in and future custom sources implement this.
 */
interface SourcePlugin : NovelSource {
    /** Unique identifier, e.g. "wattpad", "royalroad", "inkitt" */
    val pluginId: String

    /** Human-readable name */
    val displayName: String

    /** Icon URL or null for default */
    val iconUrl: String? get() = null

    /** Whether this source requires user authentication */
    val requiresAuth: Boolean get() = false

    /** Whether search is supported */
    val supportsSearch: Boolean get() = true

    /** Whether chapter download is supported */
    val supportsDownload: Boolean get() = true

    /** Whether this plugin is currently enabled (feature flag + user toggle) */
    val isEnabled: Boolean get() = true

    /** Optional authentication status */
    val isAuthenticated: Boolean get() = !requiresAuth

    /** Source category for grouping in UI */
    val category: PluginCategory get() = PluginCategory.WEB

    enum class PluginCategory {
        LOCAL,      // Device files (EPUB, PDF, etc.)
        WEB,        // Web scrapers / APIs
        OFFICIAL,   // Official platform integrations
        CUSTOM,     // User-added custom plugins
    }
}
