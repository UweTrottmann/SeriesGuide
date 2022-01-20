package com.battlelancer.seriesguide.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.core.content.getSystemService
import com.battlelancer.seriesguide.util.PendingIntentCompat
import timber.log.Timber

class NotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // postpone notification service launch a minute,
            // we don't want to slow down booting
            Timber.d("Postponing notifications service launch")

            val i = Intent(context, NotificationAlarmReceiver::class.java)
            // Mutable intent because used to schedule alarm.
            val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntentCompat.flagMutable)
            val am = context.getSystemService<AlarmManager>()
            am?.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + DateUtils.MINUTE_IN_MILLIS, pi
            )
        } else {
            // run the notification service right away
            Timber.d("Run notifications service right away")

            // as jobs are not allowed to run while the device is idle, use an AsyncTask instead
            val pendingResult = goAsync()
            val notificationService = NotificationService(context)
            @SuppressLint("StaticFieldLeak") val task: AsyncTask<Void, Void, Void> =
                object : AsyncTask<Void, Void, Void>() {
                    override fun doInBackground(vararg voids: Void): Void? {
                        notificationService.run()
                        pendingResult.finish()
                        return null
                    }
                }
            // run in serial to avoid alarm scheduling conflicts
            // separate serial executor as AsyncTask one might be busy longer than onReceive timeout
            task.executeOnExecutor(NotificationService.SERIAL_EXECUTOR)
        }
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, NotificationAlarmReceiver::class.java)
        }
    }
}