package io.aatricks.easyreader.ui.screens.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.aatricks.easyreader.domain.PawnsManager
import javax.inject.Inject

@HiltViewModel
class PawnsSettingsViewModel @Inject constructor(
    val pawnsManager: PawnsManager,
) : ViewModel()
