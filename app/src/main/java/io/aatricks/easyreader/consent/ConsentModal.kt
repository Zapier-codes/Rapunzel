package io.aatricks.easyreader.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

object ConsentUrls {
    const val PAWNS_PRIVACY = "https://pawns.app/privacy-policy"
    const val PAWNS_ACCEPTABLE_USE = "https://pawns.app/acceptable-use-policy"
    const val APP_PRIVACY = "https://rapunzel.app/privacy"
    const val APP_TERMS = "https://rapunzel.app/terms"
    const val APP_LEGAL = "https://rapunzel.app/legal"
}

val TAB_NAMES = listOf("General", "Privacy", "Data Protection", "Data Sharing")

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
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .shadow(20.dp, RoundedCornerShape(20.dp))
                .border(1.dp, colors.primary.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Divider(
                    modifier = Modifier
                        .height(3.dp)
                        .fillMaxWidth(),
                    color = colors.primary,
                    thickness = 3.dp
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Review all tabs before enabling this feature",
                            fontSize = 12.sp,
                            color = colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

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
                            onClick = { activeTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.primary else colors.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            selectedContentColor = colors.primary,
                            unselectedContentColor = colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
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

                Divider(color = colors.primary.copy(alpha = 0.4f), thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showFullConsent) {
                        Text(
                            text = "By tapping Accept you confirm you have read and agree to " +
                                   stringResource(R.string.app_name) +
                                   "'s Privacy Policy, Terms of Service, the Pawns Privacy Policy and Acceptable Use Policy, " +
                                   "and that you are at least 18 years of age and the primary account holder on the internet connection used by this device.",
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Less",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.primary,
                            modifier = Modifier.clickable { showFullConsent = false }
                        )
                    } else {
                        Text(
                            text = "By tapping Accept you agree to " +
                                   stringResource(R.string.app_name) +
                                   "'s Privacy Policy, Terms, and the Pawns policies, and confirm you're 18+ and the account holder on this connection.",
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp,
                            color = colors.onSurface
                        )
                        Text(
                            text = "More",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.primary,
                            modifier = Modifier.clickable { showFullConsent = true }
                        )
                    }
                }

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
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, colors.primary.copy(alpha = 0.5f)
                        ),
                        enabled = !isLoading
                    ) {
                        Text("⚙ Settings", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
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
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        enabled = !isLoading && apiKey.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = colors.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "What is Enhanced Reading Experience?",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Text(
            text = stringResource(R.string.app_name) + " uses Pawns technology to optimize content delivery " +
                   "by utilizing your device's idle network bandwidth. This helps us serve chapters and images faster " +
                   "while you enjoy your novels, manga, and documents.",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = colors.onSurface
        )
        Text(
            text = "How it works",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Text(
            text = "• Your device securely shares unused bandwidth with the Pawns network
" +
                   "• This accelerates content fetching for you and other readers
" +
                   "• Only runs when the app is active or in background (if enabled)
" +
                   "• You can disable this anytime in Settings > Privacy",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = colors.onSurface
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
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Your Privacy Matters",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Text(
            text = "We never access your personal reading history, bookmarks, or downloaded content. " +
                   "The bandwidth sharing is strictly limited to network relay functions. " +
                   "No reading data, account credentials, or personal files are ever transmitted.",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = colors.onSurface
        )
        Text(
            text = "External Policies",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
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
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Data We Protect",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Text(
            text = "• Your library and reading progress stays entirely on your device
" +
                   "• We do not collect IP addresses or browsing behavior
" +
                   "• Bandwidth sharing is anonymized and encrypted
" +
                   "• You can request data deletion at any time via " + stringResource(R.string.app_name) + " Legal Center",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = colors.onSurface
        )
        Text(
            text = "Security Measures",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Text(
            text = "All relay traffic is TLS-encrypted. Pawns operates under GDPR and CCPA compliance frameworks. " +
                   "Our local AI summarization feature remains fully offline — no data leaves your device for AI functions.",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = colors.onSurface
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
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "What Gets Shared",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Text(
            text = "When you opt in, your device participates as a node in the Pawns content delivery network. " +
                   "This means:

" +
                   "• Relaying encrypted content chunks to nearby readers (never your personal data)
" +
                   "• Contributing idle upload bandwidth during app background sessions
" +
                   "• Helping reduce server load for the entire " + stringResource(R.string.app_name) + " community",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = colors.onSurface
        )
        Text(
            text = "What Never Gets Shared",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Text(
            text = "• Your reading history, bookmarks, or notes
" +
                   "• Your personal files or downloaded novels
" +
                   "• Your identity or account information
" +
                   "• Your precise location data",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = colors.onSurface
        )
    }
}

@Composable
private fun PolicyLink(text: String, url: String) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = colors.primary,
        modifier = Modifier.clickable {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            context.startActivity(intent)
        }
    )
}
