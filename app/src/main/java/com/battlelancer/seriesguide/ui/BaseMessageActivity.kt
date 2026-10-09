// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2013 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.battlelancer.seriesguide.getSgAppContainer
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * A [BaseActivity] that displays a permanent snack bar
 * if a service action is running (e.g. any Cloud or Trakt action).
 *
 * Service state is determined by [ServiceTaskStatus.active]
 * and [ServiceTaskStatus.completed].
 *
 * Implementers should override [snackbarParentView] and at best
 * supply a CoordinatorLayout to attach it to.
 */
abstract class BaseMessageActivity : BaseActivity() {

    private var snackbarProgress: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceTaskStatus = getSgAppContainer().serviceTaskStatus
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    serviceTaskStatus.active.collect { handleServiceActive(it) }
                }
                launch {
                    serviceTaskStatus.completed.collect { handleServiceCompleted(it) }
                }
            }
        }
    }

    private fun handleServiceCompleted(completed: ServiceCompleted) {
        if (completed.confirmationText != null) {
            // show a confirmation/error text
            val snackbarCompleted = makeSnackbar(
                completed.confirmationText,
                if (completed.isSuccessful) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
            )
            // replaces any previous snackbar, including the indefinite progress one
            snackbarCompleted.show()
        } else {
            handleServiceActive(null)
        }
    }

    /**
     * Return a view to pass to [Snackbar.make] in [makeSnackbar], ideally a CoordinatorLayout.
     */
    open val snackbarParentView: View
        get() = findViewById(android.R.id.content)

    fun makeSnackbar(@StringRes message: Int, length: Int): Snackbar {
        return makeSnackbar(getString(message), length)
    }

    open fun makeSnackbar(message: String, length: Int): Snackbar {
        return Snackbar.make(snackbarParentView, message, length)
    }

    private fun handleServiceActive(active: ServiceActive?) {
        val currentSnackbar = snackbarProgress
        if (active != null && active.shouldDisplayMessage()) {
            val newSnackbar = if (currentSnackbar != null) {
                currentSnackbar.setText(active.getStatusMessage(this))
                currentSnackbar.duration = BaseTransientBottomBar.LENGTH_INDEFINITE
                currentSnackbar
            } else {
                makeSnackbar(active.getStatusMessage(this), Snackbar.LENGTH_INDEFINITE)
                    .also { this.snackbarProgress = it }
            }
            newSnackbar.show()
        } else currentSnackbar?.dismiss()
    }
}