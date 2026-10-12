// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.jobs

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.jobs.episodes.JobAction

/**
 * Fábrica concreta responsável por criar instâncias de [NetworkJob] para o serviço Cloud/Hexagon.
 *
 * Encapsula a criação dos jobs de episódios ([HexagonEpisodeJob]) e filmes ([HexagonMovieJob]),
 * bem como a verificação se a sincronização com o Hexagon está ativada.
 */
class HexagonJobFactory(
    private val hexagonToolsProvider: (Context) -> HexagonTools = {
        SgApp.getServicesComponent(it).hexagonTools()
    }
) : NetworkJobFactory {

    override val serviceName: String = "Hexagon"

    override fun isEnabled(context: Context): Boolean {
        return HexagonSettings.isEnabled(context)
    }

    override fun createJob(
        context: Context,
        action: JobAction,
        jobInfo: SgJobInfo,
        createdAt: Long
    ): NetworkJob? {
        val hexagonTools = hexagonToolsProvider(context)
        return when (action) {
            JobAction.EPISODE_COLLECTION,
            JobAction.EPISODE_WATCHED_FLAG -> {
                HexagonEpisodeJob(hexagonTools, action, jobInfo)
            }

            JobAction.MOVIE_COLLECTION_ADD,
            JobAction.MOVIE_COLLECTION_REMOVE,
            JobAction.MOVIE_WATCHLIST_ADD,
            JobAction.MOVIE_WATCHLIST_REMOVE,
            JobAction.MOVIE_WATCHED_SET,
            JobAction.MOVIE_WATCHED_REMOVE -> {
                HexagonMovieJob(hexagonTools, action, jobInfo)
            }

            else -> {
                null // Ação não suportada pelo Hexagon.
            }
        }
    }
}
