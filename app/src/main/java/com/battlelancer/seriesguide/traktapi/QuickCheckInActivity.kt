// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2018, 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.notifications.NotificationService
import com.battlelancer.seriesguide.traktapi.GenericCheckInDialogFragment.CheckInDialogDismissedEvent
import com.battlelancer.seriesguide.traktapi.TraktTask.TraktActionCompleteEvent
import com.battlelancer.seriesguide.ui.BaseThemeActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Blank activity, just used to quickly check into a show/episode.
 */
class QuickCheckInActivity : BaseThemeActivity() {

    override fun getCustomTheme(): Int {
        // make the activity show the wallpaper, nothing else
        return R.style.Theme_SeriesGuide_Wallpaper
    }

    override fun configureEdgeToEdge() {
        // Do nothing.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val episodeId = intent.getLongExtra(EXTRA_LONG_EPISODE_ID, 0)
        if (episodeId == 0L) {
            finish()
            return
        }

        // show check-in dialog
        if (!CheckInDialogFragment.show(this, supportFragmentManager, episodeId)) {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun onEvent(@Suppress("UNUSED_PARAMETER") event: CheckInDialogDismissedEvent?) {
        // if check-in dialog is dismissed, finish ourselves as well
        finish()
    }

    // Post on main thread so Toast works
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: TraktActionCompleteEvent) {
        if (event.traktAction != TraktAction.CHECKIN_EPISODE) {
            return
        }
        // display status toast about trakt action
        event.handle(this)

        // dismiss notification on successful check-in
        if (event.wasSuccessful) {
            NotificationService.deleteNotification(applicationContext, intent)
        }
    }

    companion object {

        private const val EXTRA_LONG_EPISODE_ID = "episode_id"

        @JvmStatic
        fun intent(episodeId: Long, context: Context): Intent {
            return Intent(context, QuickCheckInActivity::class.java)
                .putExtra(EXTRA_LONG_EPISODE_ID, episodeId)
                .addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
        }
    }
}