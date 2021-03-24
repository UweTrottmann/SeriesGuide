package com.battlelancer.seriesguide.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
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
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.AnyRes;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.UpdateSettings;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.billing.localdb.GoldStatus;
import com.uwetrottmann.seriesguide.billing.localdb.LocalBillingDb;
import java.io.File;
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
                    SgRoomDatabase.VERSION, BuildConfig.VERSION_CODE);
        } else {
            return context.getString(R.string.format_version, getVersion(context),
                    SgRoomDatabase.VERSION);
        }
    }

    /**
     * Returns if the user should get access to paid features.
     */
    public static boolean hasAccessToX(Context context) {
        // debug builds, installed X Pass key or subscription unlock all features
        if (isAmazonVersion()) {
            // Amazon version only supports all access as in-app purchase, so skip key check
            return AdvancedSettings.getLastSupporterState(context);
        } else {
            if (hasXpass(context)) {
                return true;
            } else {
                GoldStatus goldStatus = LocalBillingDb.getInstance(context).entitlementsDao()
                        .getGoldStatus();
                return goldStatus != null && goldStatus.getEntitled();
            }
        }
    }

    /**
     * Returns if X pass is installed and a purchase check with Google Play is not necessary to
     * determine access to paid features.
     */
    public static boolean hasXpass(Context context) {
        // dev builds and the SeriesGuide X key app are not handled through the Play store
        return BuildConfig.DEBUG || hasUnlockKeyInstalled(context);
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
            Timber.d("X Pass not found.");
        }

        return false;
    }

    /**
     * Launches {@link com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity} or {@link
     * BillingActivity} and notifies that something is only available with the subscription.
     */
    public static void advertiseSubscription(Context context) {
        Toast.makeText(context, R.string.onlyx, Toast.LENGTH_SHORT).show();
        context.startActivity(getBillingActivityIntent(context));
    }

    /**
     * Check if this is a build for the Amazon app store.
     */
    public static boolean isAmazonVersion() {
        //noinspection ConstantConditions Changes depending on flavor build.
        return "amazon".equals(BuildConfig.FLAVOR);
    }

    @NonNull
    public static Intent getBillingActivityIntent(Context context) {
        if (Utils.isAmazonVersion()) {
           return new Intent(context, AmazonBillingActivity.class);
        } else {
            return new Intent(context, BillingActivity.class);
        }
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
     * Returns false if there is an active, but metered connection and
     * the user did not approve it for large data downloads (e.g. images).
     */
    static boolean isAllowedLargeDataConnection(Context context) {
        boolean isConnected;
        boolean largeDataOverWifiOnly = UpdateSettings.isLargeDataOverWifiOnly(context);

        // check connection state
        if (largeDataOverWifiOnly) {
            // only allow large data downloads over non-metered connections
            isConnected = AndroidUtils.isUnmeteredNetworkConnected(context);
        } else {
            isConnected = AndroidUtils.isNetworkConnected(context);
        }

        return isConnected;
    }

    /**
     * Checks for an available network connection.
     */
    public static boolean isNotConnected(Context context) {
        boolean isConnected = AndroidUtils.isNetworkConnected(context);

        // display offline toast
        if (!isConnected) {
            Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show();
        }

        return !isConnected;
    }

    /**
     * Calls {@link Context#startActivity(Intent)} with the given {@link Intent}. Returns false if
     * no activity found to handle it. Can show an error toast on failure.
     *
     * <p> E.g. an implicit intent may fail if the web browser has been disabled through
     * restricted profiles.
     */
    @SuppressLint("LogNotTimber")
    public static boolean tryStartActivity(Context context, Intent intent, boolean displayError) {
        // Note: Android docs suggest to use resolveActivity,
        // but won't work on Android 11+ due to package visibility changes.
        // https://developer.android.com/about/versions/11/privacy/package-visibility
        boolean handled;
        try {
            context.startActivity(intent);
            handled = true;
        } catch (ActivityNotFoundException | SecurityException e) {
            // catch failure to handle explicit intents
            // log in release builds to help extension developers debug
            Log.i("Utils", "Failed to launch intent.", e);
            handled = false;
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
    public static void tryStartActivityForResult(Fragment fragment, Intent intent,
            int requestCode) {
        Context context = fragment.getContext();

        // Note: Android docs suggest to use resolveActivity,
        // but won't work on Android 11+ due to package visibility changes.
        // https://developer.android.com/about/versions/11/privacy/package-visibility
        boolean handled;
        try {
            fragment.startActivityForResult(intent, requestCode);
            handled = true;
        } catch (ActivityNotFoundException ignored) {
            handled = false;
        }

        if (!handled) {
            Toast.makeText(context, R.string.app_not_available, Toast.LENGTH_LONG).show();
        }
    }

    public static void startActivityWithAnimation(Context context, Intent intent, View view) {
        ActivityCompat.startActivity(context, intent,
                ActivityOptionsCompat
                        .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                        .toBundle()
        );
    }

    public static void startActivityWithTransition(Activity activity, Intent intent, View view,
            @StringRes int sharedElementNameRes) {
        // shared element transition on L+
        Bundle activityOptions = ActivityOptions.makeSceneTransitionAnimation(activity, view,
                activity.getString(sharedElementNameRes)).toBundle();
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
        return openNewDocument(context, intent);
    }

    /**
     * Tries to start the given intent as a new document (e.g. opening a website, other app) so it
     * appears as a new entry in the task switcher using {@link #tryStartActivity}.
     */
    public static boolean openNewDocument(@NonNull Context context, @NonNull Intent intent) {
        // launch as a new document (separate entry in task switcher)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        return Utils.tryStartActivity(context, intent, true);
    }

    /**
     * Executes the {@link android.os.AsyncTask} on the {@link android.os.AsyncTask#SERIAL_EXECUTOR},
     * e.g. one after another.
     *
     * <p> This is useful for executing non-blocking operations (e.g. NO network activity, etc.).
     */
    @SafeVarargs
    public static <Params, Progress, Result> void executeInOrder(
            AsyncTask<Params, Progress, Result> task, Params... args) {
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, args);
    }
}
