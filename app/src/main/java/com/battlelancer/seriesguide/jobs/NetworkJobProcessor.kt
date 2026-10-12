// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2017 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.jobs

import android.app.NotificationManager
import android.content.ContentProviderOperation
import android.content.Context
import android.content.OperationApplicationException
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Jobs
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.battlelancer.seriesguide.util.DBUtils
import com.uwetrottmann.androidutils.AndroidUtils
import timber.log.Timber
import java.nio.ByteBuffer

/**
 * Obtém jobs da tabela [Jobs], executando-os um a um a partir do mais antigo.
 * Com base no resultado do job, exibe uma notificação de erro e opcionalmente remove o job.
 * Se o job não puder ser removido, interrompe o processamento para tentar novamente na próxima sincronização.
 *
 * Utiliza o padrão Factory Method / Abstract Factory através de [NetworkJobFactory]
 * para desacoplar a criação e execução dos jobs de serviços de rede específicos.
 */
class NetworkJobProcessor(
    private val context: Context,
    private val jobFactories: List<NetworkJobFactory> = listOf(
        HexagonJobFactory(),
        TraktJobFactory()
    )
) {

    fun process() {
        // consulta os jobs
        val query = context.contentResolver
            .query(Jobs.CONTENT_URI, Jobs.PROJECTION, null, null, Jobs.SORT_OLDEST)
            ?: return  // consulta falhou

        // processa jobs, começando pelo mais antigo
        val jobsToRemove: MutableList<Long> = ArrayList()
        while (query.moveToNext()) {
            val jobId = query.getLong(0)
            val typeId = query.getInt(1)
            val action = JobAction.fromId(typeId)

            if (action != JobAction.UNKNOWN) {
                Timber.d("Running job %d: action = %s", jobId, action)

                val createdAt = query.getLong(2)
                val jobInfoArr = query.getBlob(3)
                val jobInfoBuffered = ByteBuffer.wrap(jobInfoArr)
                val jobInfo = SgJobInfo.getRootAsSgJobInfo(jobInfoBuffered)

                if (!doNetworkJob(jobId, action, createdAt, jobInfo)) {
                    Timber.e(
                        "Job %d failed, will retry with next sync, stop processing jobs",
                        jobId
                    )
                    break // aborta para evitar problemas de ordenação
                }
                Timber.d("Job %d completed or failed and can't retry, will remove", jobId)
            }
            jobsToRemove.add(jobId)
        }
        query.close()

        // remove jobs concluídos
        if (jobsToRemove.isNotEmpty()) {
            removeJobs(jobsToRemove)
        }
    }

    /**
     * Retorna true se o job puder ser removido, false se precisar ser tentado novamente mais tarde.
     * Itera polimorficamente pelas fábricas de jobs ativas.
     */
    private fun doNetworkJob(
        jobId: Long,
        action: JobAction,
        createdAt: Long,
        jobInfo: SgJobInfo
    ): Boolean {
        for (factory in jobFactories) {
            if (factory.isEnabled(context)) {
                if (!AndroidUtils.isNetworkConnected(context)) {
                    return false
                }
                val job = factory.createJob(context, action, jobInfo, createdAt)
                if (job != null) {
                    val result = job.execute(context)
                    // Notifica sobre falhas ou erros (ex.: aviso de item não encontrado no Trakt)
                    if (!result.successful || result.error != null) {
                        showNotification(jobId, createdAt, result)
                    }
                    if (!result.successful) {
                        return result.jobRemovable
                    }
                }
            }
        }
        return true
    }

    private fun showNotification(jobId: Long, jobCreatedAt: Long, result: NetworkJobResult) {
        if (result.action == null || result.error == null || result.item == null) {
            return // valores obrigatórios ausentes
        }
        Timber.e(
            "Job %d failed, notifying: error = %s, item = %s, action = %s",
            jobId,
            result.error,
            result.item,
            result.action
        )

        val nb = NotificationCompat.Builder(context, SgApp.NOTIFICATION_CHANNEL_ERRORS)
        NotificationSettings.setDefaultsForChannelErrors(context, nb)

        nb.setSmallIcon(R.drawable.ic_notification)
        // ex: 'Failed: Remove from collection · BoJack Horseman'
        nb.setContentTitle(
            context.getString(R.string.api_failed, "${result.action} · ${result.item}")
        )
        nb.setContentText(result.error)
        nb.setStyle(
            NotificationCompat.BigTextStyle().bigText(
                getErrorDetails(result.item, result.error, result.action, jobCreatedAt)
            )
        )
        nb.setContentIntent(result.contentIntent)
        nb.setAutoCancel(true)

        // notificação para cada job com falha
        val nm = context.getSystemService<NotificationManager>()
        nm?.notify(jobId.toString(), SgApp.NOTIFICATION_JOB_ID, nb.build())
    }

    private fun getErrorDetails(
        item: String, error: String,
        action: String, jobCreatedAt: Long
    ): String {
        val builder = StringBuilder()
        // constrói mensagem como:
        // 'Could not talk to server.
        // BoJack Horseman · Set watched · 5 sec ago'
        builder.append(error).append("\n").append(item).append(" · ").append(action)

        // anexa tempo se o job foi executado um tempo após ter sido criado
        val currentTimeMillis = System.currentTimeMillis()
        if (currentTimeMillis - jobCreatedAt > 3 * DateUtils.SECOND_IN_MILLIS) {
            builder.append(" · ")
            builder.append(
                DateUtils.getRelativeTimeSpanString(
                    jobCreatedAt,
                    currentTimeMillis, DateUtils.SECOND_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL
                )
            )
        }
        return builder.toString()
    }

    private fun removeJobs(jobsToRemove: List<Long>) {
        val batch = ArrayList<ContentProviderOperation>()
        for (jobId in jobsToRemove) {
            batch.add(
                ContentProviderOperation.newDelete(Jobs.buildJobUri(jobId))
                    .build()
            )
        }
        try {
            DBUtils.applyInSmallBatches(context, batch)
        } catch (e: OperationApplicationException) {
            Timber.e(e, "process: failed to delete completed jobs")
        }
    }

    /**
     * Se nenhum serviço remoto (Trakt ou Cloud) estiver conectado, limpa todos os jobs restantes.
     */
    fun removeObsoleteJobs(ignoreHexagonState: Boolean) {
        val anyServiceConnected = jobFactories.any { factory ->
            if (ignoreHexagonState && factory.serviceName == "Hexagon") {
                false
            } else {
                factory.isEnabled(context)
            }
        }
        if (anyServiceConnected) {
            return // Ainda conectado a pelo menos um serviço, não limpa os jobs.
        }
        context.contentResolver.delete(Jobs.CONTENT_URI, null, null)
    }
}