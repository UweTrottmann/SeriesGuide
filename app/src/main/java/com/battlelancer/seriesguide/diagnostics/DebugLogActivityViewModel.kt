// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.diagnostics

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DebugLogActivityViewModel(application: Application) : AndroidViewModel(application) {

    data class DebugLogUiState(
        val isSaving: Boolean = false,
        val userMessage: String? = null
    )

    val uiState = MutableStateFlow(DebugLogUiState())
    val logEntries = MutableStateFlow(emptyList<DebugLogEntry>())

    private val debugLogBuffer = getApplication<SgApp>().appContainer.debugLogBuffer

    fun updateDebugLogEntries() {
        viewModelScope.launch(Dispatchers.Default) {
            logEntries.value = debugLogBuffer.logBufferSnapshot()
        }
    }

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