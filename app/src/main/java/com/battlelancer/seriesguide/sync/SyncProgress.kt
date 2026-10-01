// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2017 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.sync

import android.content.Context
import androidx.annotation.StringRes
import com.battlelancer.seriesguide.R
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.util.LinkedList

class SyncProgress {

    enum class Step(
        @param:StringRes val serviceRes: Int,
        @param:StringRes val typeRes: Int
    ) {
        TMDB(R.string.tmdb, 0),
        HEXAGON_EPISODES(R.string.hexagon, R.string.episodes),
        HEXAGON_SHOWS(R.string.hexagon, R.string.shows),
        HEXAGON_MOVIES(R.string.hexagon, R.string.movies),
        HEXAGON_LISTS(R.string.hexagon, R.string.lists),
        TRAKT(R.string.trakt, 0),
        TRAKT_EPISODES(R.string.trakt, R.string.episodes),
        TRAKT_RATINGS(R.string.trakt, R.string.ratings),
        TRAKT_NOTES(R.string.trakt, R.string.title_notes),
        TRAKT_MOVIES(R.string.trakt, R.string.movies)
    }

    class SyncEvent internal constructor(
        private val step: Step?,
        private val stepsWithError: List<Step>,
        private val importantErrorOrNull: String?
    ) {
        val isSyncing: Boolean
            get() = step != null

        val isFinishedWithError: Boolean
            get() = stepsWithError.isNotEmpty()

        fun getDescription(context: Context): String {
            val statusText = StringBuilder(context.getString(R.string.sync_and_update))

            val stepToDisplay = getStepToDisplay()
            if (stepToDisplay != null) {
                statusText.append(" - ")
                statusText.append(context.getString(stepToDisplay.serviceRes))
                if (stepToDisplay.typeRes != 0) {
                    statusText.append(" - ")
                    statusText.append(context.getString(stepToDisplay.typeRes))
                }
            }

            if (importantErrorOrNull != null) {
                statusText.append(" - ").append(importantErrorOrNull)
            }

            return statusText.toString()
        }

        private fun getStepToDisplay(): Step? {
            return step
            // display first step that had error
                ?: stepsWithError.firstOrNull()
        }
    }

    private val stepsWithError: MutableList<Step> = LinkedList()
    private var currentStep: Step? = null
    private var importantErrorOrNull: String? = null

    internal fun publish(step: Step) {
        currentStep = step
        EventBus.getDefault().postSticky(SyncEvent(step, stepsWithError, importantErrorOrNull))
        Timber.d("Syncing: %s...", step.name)
    }

    /**
     * Record an error for the last published step.
     */
    internal fun recordError() {
        currentStep?.let {
            stepsWithError.add(it)
            Timber.d("Syncing: %s...FAILED", it.name)
        }
    }

    /**
     * Set message to be appended to the step description once
     * [publish] or [publishFinished] is called.
     * Does nothing if this was already called.
     */
    fun setImportantErrorIfNone(message: String) {
        if (importantErrorOrNull == null) {
            importantErrorOrNull = message
        }
    }

    internal fun publishFinished() {
        EventBus.getDefault().postSticky(SyncEvent(null, stepsWithError, importantErrorOrNull))
    }
}
