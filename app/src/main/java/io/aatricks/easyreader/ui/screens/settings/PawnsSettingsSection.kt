@file:Suppress("WildcardImport")

package io.aatricks.easyreader.ui.screens.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.aatricks.easyreader.domain.PawnsManager
import kotlinx.coroutines.launch

@Composable
fun PawnsSettingsSection(
    modifier: Modifier = Modifier,
    pawnsManager: PawnsManager = hiltViewModel<PawnsSettingsViewModel>().pawnsManager,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isSharing by pawnsManager.isSharing.collectAsState()
    val consentGiven by pawnsManager.consentGiven.collectAsState()
    val earningsToday by pawnsManager.earningsToday.collectAsState()
    val totalEarnings by pawnsManager.totalEarnings.collectAsState()
    val isAvailable = pawnsManager.isAvailable

    var showConsentDialog by remember { mutableStateOf(false) }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pawnsManager.setConsent(true)
            scope.launch { pawnsManager.start(context) }
        }
    }

    LaunchedEffect(Unit) {
        pawnsManager.refreshConsent()
        pawnsManager.refreshEarnings()
    }

    if (!isAvailable) return

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Bandwidth Sharing",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Share Unused Bandwidth",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Earn passive income by sharing idle internet bandwidth",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isSharing,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                if (enabled) {
                                    if (!consentGiven) {
                                        showConsentDialog = true
                                    } else {
                                        pawnsManager.start(context)
                                    }
                                } else {
                                    pawnsManager.stop(context)
                                }
                            }
                        }
                    )
                }

                if (isSharing || earningsToday > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EarningsStat("Today", "\$${String.format(java.util.Locale.US, "%.2f", earningsToday)}")
                        EarningsStat("Total", "\$${String.format(java.util.Locale.US, "%.2f", totalEarnings)}")
                    }
                }
            }
        }
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text("Consent Required") },
            text = {
                Text(
                    "To share bandwidth, you must agree to the Pawns.app terms and conditions. " +
                    "Your device will act as a secure gateway for internet traffic. " +
                    "You can withdraw consent at any time in settings."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConsentDialog = false
                    val intent = pawnsManager.getConsentIntent(context)
                    if (intent != null) {
                        consentLauncher.launch(intent)
                    }
                }) {
                    Text("Review & Consent")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EarningsStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
