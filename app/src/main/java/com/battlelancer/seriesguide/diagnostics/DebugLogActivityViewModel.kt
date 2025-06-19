// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.diagnostics

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.R
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

    fun updateDebugLogEntries() {
        viewModelScope.launch(Dispatchers.Default) {
            logEntries.value = DebugLogBuffer.getInstance(getApplication()).logBufferSnapshot()
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
            DebugLogBuffer.getInstance(getApplication())
                .save(uri, object : DebugLogBuffer.OnSaveLogListener {
                    override fun onSuccess() {
                        uiState.update {
                            it.copy(
                                isSaving = false,
                                userMessage = getApplication<Application>().getString(R.string.status_saving_file_success)
                            )
                        }
                    }

                    override fun onError() {
                        uiState.update {
                            it.copy(
                                isSaving = false,
                                userMessage = getApplication<Application>().getString(R.string.status_saving_file_failed)
                            )
                        }
                    }
                })
        }
    }

}