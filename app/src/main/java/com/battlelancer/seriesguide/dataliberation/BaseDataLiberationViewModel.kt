// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.dataliberation

import android.app.Application
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.AndroidViewModel
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Supports running an import task, provides state flows for progress and import summary.
 */
open class BaseDataLiberationViewModel(application: Application) : AndroidViewModel(application) {

    data class ImportSummaryUiState(
        val message: String?,
        val isError: Boolean,
        val handled: Boolean = false
    ) {
        // This introduces Android UI classes into imports, but is fine as the model is currently
        // not used in unit tests.
        fun applyTo(
            model: BaseDataLiberationViewModel,
            textViewSummary: TextView,
            scrollView: NestedScrollView
        ) {
            textViewSummary.apply {
                setTextAppearance(
                    if (isError) {
                        R.style.TextAppearance_SeriesGuide_Body2_Error
                    } else {
                        R.style.TextAppearance_SeriesGuide_Body2
                    }
                )
                text = message
            }
            if (!handled) {
                model.importSummaryState.update { it.copy(handled = true) }

                // Scroll the top of the summary into view
                scrollView.post {
                    scrollView.scrollTo(0, textViewSummary.top)
                }
            }
        }
    }

    val isInProgress = MutableStateFlow(false)
    val importSummaryState = MutableStateFlow(
        ImportSummaryUiState(
            message = null,
            isError = false,
            handled = true
        )
    )

    var dataLibJob: Job? = null

    private val isDataLibTaskNotCompleted: Boolean
        get() {
            val dataLibJob = dataLibJob
            return (dataLibJob != null && !dataLibJob.isCompleted)
        }

    override fun onCleared() {
        if (isDataLibTaskNotCompleted) {
            dataLibJob?.cancel(null)
        }
        dataLibJob = null
    }

    fun setInProgress(value: Boolean) {
        isInProgress.value = value
    }

    fun runImportTask(
        jsonImportTask: () -> JsonImportTask
    ) {
        setInProgress(true)

        // Use global coroutine scope: rather keep this view model alive for a bit than cancel an
        // in-progress import.
        dataLibJob = SgApp.coroutineScope.launch {
            val result = jsonImportTask().run()

            setInProgress(false)
            importSummaryState.value = ImportSummaryUiState(
                result.message,
                result.isError
            )
        }
    }

}