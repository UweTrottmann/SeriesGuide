package com.battlelancer.seriesguide

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.support.annotation.RequiresApi
import com.battlelancer.seriesguide.extensions.ExtensionManager
import com.battlelancer.seriesguide.modules.AppModule
import com.battlelancer.seriesguide.modules.DaggerServicesComponent
import com.battlelancer.seriesguide.modules.HttpClientModule
import com.battlelancer.seriesguide.modules.ServicesComponent
import com.battlelancer.seriesguide.modules.TmdbModule
import com.battlelancer.seriesguide.modules.TraktModule
import com.battlelancer.seriesguide.modules.TvdbModule
import com.battlelancer.seriesguide.service.NotificationService
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.SgPicassoRequestHandler
import com.battlelancer.seriesguide.util.ThemeUtils
import com.crashlytics.android.core.CrashlyticsCore
import com.google.android.gms.analytics.GoogleAnalytics
import com.jakewharton.picasso.OkHttp3Downloader
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.picasso.Picasso
import io.fabric.sdk.android.Fabric
import io.palaima.debugdrawer.timber.data.LumberYard
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.EventBusException
import timber.log.Timber
import java.util.ArrayList

/**
 * Initializes logging and services.
 */
class SgApp : Application() {

    companion object {

        @JvmField
        val JOB_ID_EXTENSION_AMAZON = 1001
        @JvmField
        val JOB_ID_EXTENSION_GOOGLE_PLAY = 1002
        @JvmField
        val JOB_ID_EXTENSION_VODSTER = 1003
        @JvmField
        val JOB_ID_EXTENSION_WEBSEARCH = 1004
        @JvmField
        val JOB_ID_EXTENSION_YOUTUBE = 1005
        @JvmField
        val JOB_ID_EXTENSION_ACTIONS_SERVICE = 1006
        @JvmField
        val JOB_ID_UNWATCHED_UPDATER_SERVICE = 1007

        @JvmField
        val NOTIFICATION_EPISODE_ID = 1
        @JvmField
        val NOTIFICATION_SUBSCRIPTION_ID = 2
        @JvmField
        val NOTIFICATION_TRAKT_AUTH_ID = 3
        @JvmField
        val NOTIFICATION_JOB_ID = 4

        @JvmField
        val NOTIFICATION_CHANNEL_EPISODES = "episodes"
        @JvmField
        val NOTIFICATION_CHANNEL_ERRORS = "errors"

        /**
         * Time calculation has changed, all episodes need re-calculation.
         */
        @JvmField
        val RELEASE_VERSION_12_BETA5 = 218
        /**
         * Requires legacy cache clearing due to switch to Picasso for posters.
         */
        @JvmField
        val RELEASE_VERSION_16_BETA1 = 15010
        /**
         * Requires trakt watched movie (re-)download.
         */
        @JvmField
        val RELEASE_VERSION_23_BETA4 = 15113
        /**
         * Requires full show update due to switch to locally stored trakt ids.
         */
        @JvmField
        val RELEASE_VERSION_26_BETA3 = 15142
        /**
         * Populate shows last watched field from activity table.
         */
        @JvmField
        val RELEASE_VERSION_34_BETA4 = 15223
        /**
         * Switched to Google Sign-In: notify existing Cloud users to sign in again.
         */
        @JvmField
        val RELEASE_VERSION_36_BETA2 = 15241
        /**
         * Extensions API v2, old extensions no longer work.
         */
        @JvmField
        val RELEASE_VERSION_40_BETA4 = 1502803
        /**
         * ListWidgetProvider alarm intent is now explicit.
         */
        @JvmField
        val RELEASE_VERSION_40_BETA6 = 1502805

        /**
         * The content authority used to identify the SeriesGuide [android.content.ContentProvider].
         */
        @JvmField
        val CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"

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
                        .tvdbModule(TvdbModule())
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
        initializePicasso()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initializeNotificationChannels()
        }

        // Load the current theme into a global variable
        ThemeUtils.updateTheme(DisplaySettings.getThemeIndex(this))

        ExtensionManager.get().checkEnabledExtensions(this)
    }

    private fun initializeLogging() {
        // set up reporting tools first
        if (!Fabric.isInitialized()) {
            // use core kit only, Crashlytics kit also adds Answers and Beta kit
            Fabric.with(this, CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
        }
        // Ensure GA opt-out
        GoogleAnalytics.getInstance(this).appOptOut = AppSettings.isGaAppOptOut(this)
        if (BuildConfig.DEBUG) {
            GoogleAnalytics.getInstance(this).setDryRun(true)
        }
        // Initialize tracker
        Analytics.getTracker(this)

        if (BuildConfig.DEBUG) {
            // debug drawer logging
            val lumberYard = LumberYard.getInstance(this)
            lumberYard.cleanUp()
            Timber.plant(lumberYard.tree())
            // detailed logcat logging
            Timber.plant(Timber.DebugTree())
        }
        // crash and error reporting
        Timber.plant(AnalyticsTree(this))
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
        val downloader = OkHttp3Downloader(this)
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
        val colorAccent = getColor(R.color.accent_primary)

        val channelEpisodes = NotificationChannel(NOTIFICATION_CHANNEL_EPISODES,
                getString(R.string.episodes),
                NotificationManager.IMPORTANCE_DEFAULT)
        channelEpisodes.description = getString(R.string.pref_notificationssummary)
        channelEpisodes.enableLights(true)
        channelEpisodes.lightColor = colorAccent
        channelEpisodes.vibrationPattern = NotificationService.VIBRATION_PATTERN
        channels.add(channelEpisodes)

        val channelJobs = NotificationChannel(NOTIFICATION_CHANNEL_ERRORS,
                getString(R.string.pref_notification_channel_errors),
                NotificationManager.IMPORTANCE_HIGH)
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
            detectAll()
            penaltyLog()
            StrictMode.setThreadPolicy(build())
        }

        // custom config to disable detecting untagged sockets
        with(VmPolicy.Builder()) {
            penaltyLog()

            detectLeakedSqlLiteObjects()
            detectActivityLeaks()
            detectLeakedClosableObjects()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                detectLeakedRegistrationObjects()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                detectFileUriExposure()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                detectContentUriWithoutPermission()
            }
            // Policy applied to all threads in the virtual machine's process
            StrictMode.setVmPolicy(build())
        }
    }
}
