// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools

/**
 * Listens to notification actions, currently only setting an episode watched.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (NotificationService.ACTION_CLEARED == intent.action) {
            NotificationService.handleDeleteIntent(context, intent)
            return
        }
        val episodeId = intent.getLongExtra(EXTRA_LONG_EPISODE_ID, 0)
        if (episodeId <= 0) {
            return  // not notification set watched action
        }

        // mark episode watched
        EpisodeTools.episodeWatched(context, episodeId, EpisodeFlags.WATCHED)

        // dismiss the notification
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(SgApp.NOTIFICATION_EPISODE_ID)
        // replicate delete intent
        NotificationService.handleDeleteIntent(context, intent)
    }

    companion object {

        private const val EXTRA_LONG_EPISODE_ID = "episode_id"

        fun intent(episodeId: Long, context: Context): Intent {
            return Intent(context, NotificationActionReceiver::class.java)
                .putExtra(EXTRA_LONG_EPISODE_ID, episodeId)
        }
    }
}