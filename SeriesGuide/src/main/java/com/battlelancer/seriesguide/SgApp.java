package com.battlelancer.seriesguide;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
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
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.ThemeUtils;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import io.fabric.sdk.android.Fabric;
import net.danlew.android.joda.JodaTimeAndroid;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusException;
import timber.log.Timber;

/**
 * Initializes settings and services and on pre-ICS implements actions for low memory state.
 *
 * @author Uwe Trottmann
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
     * The content authority used to identify the SeriesGuide {@link ContentProvider}
     */
    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";

    private ServicesComponent servicesComponent;
    private MovieTools movieTools;
    private ShowTools showTools;

    @Override
    public void onCreate() {
        super.onCreate();

        // logging setup
        if (BuildConfig.DEBUG) {
            // detailed logcat logging
            Timber.plant(new Timber.DebugTree());
        } else {
            // crash and error reporting
            Timber.plant(new AnalyticsTree(this));
            if (!Fabric.isInitialized()) {
                Fabric.with(this, new Crashlytics());
            }
        }

        // initialize EventBus
        try {
            EventBus.builder().addIndex(new SgEventBusIndex()).installDefaultEventBus();
        } catch (EventBusException e) {
            // looks like instance sometimes still lingers around,
            // so far happening from services, though no exact cause found, yet
            Timber.e(e, "EventBus already installed.");
        }

        // initialize joda-time-android
        JodaTimeAndroid.init(this);

        // Load the current theme into a global variable
        ThemeUtils.updateTheme(DisplaySettings.getThemeIndex(this));

        // Ensure GA opt-out
        GoogleAnalytics.getInstance(this).setAppOptOut(AppSettings.isGaAppOptOut(this));
        if (BuildConfig.DEBUG) {
            GoogleAnalytics.getInstance(this).setDryRun(true);
        }
        // Initialize tracker
        Analytics.getTracker(this);

        enableStrictMode();

        servicesComponent = DaggerServicesComponent.builder()
                .appModule(new AppModule(this))
                .httpClientModule(new HttpClientModule())
                .tmdbModule(new TmdbModule())
                .traktModule(new TraktModule())
                .tvdbModule(new TvdbModule())
                .build();
    }

    public static SgApp from(Activity activity) {
        return (SgApp) activity.getApplication();
    }

    public ServicesComponent getServicesComponent() {
        return servicesComponent;
    }

    public synchronized MovieTools getMovieTools() {
        if (movieTools == null) {
            movieTools = new MovieTools(this);
        }
        return movieTools;
    }

    public synchronized ShowTools getShowTools() {
        if (showTools == null) {
            showTools = new ShowTools(this);
        }
        return showTools;
    }

    /**
     * Used to enable {@link StrictMode} for debug builds.
     */
    private static void enableStrictMode() {
        if (!BuildConfig.DEBUG) {
            return;
        }
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
