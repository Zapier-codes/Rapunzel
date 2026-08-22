package io.aatricks.easyreader.consent

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ConsentState(
    val hasConsented: Boolean = false,
    val hasDismissed: Boolean = false,
    val consentVersion: Int = CURRENT_CONSENT_VERSION
)

private const val PREFS_NAME = "rapunzel_consent_prefs"
private const val KEY_HAS_CONSENTED = "has_consented"
private const val KEY_HAS_DISMISSED = "has_dismissed"
private const val KEY_CONSENT_VERSION = "consent_version"
private const val CURRENT_CONSENT_VERSION = 1

@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _consentState = MutableStateFlow(loadState())
    val consentState: StateFlow<ConsentState> = _consentState.asStateFlow()

    private fun loadState(): ConsentState {
        return ConsentState(
            hasConsented = prefs.getBoolean(KEY_HAS_CONSENTED, false),
            hasDismissed = prefs.getBoolean(KEY_HAS_DISMISSED, false),
            consentVersion = prefs.getInt(KEY_CONSENT_VERSION, 0)
        )
    }

    fun setConsented(value: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_CONSENTED, value).apply()
        _consentState.value = _consentState.value.copy(hasConsented = value)
    }

    fun setDismissed(value: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_DISMISSED, value).apply()
        _consentState.value = _consentState.value.copy(hasDismissed = value)
    }
}
