package io.aatricks.easyreader.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.aatricks.easyreader.data.repository.source.PluginRegistry
import io.aatricks.easyreader.data.repository.source.SourcePlugin

@Composable
fun PluginSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: PluginSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val plugins = viewModel.plugins

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Source Plugins") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = "Active Sources",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            items(plugins) { plugin ->
                PluginCard(plugin = plugin)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Add Custom Source",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Custom plugin support coming in a future update.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginCard(plugin: SourcePlugin) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (plugin.category) {
                    SourcePlugin.PluginCategory.LOCAL -> Icons.Default.Storage
                    SourcePlugin.PluginCategory.WEB -> Icons.Default.Public
                    SourcePlugin.PluginCategory.OFFICIAL -> Icons.Default.Extension
                    SourcePlugin.PluginCategory.CUSTOM -> Icons.Default.Extension
                },
                contentDescription = null,
                tint = if (plugin.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = buildString {
                        append(plugin.category.name.lowercase().replaceFirstChar { it.uppercase() })
                        if (plugin.requiresAuth) append(" • Auth required")
                        if (!plugin.isEnabled) append(" • Disabled")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (plugin.requiresAuth && !plugin.isAuthenticated) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Auth required",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
