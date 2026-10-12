// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.jobs

import android.content.Context
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.google.common.truth.Truth.assertThat
import com.google.flatbuffers.FlatBufferBuilder
import org.junit.Test
import org.mockito.Mockito.mock
import java.nio.ByteBuffer

/**
 * Testes unitários para validar as implementações de [NetworkJobFactory]
 * ([HexagonJobFactory] e [TraktJobFactory]).
 */
class NetworkJobFactoryTest {

    private fun createDummyJobInfo(): SgJobInfo {
        val builder = FlatBufferBuilder(0)
        val jobInfo = SgJobInfo.createSgJobInfo(builder, 1, 0, 100, 1, 42)
        builder.finish(jobInfo)
        return SgJobInfo.getRootAsSgJobInfo(ByteBuffer.wrap(builder.sizedByteArray()))
    }

    @Test
    fun hexagonJobFactory_criaJobsCorretos() {
        val mockContext = mock(Context::class.java)
        val mockHexagonTools = mock(HexagonTools::class.java)
        val factory = HexagonJobFactory(hexagonToolsProvider = { mockHexagonTools })
        val jobInfo = createDummyJobInfo()

        // Valida nome do serviço
        assertThat(factory.serviceName).isEqualTo("Hexagon")

        // Valida criação de job de episódio
        val episodeJob = factory.createJob(mockContext, JobAction.EPISODE_WATCHED_FLAG, jobInfo, 0L)
        assertThat(episodeJob).isInstanceOf(HexagonEpisodeJob::class.java)

        val episodeCollectionJob = factory.createJob(mockContext, JobAction.EPISODE_COLLECTION, jobInfo, 0L)
        assertThat(episodeCollectionJob).isInstanceOf(HexagonEpisodeJob::class.java)

        // Valida criação de job de filme
        val movieJob = factory.createJob(mockContext, JobAction.MOVIE_WATCHLIST_ADD, jobInfo, 0L)
        assertThat(movieJob).isInstanceOf(HexagonMovieJob::class.java)

        // Valida ação não suportada
        val unsupportedJob = factory.createJob(mockContext, JobAction.UNKNOWN, jobInfo, 0L)
        assertThat(unsupportedJob).isNull()
    }

    @Test
    fun traktJobFactory_criaJobsCorretos() {
        val mockContext = mock(Context::class.java)
        val factory = TraktJobFactory()
        val jobInfo = createDummyJobInfo()

        // Valida nome do serviço
        assertThat(factory.serviceName).isEqualTo("Trakt")

        // Valida criação de job de episódio
        val episodeJob = factory.createJob(mockContext, JobAction.EPISODE_WATCHED_FLAG, jobInfo, 1000L)
        assertThat(episodeJob).isInstanceOf(TraktEpisodeJob::class.java)

        val episodeCollectionJob = factory.createJob(mockContext, JobAction.EPISODE_COLLECTION, jobInfo, 1000L)
        assertThat(episodeCollectionJob).isInstanceOf(TraktEpisodeJob::class.java)

        // Valida criação de job de filme
        val movieJob = factory.createJob(mockContext, JobAction.MOVIE_WATCHLIST_ADD, jobInfo, 1000L)
        assertThat(movieJob).isInstanceOf(TraktMovieJob::class.java)

        // Valida ação não suportada
        val unsupportedJob = factory.createJob(mockContext, JobAction.UNKNOWN, jobInfo, 1000L)
        assertThat(unsupportedJob).isNull()
    }
}
