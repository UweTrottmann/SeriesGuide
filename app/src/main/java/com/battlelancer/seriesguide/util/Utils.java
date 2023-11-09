// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.util;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.UpdateSettings;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.billing.localdb.GoldStatus;
import com.uwetrottmann.seriesguide.billing.localdb.LocalBillingDb;

/**
 * Various generic helper methods that do not fit other tool categories.
 */
public class Utils {

    private Utils() {
        // prevent instantiation
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
        return BuildConfig.DEBUG || PackageTools.hasUnlockKeyInstalled(context);
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

    /**
     * Tries to start the given intent as a new document (e.g. opening a website, other app) so it
     * appears as a new entry in the task switcher using {@link #tryStartActivity}.
     */
    public static boolean openNewDocument(@NonNull Context context, @NonNull Intent intent) {
        // launch as a new document (separate entry in task switcher)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        return Utils.tryStartActivity(context, intent, true);
    }

}
