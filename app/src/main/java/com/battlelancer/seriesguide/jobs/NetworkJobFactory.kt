// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.jobs

import android.content.Context
import com.battlelancer.seriesguide.jobs.episodes.JobAction

/**
 * Interface de fábrica para criação de instâncias de [NetworkJob] para um serviço de sincronização remota.
 *
 * Implementa o padrão Factory Method / Abstract Factory para desacoplar o processamento de jobs
 * das implementações concretas dos serviços remotos (como Cloud/Hexagon e Trakt).
 */
interface NetworkJobFactory {

    /**
     * Identificador único do serviço remoto.
     */
    val serviceName: String

    /**
     * Retorna verdadeiro se o serviço estiver habilitado/conectado e pronto para processar jobs.
     */
    fun isEnabled(context: Context): Boolean

    /**
     * Cria um [NetworkJob] apropriado para a [action] e [jobInfo] fornecidos,
     * ou null se este serviço não suportar ou não manipular esta ação.
     */
    fun createJob(
        context: Context,
        action: JobAction,
        jobInfo: SgJobInfo,
        createdAt: Long
    ): NetworkJob?

}
