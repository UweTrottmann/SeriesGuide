// SPDX-License-Identifier: Apache-2.0
// Copyright 2011-2025 Uwe Trottmann
// Copyright 2013 Andrew Neal

package com.battlelancer.seriesguide

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.annotation.RequiresApi
import com.battlelancer.seriesguide.diagnostics.DebugLogBuffer
import com.battlelancer.seriesguide.modules.AppModule
import com.battlelancer.seriesguide.modules.DaggerServicesComponent
import com.battlelancer.seriesguide.modules.HttpClientModule
import com.battlelancer.seriesguide.modules.HttpClientModule.Companion.trustLetsEncryptAndroidNOrLower
import com.battlelancer.seriesguide.modules.ServicesComponent
import com.battlelancer.seriesguide.modules.TmdbModule
import com.battlelancer.seriesguide.modules.TraktModule
import com.battlelancer.seriesguide.notifications.NotificationService
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.SgPicassoRequestHandler
import com.battlelancer.seriesguide.util.ThemeUtils
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import okhttp3.OkHttpClient
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.EventBusException
import timber.log.Timber
import java.util.concurrent.Executors


/**
 * Initializes logging and services.
 */
class SgApp : Application() {

    companion object {

        const val JOB_ID_EXTENSION_AMAZON = 1001
        const val JOB_ID_EXTENSION_GOOGLE_PLAY = 1002
        const val JOB_ID_EXTENSION_VODSTER = 1003
        const val JOB_ID_EXTENSION_WEBSEARCH = 1004
        const val JOB_ID_EXTENSION_YOUTUBE = 1005
        const val JOB_ID_EXTENSION_ACTIONS_SERVICE = 1006

        const val BASE_NOTIFICATION_ID_EPISODES = 100
        const val NOTIFICATION_EPISODE_ID = 1
        const val NOTIFICATION_SUBSCRIPTION_ID = 2
        const val NOTIFICATION_TRAKT_AUTH_ID = 3
        const val NOTIFICATION_JOB_ID = 4

        const val NOTIFICATION_CHANNEL_EPISODES = "episodes"
        const val NOTIFICATION_CHANNEL_ERRORS = "errors"

        const val NOTIFICATION_GROUP_EPISODES = "com.uwetrottmann.seriesguide.EPISODES"

        const val RELEASE_VERSION_16_BETA1 = 15010

        const val RELEASE_VERSION_23_BETA4 = 15113

        const val RELEASE_VERSION_36_BETA2 = 15241

        const val RELEASE_VERSION_40_BETA4 = 1502803

        const val RELEASE_VERSION_50_1 = 2105008

        const val RELEASE_VERSION_51_BETA4 = 2105103

        const val RELEASE_VERSION_59_BETA1 = 2105900

        const val RELEASE_VERSION_72_0_1 = 2107201

        const val RELEASE_VERSION_2024_3_5 = 21240305

        const val RELEASE_VERSION_2025_1_1 = 21250102

        /**
         * The content authority used to identify the SeriesGuide [android.content.ContentProvider].
         */
        const val CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"

        /**
         * A global [CoroutineScope] to avoid using [kotlinx.coroutines.GlobalScope]
         * and leave open the possibility of exception handling and other things.
         * Uses [Dispatchers.Default] by default.
         */
        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** Executes one coroutine at a time. But does not guarantee order if they suspend. */
        val SINGLE = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        private var servicesComponent: ServicesComponent? = null

        @JvmStatic
        @Synchronized
        fun getServicesComponent(context: Context): ServicesComponent {
            if (servicesComponent == null) {
                servicesComponent = DaggerServicesComponent.builder()
                    .appModule(AppModule(context))
                    .httpClientModule(HttpClientModule())
                    .tmdbModule(TmdbModule())
                    .traktModule(TraktModule())
                    .build()
            }
            return servicesComponent!!
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        // set up logging first so crashes during initialization are caught
        initializeLogging()

        AndroidThreeTen.init(this)
        initializeEventBus()
        if (AndroidUtils.isAtLeastOreo) {
            initializeNotificationChannels()
        }

        // Load the current theme into a global variable
        ThemeUtils.updateTheme(DisplaySettings.getThemeIndex(this))

        // Update security provider before building HTTP client (for Picasso and in HttpClientModule).
        initializeSecurityProvider()
        initializePicasso()
    }

    /**
     * Tell Google Play Services to update the security provider.
     * This enables older devices to keep connecting to APIs and image servers
     * by use modern encryption.
     */
    private fun initializeSecurityProvider() {
        // TODO Figure out how to do this async
        //  (either Picasso and HttpClientModule need to wait, or replace them on success?).
//        ProviderInstaller.installIfNeededAsync(applicationContext, providerInstallListener)
        try {
            ProviderInstaller.installIfNeeded(applicationContext)
            Timber.v("Successfully installed GMS security provider")
        } catch (e: GooglePlayServicesRepairableException) {
            Timber.e("Failed to install GMS security provider ${e.connectionStatusCode}")
        } catch (e: GooglePlayServicesNotAvailableException) {
            Timber.e("Failed to install GMS security provider ${e.errorCode}")
        }
    }

    private fun initializeLogging() {
        // Note: Firebase Crashlytics is automatically initialized through its content provider.
        // Pass current enabled state to Crashlytics (e.g. in case app was restored from backup).
        val isSendErrors = AppSettings.isSendErrorReports(this)
        Timber.d("Turning error reporting %s", if (isSendErrors) "ON" else "OFF")
        Errors.getReporter()?.setCrashlyticsCollectionEnabled(isSendErrors)

        if (AppSettings.isUserDebugModeEnabled(this)) {
            // debug logging
            DebugLogBuffer.getInstance(this).enable()
        }
        if (BuildConfig.DEBUG) {
            // detailed logcat logging
            Timber.plant(Timber.DebugTree())
        }
        // crash and error reporting
        Timber.plant(AnalyticsTree())
    }

    private fun initializeEventBus() {
        try {
            EventBus.builder()
                .logNoSubscriberMessages(BuildConfig.DEBUG)
                .addIndex(SgEventBusIndex())
                .installDefaultEventBus()
        } catch (ignored: EventBusException) {
            // instance was already set
        }

    }

    private fun initializePicasso() {
        val builder = OkHttpClient.Builder()
            .cache(HttpClientModule.getImageDiskCache(this))
            .trustLetsEncryptAndroidNOrLower()
        val downloader = OkHttp3Downloader(builder.build())
        val picasso = Picasso.Builder(this)
            .downloader(downloader)
            .addRequestHandler(SgPicassoRequestHandler(downloader, this))
            .build()
        try {
            Picasso.setSingletonInstance(picasso)
        } catch (ignored: IllegalStateException) {
            // instance was already set
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun initializeNotificationChannels() {
        // note: sound is on by default
        val channels = ArrayList<NotificationChannel>()
        val colorAccent = getColor(R.color.sg_color_primary)

        val channelEpisodes = NotificationChannel(
            NOTIFICATION_CHANNEL_EPISODES,
            getString(R.string.episodes),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channelEpisodes.description = getString(R.string.pref_notificationssummary)
        channelEpisodes.enableLights(true)
        channelEpisodes.lightColor = colorAccent
        channelEpisodes.vibrationPattern = NotificationService.VIBRATION_PATTERN
        channels.add(channelEpisodes)

        val channelJobs = NotificationChannel(
            NOTIFICATION_CHANNEL_ERRORS,
            getString(R.string.pref_notification_channel_errors),
            NotificationManager.IMPORTANCE_HIGH
        )
        channelJobs.enableLights(true)
        channelEpisodes.lightColor = colorAccent
        channels.add(channelJobs)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        manager?.createNotificationChannels(channels)
    }

    /**
     * Used to enable [StrictMode] for debug builds.
     */
    private fun enableStrictMode() {
        // Enable StrictMode
        with(ThreadPolicy.Builder()) {
            penaltyLog()

            // exclude disk reads
            detectDiskWrites()
            detectNetwork()
            detectCustomSlowCalls()
            if (AndroidUtils.isMarshmallowOrHigher) {
                detectResourceMismatches()
            }
            if (AndroidUtils.isAtLeastOreo) {
                detectUnbufferedIo()
            }
            StrictMode.setThreadPolicy(build())
        }

        // custom config to disable detecting untagged sockets
        with(VmPolicy.Builder()) {
            penaltyLog()

            detectLeakedSqlLiteObjects()
            detectActivityLeaks()
            detectLeakedClosableObjects()
            detectLeakedRegistrationObjects()
            detectFileUriExposure()
            if (AndroidUtils.isAtLeastOreo) {
                detectContentUriWithoutPermission()
            }
            // Check for optional safer intents changes on Android 15
            // This only affects extensions, where the receiver should declare the subscribe and
            // update actions that SeriesGuide uses in the broadcast intent.
            // https://developer.android.com/about/versions/15/behavior-changes-15#safer-intents
            if (AndroidUtils.isAtLeastS) {
                detectUnsafeIntentLaunch()
            }
            // Policy applied to all threads in the virtual machine's process
            StrictMode.setVmPolicy(build())
        }
    }
}
