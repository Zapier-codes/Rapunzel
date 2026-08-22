package io.aatricks.easyreader.consent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PawnsBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var consentManager: ConsentManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val hasConsented = consentManager.consentState.value.hasConsented
        if (!hasConsented) {
            Log.d("PawnsBootReceiver", "User has not consented, skipping Pawns init")
            return
        }

        val apiKey = io.aatricks.easyreader.BuildConfig.PAWNS_API_KEY
        PawnsManager.initialize(context, apiKey)
        Log.i("PawnsBootReceiver", "Pawns SDK reinitialized after boot")
    }
}
