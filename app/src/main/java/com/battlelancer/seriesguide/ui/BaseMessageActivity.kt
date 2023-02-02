package com.battlelancer.seriesguide.ui

import android.content.Context
import android.view.View
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.jobs.FlagJob
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceActiveEvent
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceCompletedEvent
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * A [BaseActivity] that displays a permanent snack bar
 * if a service action is running (e.g. any Cloud or Trakt action).
 *
 * Service state is determined by the [ServiceActiveEvent]
 * and [ServiceCompletedEvent] events.
 *
 * Implementers should override [snackbarParentView] and at best
 * supply a CoordinatorLayout to attach it to.
 */
abstract class BaseMessageActivity : BaseActivity() {

    /**
     * Posted sticky while a service task is running.
     */
    class ServiceActiveEvent(
        private val shouldSendToHexagon: Boolean,
        private val shouldSendToTrakt: Boolean
    ) {
        fun shouldDisplayMessage(): Boolean {
            return shouldSendToHexagon || shouldSendToTrakt
        }

        fun getStatusMessage(context: Context): String {
            val statusText = StringBuilder()
            if (shouldSendToHexagon) {
                statusText.append(context.getString(R.string.hexagon_api_queued))
            }
            if (shouldSendToTrakt) {
                if (statusText.isNotEmpty()) {
                    statusText.append(" ")
                }
                statusText.append(context.getString(R.string.trakt_submitqueued))
            }
            return statusText.toString()
        }
    }

    /**
     * Posted once a service action has completed. It may not have been successful.
     */
    class ServiceCompletedEvent(
        val confirmationText: String?,
        var isSuccessful: Boolean,
        val flagJob: FlagJob?
    )

    private var snackbarProgress: Snackbar? = null

    override fun onStart() {
        super.onStart()
        val event = EventBus.getDefault().getStickyEvent(ServiceActiveEvent::class.java)
        handleServiceActiveEvent(event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventEpisodeTask(event: ServiceActiveEvent?) {
        handleServiceActiveEvent(event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventEpisodeTask(event: ServiceCompletedEvent) {
        if (event.confirmationText != null) {
            // show a confirmation/error text
            val snackbarCompleted = Snackbar.make(
                snackbarParentView, event.confirmationText,
                if (event.isSuccessful) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
            )
            // replaces any previous snackbar, including the indefinite progress one
            snackbarCompleted.show()
        } else {
            handleServiceActiveEvent(null)
        }
    }

    /**
     * Return a view to pass to [Snackbar.make], ideally a CoordinatorLayout.
     */
    open val snackbarParentView: View
        get() = findViewById(android.R.id.content)

    private fun handleServiceActiveEvent(event: ServiceActiveEvent?) {
        val currentSnackbar = snackbarProgress
        if (event != null && event.shouldDisplayMessage()) {
            val newSnackbar = if (currentSnackbar != null) {
                currentSnackbar.setText(event.getStatusMessage(this))
                currentSnackbar.duration = BaseTransientBottomBar.LENGTH_INDEFINITE
                currentSnackbar
            } else {
                Snackbar.make(
                    snackbarParentView,
                    event.getStatusMessage(this), Snackbar.LENGTH_INDEFINITE
                ).also { this.snackbarProgress = it }
            }
            newSnackbar.show()
        } else currentSnackbar?.dismiss()
    }
}