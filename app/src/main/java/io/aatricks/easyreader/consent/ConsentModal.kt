package io.aatricks.easyreader.consent

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.aatricks.easyreader.R
import kotlinx.coroutines.launch

private val TAB_NAMES = listOf("General", "Privacy", "Data Protection", "Data Sharing")

private const val MODAL_WIDTH = 0.95f
private const val MODAL_HEIGHT = 0.92f
private const val CORNER_RADIUS = 20
private const val SHADOW_ELEVATION = 20
private const val DIVIDER_THICKNESS = 3
private const val SPACING_DP = 16
private const val HEADER_FONT_SIZE = 16
private const val SUBTITLE_FONT_SIZE = 12
private const val TAB_FONT_SIZE = 11
private const val BODY_FONT_SIZE = 13
private const val LINE_HEIGHT = 20
private const val CONSENT_FONT_SIZE = 12
private const val BUTTON_FONT_SIZE = 13

@Composable
fun ConsentModal(
    visible: Boolean,
    apiKey: String,
    onAccept: suspend () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (!visible) return

    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var showFullConsent by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(SPACING_DP.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(MODAL_WIDTH)
                .fillMaxHeight(MODAL_HEIGHT)
                .shadow(SHADOW_ELEVATION.dp, RoundedCornerShape(CORNER_RADIUS.dp))
                .border(
                    1.dp,
                    colors.primary.copy(alpha = 0.6f),
                    RoundedCornerShape(CORNER_RADIUS.dp)
                ),
            shape = RoundedCornerShape(CORNER_RADIUS.dp),
            color = colors.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ConsentHeader(colors)
                ConsentTabBar(activeTab, colors) { activeTab = it }
                ConsentTabContent(activeTab, Modifier.weight(1f))
                ConsentFooter(showFullConsent, colors) { showFullConsent = !showFullConsent }
                ConsentActions(
                    isLoading = isLoading,
                    apiKey = apiKey,
                    colors = colors,
                    onDismiss = onDismiss,
                    onOpenSettings = onOpenSettings,
                    onAccept = {
                        if (!isLoading) {
                            isLoading = true
                            scope.launch {
                                try {
                                    onAccept()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ConsentHeader(colors: androidx.compose.material3.ColorScheme) {
    Divider(
        modifier = Modifier
            .height(DIVIDER_THICKNESS.dp)
            .fillMaxWidth(),
        color = colors.primary,
        thickness = DIVIDER_THICKNESS.dp
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .background(colors.primary, RoundedCornerShape(2.dp))
        )
        Column {
            Text(
                text = "📚 Enhanced Reading Experience",
                fontSize = HEADER_FONT_SIZE.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = "Review all tabs before enabling this feature",
                fontSize = SUBTITLE_FONT_SIZE.sp,
                color = colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ConsentTabBar(
    activeTab: Int,
    colors: androidx.compose.material3.ColorScheme,
    onTabSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = activeTab,
        containerColor = Color.Transparent,
        edgePadding = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        TAB_NAMES.forEachIndexed { index, title ->
            val isSelected = activeTab == index
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontSize = TAB_FONT_SIZE.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            colors.primary
                        } else {
                            colors.onSurface.copy(alpha = 0.6f)
                        }
                    )
                },
                selectedContentColor = colors.primary,
                unselectedContentColor = colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ConsentTabContent(activeTab: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        when (activeTab) {
            0 -> GeneralTab()
            1 -> PrivacyTab()
            2 -> DataProtectionTab()
            3 -> DataSharingTab()
        }
    }
}

@Composable
private fun ConsentFooter(
    showFullConsent: Boolean,
    colors: androidx.compose.material3.ColorScheme,
    onToggle: () -> Unit
) {
    Divider(color = colors.primary.copy(alpha = 0.4f), thickness = 1.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val appName = stringResource(R.string.app_name)
        if (showFullConsent) {
            Text(
                text = buildFullConsentText(appName),
                fontSize = CONSENT_FONT_SIZE.sp,
                lineHeight = 19.sp,
                color = colors.onSurface
            )
            Text(
                text = "Less",
                fontSize = CONSENT_FONT_SIZE.sp,
                fontWeight = FontWeight.Medium,
                color = colors.primary,
                modifier = Modifier.clickable(onClick = onToggle)
            )
        } else {
            Text(
                text = buildShortConsentText(appName),
                fontSize = CONSENT_FONT_SIZE.sp,
                lineHeight = 19.sp,
                color = colors.onSurface
            )
            Text(
                text = "More",
                fontSize = CONSENT_FONT_SIZE.sp,
                fontWeight = FontWeight.Medium,
                color = colors.primary,
                modifier = Modifier.clickable(onClick = onToggle)
            )
        }
    }
}

@Composable
private fun buildFullConsentText(appName: String): String {
    return "By tapping Accept you confirm you have read and agree to " +
           "$appName's Privacy Policy, Terms of Service, the Pawns Privacy Policy " +
           "and Acceptable Use Policy, and that you are at least 18 years of age " +
           "and the primary account holder on the internet connection used by this device."
}

@Composable
private fun buildShortConsentText(appName: String): String {
    return "By tapping Accept you agree to $appName's Privacy Policy, Terms, " +
           "and the Pawns policies, and confirm you're 18+ and the account holder " +
           "on this connection."
}

@Composable
private fun ConsentActions(
    isLoading: Boolean,
    apiKey: String,
    colors: androidx.compose.material3.ColorScheme,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onAccept: () -> Unit
) {
    Divider(color = colors.primary.copy(alpha = 0.4f), thickness = 1.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = {
                onDismiss()
                onOpenSettings()
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = colors.onSurface.copy(alpha = 0.8f)
            ),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
            enabled = !isLoading
        ) {
            Text("⚙ Settings", fontSize = BUTTON_FONT_SIZE.sp, fontWeight = FontWeight.Medium)
        }

        Button(
            onClick = onAccept,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            ),
            enabled = !isLoading && apiKey.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp),
                    color = colors.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Accept", fontSize = BUTTON_FONT_SIZE.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GeneralTab() {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = SPACING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(SPACING_DP.dp)
    ) {
        TabSectionTitle("What is Enhanced Reading Experience?", colors)
        TabBodyText(
            stringResource(R.string.app_name) +
            " uses Pawns technology to optimize content delivery by utilizing " +
            "your device's idle network bandwidth. This helps us serve chapters " +
            "and images faster while you enjoy your novels, manga, and documents.",
            colors
        )
        TabSectionTitle("How it works", colors)
        TabBodyText(
            "• Your device securely shares unused bandwidth with the Pawns network\n" +
            "• This accelerates content fetching for you and other readers\n" +
            "• Only runs when the app is active or in background (if enabled)\n" +
            "• You can disable this anytime in Settings > Privacy",
            colors
        )
    }
}

@Composable
private fun PrivacyTab() {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = SPACING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(SPACING_DP.dp)
    ) {
        TabSectionTitle("Your Privacy Matters", colors)
        TabBodyText(
            "We never access your personal reading history, bookmarks, or downloaded content. " +
            "The bandwidth sharing is strictly limited to network relay functions. " +
            "No reading data, account credentials, or personal files are ever transmitted.",
            colors
        )
        TabSectionTitle("External Policies", colors)
        PolicyLink("Pawns Privacy Policy", ConsentUrls.PAWNS_PRIVACY)
        PolicyLink("Pawns Acceptable Use Policy", ConsentUrls.PAWNS_ACCEPTABLE_USE)
        PolicyLink(stringResource(R.string.app_name) + " Privacy Policy", ConsentUrls.APP_PRIVACY)
        PolicyLink(stringResource(R.string.app_name) + " Terms of Service", ConsentUrls.APP_TERMS)
    }
}

@Composable
private fun DataProtectionTab() {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = SPACING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(SPACING_DP.dp)
    ) {
        TabSectionTitle("Data We Protect", colors)
        TabBodyText(
            "• Your library and reading progress stays entirely on your device\n" +
            "• We do not collect IP addresses or browsing behavior\n" +
            "• Bandwidth sharing is anonymized and encrypted\n" +
            "• You can request data deletion at any time via " +
            stringResource(R.string.app_name) + " Legal Center",
            colors
        )
        TabSectionTitle("Security Measures", colors)
        TabBodyText(
            "All relay traffic is TLS-encrypted. Pawns operates under GDPR and CCPA " +
            "compliance frameworks. Our local AI summarization feature remains fully " +
            "offline — no data leaves your device for AI functions.",
            colors
        )
    }
}

@Composable
private fun DataSharingTab() {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = SPACING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(SPACING_DP.dp)
    ) {
        TabSectionTitle("What Gets Shared", colors)
        TabBodyText(
            "When you opt in, your device participates as a node in the Pawns content " +
            "delivery network. This means:\n" +
            "• Relaying encrypted content chunks to nearby readers (never your personal data)\n" +
            "• Contributing idle upload bandwidth during app background sessions\n" +
            "• Helping reduce server load for the entire " +
            stringResource(R.string.app_name) + " community",
            colors
        )
        TabSectionTitle("What Never Gets Shared", colors)
        TabBodyText(
            "• Your reading history, bookmarks, or notes\n" +
            "• Your personal files or downloaded novels\n" +
            "• Your identity or account information\n" +
            "• Your precise location data",
            colors
        )
    }
}

@Composable
private fun TabSectionTitle(text: String, colors: androidx.compose.material3.ColorScheme) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = colors.primary
    )
}

@Composable
private fun TabBodyText(text: String, colors: androidx.compose.material3.ColorScheme) {
    Text(
        text = text,
        fontSize = BODY_FONT_SIZE.sp,
        lineHeight = LINE_HEIGHT.sp,
        color = colors.onSurface
    )
}

@Composable
private fun PolicyLink(text: String, url: String) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    Text(
        text = text,
        fontSize = BODY_FONT_SIZE.sp,
        fontWeight = FontWeight.Medium,
        color = colors.primary,
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    )
}
