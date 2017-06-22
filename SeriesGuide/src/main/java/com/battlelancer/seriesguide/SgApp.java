package com.battlelancer.seriesguide;

import android.app.Application;
import android.content.ContentProvider;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.VmPolicy;
import com.battlelancer.seriesguide.modules.AppModule;
import com.battlelancer.seriesguide.modules.DaggerServicesComponent;
import com.battlelancer.seriesguide.modules.HttpClientModule;
import com.battlelancer.seriesguide.modules.ServicesComponent;
import com.battlelancer.seriesguide.modules.TmdbModule;
import com.battlelancer.seriesguide.modules.TraktModule;
import com.battlelancer.seriesguide.modules.TvdbModule;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.ThemeUtils;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.squareup.picasso.Picasso;
import io.fabric.sdk.android.Fabric;
import io.palaima.debugdrawer.timber.data.LumberYard;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusException;
import timber.log.Timber;

/**
 * Initializes logging and services.
 */
public class SgApp extends Application {

    public static final int NOTIFICATION_EPISODE_ID = 1;
    public static final int NOTIFICATION_SUBSCRIPTION_ID = 2;
    public static final int NOTIFICATION_TRAKT_AUTH_ID = 3;

    /**
     * Time calculation has changed, all episodes need re-calculation.
     */
    public static final int RELEASE_VERSION_12_BETA5 = 218;
    /**
     * Requires legacy cache clearing due to switch to Picasso for posters.
     */
    public static final int RELEASE_VERSION_16_BETA1 = 15010;
    /**
     * Requires trakt watched movie (re-)download.
     */
    public static final int RELEASE_VERSION_23_BETA4 = 15113;
    /**
     * Requires full show update due to switch to locally stored trakt ids.
     */
    public static final int RELEASE_VERSION_26_BETA3 = 15142;
    /**
     * Populate shows last watched field from activity table.
     */
    public static final int RELEASE_VERSION_34_BETA4 = 15223;
    /**
     * Switched to Google Sign-In: notify existing Cloud users to sign in again.
     */
    public static final int RELEASE_VERSION_36_BETA2 = 15241;

    /**
     * The content authority used to identify the SeriesGuide {@link ContentProvider}
     */
    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";

    private static ServicesComponent servicesComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            enableStrictMode();
        }

        // set up logging first so crashes during initialization are caught
        initializeLogging();

        AndroidThreeTen.init(this);
        initializeEventBus();
        initializePicasso();

        // Load the current theme into a global variable
        ThemeUtils.updateTheme(DisplaySettings.getThemeIndex(this));
    }

    private void initializeLogging() {
        if (BuildConfig.DEBUG) {
            // debug drawer logging
            LumberYard lumberYard = LumberYard.getInstance(this);
            lumberYard.cleanUp();
            Timber.plant(lumberYard.tree());
            // detailed logcat logging
            Timber.plant(new Timber.DebugTree());
        } else {
            // crash and error reporting
            Timber.plant(new AnalyticsTree(this));
            if (!Fabric.isInitialized()) {
                Fabric.with(this, new Crashlytics());
            }
        }

        // Ensure GA opt-out
        GoogleAnalytics.getInstance(this).setAppOptOut(AppSettings.isGaAppOptOut(this));
        if (BuildConfig.DEBUG) {
            GoogleAnalytics.getInstance(this).setDryRun(true);
        }
        // Initialize tracker
        Analytics.getTracker(this);
    }

    private void initializeEventBus() {
        try {
            EventBus.builder()
                    .logNoSubscriberMessages(BuildConfig.DEBUG)
                    .addIndex(new SgEventBusIndex())
                    .installDefaultEventBus();
        } catch (EventBusException ignored) {
            // instance was already set
        }
    }

    private void initializePicasso() {
        Picasso picasso = new Picasso.Builder(this)
                .downloader(new OkHttp3Downloader(this))
                .build();
        try {
            Picasso.setSingletonInstance(picasso);
        } catch (IllegalStateException ignored) {
            // instance was already set
        }
    }

    public static synchronized ServicesComponent getServicesComponent(Context context) {
        if (servicesComponent == null) {
            servicesComponent = DaggerServicesComponent.builder()
                    .appModule(new AppModule(context))
                    .httpClientModule(new HttpClientModule())
                    .tmdbModule(new TmdbModule())
                    .traktModule(new TraktModule())
                    .tvdbModule(new TvdbModule())
                    .build();
        }
        return servicesComponent;
    }

    /**
     * Used to enable {@link StrictMode} for debug builds.
     */
    private static void enableStrictMode() {
        // Enable StrictMode
        final ThreadPolicy.Builder threadPolicyBuilder = new ThreadPolicy.Builder();
        threadPolicyBuilder.detectAll();
        threadPolicyBuilder.penaltyLog();
        StrictMode.setThreadPolicy(threadPolicyBuilder.build());

        // Policy applied to all threads in the virtual machine's process
        final VmPolicy.Builder vmPolicyBuilder = new VmPolicy.Builder();
        vmPolicyBuilder.detectAll();
        vmPolicyBuilder.penaltyLog();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            vmPolicyBuilder.detectLeakedRegistrationObjects();
        }
        StrictMode.setVmPolicy(vmPolicyBuilder.build());
    }
}
