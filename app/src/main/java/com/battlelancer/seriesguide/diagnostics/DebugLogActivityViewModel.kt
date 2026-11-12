// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.diagnostics

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class DebugLogActivityViewModel(application: Application) : AndroidViewModel(application) {

    data class DebugLogUiState(
        val isSaving: Boolean = false,
        val userMessage: String? = null
    )

    val uiState = MutableStateFlow(DebugLogUiState())

    private val debugLogBuffer = getApplication<SgApp>().appContainer.debugLogBuffer

    val logEntries: StateFlow<List<DebugLogEntry>> = debugLogBuffer.logEntriesLatestFirst()
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
            emptyList()
        )

    fun userMessageShown() {
        uiState.update {
            it.copy(userMessage = null)
        }
    }

    fun saveDebugLogToFile(uri: Uri) {
        uiState.update {
            it.copy(isSaving = true)
        }

        viewModelScope.launch(Dispatchers.IO) {
            debugLogBuffer
                .save(uri, object : DebugLogBuffer.OnSaveLogListener {
                    override fun onSuccess() {
                        uiState.value = DebugLogUiState(
                            isSaving = false,
                            userMessage = getApplication<Application>().getString(R.string.status_saving_file_success)
                        )
                    }

                    override fun onError() {
                        uiState.value = DebugLogUiState(
                            isSaving = false,
                            userMessage = getApplication<Application>().getString(R.string.status_saving_file_failed)
                        )
                    }
                })
        }
    }

}