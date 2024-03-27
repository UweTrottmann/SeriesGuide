// SPDX-License-Identifier: Apache-2.0
// Copyright 2016-2018, 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        NotificationService.deleteNotification(context, intent)
    }

    companion object {

        private const val EXTRA_LONG_EPISODE_ID = "episode_id"

        fun intent(episodeId: Long, context: Context): Intent {
            return Intent(context, NotificationActionReceiver::class.java)
                .putExtra(EXTRA_LONG_EPISODE_ID, episodeId)
        }
    }
}