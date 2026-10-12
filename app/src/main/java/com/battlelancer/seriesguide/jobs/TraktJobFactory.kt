// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.jobs

import android.content.Context
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.traktapi.TraktCredentials

/**
 * Fábrica concreta responsável por criar instâncias de [NetworkJob] para o serviço Trakt.
 *
 * Encapsula a criação dos jobs de episódios ([TraktEpisodeJob]) e filmes ([TraktMovieJob]),
 * bem como a verificação se o usuário possui credenciais válidas do Trakt configuradas.
 */
class TraktJobFactory : NetworkJobFactory {

    override val serviceName: String = "Trakt"

    override fun isEnabled(context: Context): Boolean {
        return TraktCredentials.get(context).hasCredentials()
    }

    override fun createJob(
        context: Context,
        action: JobAction,
        jobInfo: SgJobInfo,
        createdAt: Long
    ): NetworkJob? {
        return when (action) {
            JobAction.EPISODE_COLLECTION,
            JobAction.EPISODE_WATCHED_FLAG -> {
                TraktEpisodeJob(action, jobInfo, createdAt)
            }

            JobAction.MOVIE_COLLECTION_ADD,
            JobAction.MOVIE_COLLECTION_REMOVE,
            JobAction.MOVIE_WATCHLIST_ADD,
            JobAction.MOVIE_WATCHLIST_REMOVE,
            JobAction.MOVIE_WATCHED_SET,
            JobAction.MOVIE_WATCHED_REMOVE -> {
                TraktMovieJob(action, jobInfo, createdAt)
            }

            else -> {
                null // Ação não suportada pelo Trakt.
            }
        }
    }
}
