package com.battlelancer.seriesguide.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.AnyRes;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;
import com.battlelancer.seriesguide.Analytics;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.service.OnAlarmReceiver;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.UpdateSettings;
import com.google.android.gms.analytics.HitBuilders;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.io.File;
import java.net.UnknownHostException;
import timber.log.Timber;

/**
 * Various generic helper methods that do not fit other tool categories.
 */
public class Utils {

    private Utils() {
        // prevent instantiation
    }

    public static String getVersion(Context context) {
        String version;
        try {
            version = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            version = "UnknownVersion";
        }
        return version;
    }

    /**
     * Return a version string like "v42 (Database v42)".
     */
    public static String getVersionString(Context context) {
        if (BuildConfig.DEBUG) {
            return context.getString(R.string.format_version_debug, getVersion(context),
                    SeriesGuideDatabase.DATABASE_VERSION, BuildConfig.VERSION_CODE);
        } else {
            return context.getString(R.string.format_version, getVersion(context),
                    SeriesGuideDatabase.DATABASE_VERSION);
        }
    }

    /**
     * Run the notification service to display and (re)schedule upcoming episode alarms.
     */
    public static void runNotificationService(Context context) {
        Intent i = new Intent(context, NotificationService.class);
        context.startService(i);
    }

    /**
     * Run the notification service delayed by a minute to display and (re)schedule upcoming episode
     * alarms.
     */
    public static void runNotificationServiceDelayed(Context context) {
        Intent i = new Intent(context, OnAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + DateUtils.MINUTE_IN_MILLIS, pi);
        }
    }

    /**
     * Returns if the user should get access to paid features.
     */
    public static boolean hasAccessToX(Context context) {
        // debug builds, installed X Pass or subscription unlock all features
        // Amazon version only supports X pass as in-app purchase, so skip check
        return (!isAmazonVersion() && hasXpass(context))
                || AdvancedSettings.getLastSupporterState(context);
    }

    /**
     * Returns if X pass is installed and a purchase check with Google Play is not necessary to
     * determine access to paid features.
     */
    public static boolean hasXpass(Context context) {
        // dev builds and the SeriesGuide X key app are not handled through the Play store
        //noinspection ConstantConditions,PointlessBooleanExpression
        return (BuildConfig.DEBUG || hasUnlockKeyInstalled(context));
    }

    /**
     * Returns if the user has a valid copy of X Pass installed.
     */
    private static boolean hasUnlockKeyInstalled(Context context) {
        try {
            // Get our signing key
            PackageManager manager = context.getPackageManager();
            @SuppressLint("PackageManagerGetSignatures") PackageInfo appInfoSeriesGuide = manager
                    .getPackageInfo(
                            context.getApplicationContext().getPackageName(),
                            PackageManager.GET_SIGNATURES);

            // Try to find the X signing key
            @SuppressLint("PackageManagerGetSignatures") PackageInfo appInfoSeriesGuideX = manager
                    .getPackageInfo(
                            "com.battlelancer.seriesguide.x",
                            PackageManager.GET_SIGNATURES);

            Signature[] sgSignatures = appInfoSeriesGuide.signatures;
            Signature[] xSignatures = appInfoSeriesGuideX.signatures;
            if (sgSignatures.length == xSignatures.length) {
                for (int i = 0; i < sgSignatures.length; i++) {
                    if (!sgSignatures[i].toCharsString().equals(xSignatures[i].toCharsString())) {
                        return false; // a signature does not match
                    }
                }
                return true;
            }
        } catch (NameNotFoundException e) {
            // Expected exception that occurs if the package is not present.
        }

        return false;
    }

    /**
     * Launches {@link com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity} or {@link
     * BillingActivity} and notifies that something is only available with the subscription.
     */
    public static void advertiseSubscription(Context context) {
        Toast.makeText(context, R.string.onlyx, Toast.LENGTH_SHORT).show();
        if (isAmazonVersion()) {
            context.startActivity(new Intent(context, AmazonBillingActivity.class));
        } else {
            context.startActivity(new Intent(context, BillingActivity.class));
        }
    }

    /**
     * Check if this is a build for the Amazon app store.
     */
    public static boolean isAmazonVersion() {
        return "amazon".equals(BuildConfig.FLAVOR);
    }

    /**
     * Clear all files in files directory on external storage.
     */
    public static void clearLegacyExternalFileCache(Context context) {
        File path = context.getApplicationContext().getExternalFilesDir(null);
        if (path == null) {
            Timber.w("Could not clear cache, external storage not available");
            return;
        }

        final File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    /**
     * Track a custom event that does not fit the {@link #trackAction(android.content.Context,
     * String, String)}, {@link #trackContextMenu(android.content.Context, String, String)} or
     * {@link #trackClick(android.content.Context, String, String)} trackers. Commonly important
     * status information.
     */
    public static void trackCustomEvent(@NonNull Context context, String category, String action,
            String label) {
        Analytics.getTracker(context).send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());
    }

    /**
     * Track an action event, e.g. when an action item is clicked.
     */
    public static void trackAction(Context context, String category, String label) {
        Analytics.getTracker(context).send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction("Action Item")
                .setLabel(label)
                .build());
    }

    /**
     * Track a context menu event, e.g. when a context item is clicked.
     */
    public static void trackContextMenu(Context context, String category, String label) {
        Analytics.getTracker(context).send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction("Context Item")
                .setLabel(label)
                .build());
    }

    /**
     * Track a generic click that does not fit {@link #trackAction(android.content.Context, String,
     * String)} or {@link #trackContextMenu(android.content.Context, String, String)}.
     */
    public static void trackClick(Context context, String category, String label) {
        Analytics.getTracker(context).send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction("Click")
                .setLabel(label)
                .build());
    }

    public static void trackFailedRequest(Context context, String category, String action,
            int code, String message) {
        // log like "action: 404 not found"
        Timber.tag(category);
        Timber.e("%s: %s %s", action, code, message);

        Utils.trackCustomEvent(context, category, action, code + " " + message);
    }

    public static void trackFailedRequest(Context context, String category, String action,
            @NonNull Throwable throwable) {
        // log like "action: Unable to resolve host"
        Timber.tag(category);
        Timber.e(throwable, "%s: %s", action, throwable.getMessage());

        if (throwable instanceof UnknownHostException /* mostly devices loosing connection */) {
            return; // do not track
        }
        // for tracking only send exception name
        Utils.trackCustomEvent(context, category, action, throwable.getClass().getSimpleName());
    }

    /**
     * Returns false if there is an active, but metered (pre-Jelly Bean: non-WiFi) connection and
     * the user did not approve it for large data downloads (e.g. images).
     */
    public static boolean isAllowedLargeDataConnection(Context context) {
        boolean isConnected;
        boolean largeDataOverWifiOnly = UpdateSettings.isLargeDataOverWifiOnly(context);

        // check connection state
        if (largeDataOverWifiOnly) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                // better: only allow large data downloads over non-metered connections
                isConnected = AndroidUtils.isUnmeteredNetworkConnected(context);
            } else {
                // only allow large data downloads over WiFi,
                // assuming it is most likely to be not metered
                isConnected = AndroidUtils.isWifiConnected(context);
            }
        } else {
            isConnected = AndroidUtils.isNetworkConnected(context);
        }

        return isConnected;
    }

    /**
     * Checks for an available network connection.
     *
     * @param showOfflineToast If not connected, displays a toast asking the user to connect to a
     * network.
     */
    public static boolean isNotConnected(Context context, boolean showOfflineToast) {
        boolean isConnected = AndroidUtils.isNetworkConnected(context);

        // display optional offline toast
        if (!isConnected && showOfflineToast) {
            Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show();
        }

        return !isConnected;
    }

    /**
     * Calls {@link Context#startActivity(Intent)} with the given {@link Intent}. If it is
     * <b>implicit</b>, makes sure there is an {@link Activity} to handle it. If <b>explicit</b>,
     * will intercept {@link android.content.ActivityNotFoundException}. Can show an error toast on
     * failure.
     *
     * <p> E.g. an implicit intent may fail if e.g. the web browser has been disabled through
     * restricted profiles.
     *
     * @return Whether the {@link Intent} could be handled.
     */
    public static boolean tryStartActivity(Context context, Intent intent, boolean displayError) {
        boolean handled = false;

        // check if an implicit intent can be handled (always true for explicit intents)
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            try {
                context.startActivity(intent);
                handled = true;
            } catch (ActivityNotFoundException | SecurityException e) {
                // catch failure to handle explicit intents
                // log in release builds to help extension developers debug
                Log.i("Utils", "Failed to launch intent.", e);
            }
        }

        if (displayError && !handled) {
            Toast.makeText(context, R.string.app_not_available, Toast.LENGTH_LONG).show();
        }

        return handled;
    }

    /**
     * Similar to {@link #tryStartActivity(Context, Intent, boolean)}, but starting an activity for
     * a result.
     */
    public static boolean tryStartActivityForResult(Fragment fragment, Intent intent,
            int requestCode) {
        Context context = fragment.getContext();

        // check if the intent can be handled
        boolean handled = false;
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            try {
                fragment.startActivityForResult(intent, requestCode);
                handled = true;
            } catch (ActivityNotFoundException ignored) {
                // catch failure to handle explicit intents
            }
        }

        if (!handled) {
            Toast.makeText(context, R.string.app_not_available, Toast.LENGTH_LONG).show();
        }

        return handled;
    }

    public static void startActivityWithAnimation(Activity activity, Intent intent, View view) {
        ActivityCompat.startActivity(activity, intent,
                ActivityOptionsCompat
                        .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                        .toBundle()
        );
    }

    public static void startActivityWithTransition(Activity activity, Intent intent, View view,
            @StringRes int sharedElementNameRes) {
        Bundle activityOptions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // shared element transition on L+
            activityOptions = ActivityOptions.makeSceneTransitionAnimation(activity, view,
                    activity.getString(sharedElementNameRes)).toBundle();
        } else {
            // simple scale up animation pre-L
            activityOptions = ActivityOptionsCompat
                    .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                    .toBundle();
        }
        ActivityCompat.startActivity(activity, intent, activityOptions);
    }

    /**
     * Resolves the given attribute to the resource id for the given theme.
     */
    @AnyRes
    public static int resolveAttributeToResourceId(Resources.Theme theme,
            @AttrRes int attributeResId) {
        TypedValue outValue = new TypedValue();
        theme.resolveAttribute(attributeResId, outValue, true);
        return outValue.resourceId;
    }

    /**
     * Tries to start a new activity to handle the given URL using {@link #openNewDocument}.
     */
    public static boolean launchWebsite(@Nullable Context context, @Nullable String url) {
        if (context == null || TextUtils.isEmpty(url)) {
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        return openNewDocument(context, intent, null, null);
    }

    /**
     * Tries to start a new activity to handle the given URL using {@link #openNewDocument}.
     */
    public static void launchWebsite(@Nullable Context context, @Nullable String url,
            @NonNull String logTag, @NonNull String logItem) {
        if (context == null || TextUtils.isEmpty(url)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        openNewDocument(context, intent, logTag, logItem);
    }

    /**
     * Tries to start the given intent as a new document (e.g. opening a website, other app) so it
     * appears as a new entry in the task switcher using {@link #tryStartActivity}.
     *
     * <p>On versions before L, will instead clear the launched activity from the task stack when
     * returning to the app through the task switcher.
     */
    public static boolean openNewDocument(@NonNull Context context, @NonNull Intent intent,
            @Nullable String logTag, @Nullable String logItem) {
        // launch as a new document (separate entry in task switcher)
        // or on older versions: clear from task stack when returning to app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else {
            //noinspection deprecation
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }

        boolean handled = Utils.tryStartActivity(context, intent, true);

        if (logTag != null && logItem != null) {
            Utils.trackAction(context, logTag, logItem);
        }

        return handled;
    }

    /**
     * Executes the {@link android.os.AsyncTask} on the {@link android.os.AsyncTask#SERIAL_EXECUTOR},
     * e.g. one after another.
     *
     * <p> This is useful for executing non-blocking operations (e.g. NO network activity, etc.).
     */
    @SafeVarargs
    public static <Params, Progress, Result> AsyncTask<Params, Progress, Result> executeInOrder(
            AsyncTask<Params, Progress, Result> task, Params... args) {
        return task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, args);
    }
}
