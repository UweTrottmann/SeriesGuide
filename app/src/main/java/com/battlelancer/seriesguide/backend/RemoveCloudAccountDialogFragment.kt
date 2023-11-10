// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.backend

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialogFragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.backend.RemoveCloudAccountDialogFragment.AccountRemovedEvent
import com.battlelancer.seriesguide.backend.RemoveCloudAccountDialogFragment.CanceledEvent
import com.battlelancer.seriesguide.util.Errors
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.tasks.Tasks
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ExecutionException

/**
 * Confirms whether to obliterate a SeriesGuide cloud account. If removal is tried, posts result as
 * [AccountRemovedEvent]. If dialog is canceled, posts a [CanceledEvent].
 */
class RemoveCloudAccountDialogFragment : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.hexagon_remove_account_confirmation)
            .setPositiveButton(R.string.hexagon_remove_account) { _: DialogInterface?, _: Int ->
                SgApp.coroutineScope.launch {
                    RemoveHexagonAccountTask(requireContext()).run()
                }
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
                sendCanceledEvent()
            }
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        sendCanceledEvent()
    }

    private fun sendCanceledEvent() {
        EventBus.getDefault().post(CanceledEvent())
    }

    class RemoveHexagonAccountTask(context: Context) {

        private val context: Context = context.applicationContext
        private val hexagonTools: HexagonTools = getServicesComponent(context).hexagonTools()

        suspend fun run() {
            withContext(Dispatchers.IO) {
                removeJobSemaphore.withPermit {
                    val result = doInBackground()
                    onPostExecute(result)
                }
            }
        }

        private fun doInBackground(): Boolean {
            // remove account from hexagon
            try {
                val accountService = hexagonTools.buildAccountService()
                    ?: return false
                accountService.deleteData().execute()
            } catch (e: IOException) {
                Errors.logAndReportHexagon(ACTION_REMOVE_ACCOUNT, e)
                return false
            }

            // Delete Firebase account so other clients are signed out as well
            val task = AuthUI.getInstance().delete(context)
            try {
                Tasks.await(task)
            } catch (e: Exception) {
                // https://developers.google.com/android/reference/com/google/android/gms/tasks/Tasks#public-static-tresult-await-tasktresult-task
                if (e is InterruptedException) {
                    // Do not report thread interruptions, it's expected.
                    Timber.w(e, ACTION_REMOVE_ACCOUNT)
                } else {
                    val cause = if (e is ExecutionException) {
                        e.cause ?: e // The Task failed, getCause returns the original exception.
                    } else {
                        e // Unexpected exception.
                    }
                    val authEx = HexagonAuthError.build(ACTION_REMOVE_ACCOUNT, cause)
                    Errors.logAndReportHexagonAuthError(authEx)
                }
                return false
            }

            // disable Hexagon integration, remove local account data
            hexagonTools.removeAccountAndSetDisabled()
            return true
        }

        private fun onPostExecute(result: Boolean) {
            EventBus.getDefault().post(AccountRemovedEvent(result))
        }

        companion object {
            private const val ACTION_REMOVE_ACCOUNT = "remove account"
            private val removeJobSemaphore = Semaphore(permits = 1)
        }
    }

    class CanceledEvent

    class AccountRemovedEvent(val successful: Boolean) {
        /**
         * Display status toasts depending on the result.
         */
        fun handle(context: Context) {
            Toast.makeText(
                context,
                if (successful) R.string.hexagon_remove_account_success else R.string.hexagon_remove_account_failure,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}