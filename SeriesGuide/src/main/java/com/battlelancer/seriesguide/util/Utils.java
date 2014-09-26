/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.battlelancer.seriesguide.Analytics;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.service.OnAlarmReceiver;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.UpdateSettings;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import timber.log.Timber;

public class Utils {

    /**
     * Returns a string in format "1x01 title" or "S1E01 title" dependent on a user preference.
     */
    public static String getNextEpisodeString(Context context, int season, int episode,
            String title) {
        String result = getEpisodeNumber(context, season, episode);
        result += " " + title;
        return result;
    }

    /**
     * Returns the episode number formatted according to the users preference (e.g. '1x01',
     * 'S01E01', ...).
     */
    public static String getEpisodeNumber(Context context, int season, int episode) {
        String format = DisplaySettings.getNumberFormat(context);
        String result = String.valueOf(season);
        if (DisplaySettings.NUMBERFORMAT_DEFAULT.equals(format)) {
            // 1x01 format
            result += "x";
        } else {
            // S01E01 format
            // make season number always two chars long
            if (season < 10) {
                result = "0" + result;
            }
            if (DisplaySettings.NUMBERFORMAT_ENGLISHLOWER.equals(format)) {
                result = "s" + result + "e";
            } else {
                result = "S" + result + "E";
            }
        }

        if (episode != -1) {
            // make episode number always two chars long
            if (episode < 10) {
                result += "0";
            }

            result += episode;
        }
        return result;
    }

    /**
     * Splits the string and reassembles it, separating the items with commas. The given object is
     * returned with the new string.
     */
    public static String splitAndKitTVDBStrings(String tvdbstring) {
        if (tvdbstring == null) {
            tvdbstring = "";
        }
        String[] splitted = tvdbstring.split("\\|");
        tvdbstring = "";
        for (String item : splitted) {
            if (tvdbstring.length() != 0) {
                tvdbstring += ", ";
            }
            tvdbstring += item.trim();
        }
        return tvdbstring;
    }

    public static String getVersion(Context context) {
        String version;
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA).versionName;
        } catch (NameNotFoundException e) {
            version = "UnknownVersion";
        }
        return version;
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
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, OnAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1
                * DateUtils.MINUTE_IN_MILLIS, pi);
    }

    /**
     * Creates a SHA1 hex encoded representation of the given String.
     */
    public static String toSHA1(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageBytes = message.getBytes("UTF-8");
            byte[] digest = md.digest(messageBytes);

            String result = "";
            for (int i = 0; i < digest.length; i++) {
                result += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1);
            }

            return result;
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            Timber.e(e, "Failed creating SHA1");
        }
        return null;
    }

    /**
     * Returns whether a regular check with the Google Play app is not necessary to determine access
     * to X features (e.g. the subscription is still valid).
     */
    public static boolean canSkipPurchaseCheck(Context context) {
        // dev builds and the SeriesGuide X key app are not handled through the Play store
        return (BuildConfig.DEBUG || hasUnlockKeyInstalled(context));
    }

    /**
     * Returns whether this user should currently get access to X features.
     */
    public static boolean hasAccessToX(Context context) {
        // dev builds, SeriesGuide X installed or a valid purchase unlock X features
        // Fire OS build is paid only
        return isAmazonVersion()
                || canSkipPurchaseCheck(context)
                || AdvancedSettings.isSubscribedToX(context);
    }

    /**
     * Returns true if the user has the legacy SeriesGuide X version installed, signed with the same
     * key as we are.
     */
    private static boolean hasUnlockKeyInstalled(Context context) {
        try {
            // Get our signing key
            PackageManager manager = context.getPackageManager();
            PackageInfo appInfoSeriesGuide = manager
                    .getPackageInfo(
                            context.getApplicationContext().getPackageName(),
                            PackageManager.GET_SIGNATURES);

            // Try to find the X signing key
            PackageInfo appInfoSeriesGuideX = manager
                    .getPackageInfo(
                            "com.battlelancer.seriesguide.x",
                            PackageManager.GET_SIGNATURES);

            final String ourKey = appInfoSeriesGuide.signatures[0].toCharsString();
            final String xKey = appInfoSeriesGuideX.signatures[0].toCharsString();
            return ourKey.equals(xKey);
        } catch (NameNotFoundException e) {
            // Expected exception that occurs if the package is not present.
        }

        return false;
    }

    /**
     * Launches {@link BillingActivity} and notifies that something is only available with the X
     * subscription.
     */
    public static void advertiseSubscription(Context context) {
        Toast.makeText(context, R.string.onlyx, Toast.LENGTH_SHORT).show();
        context.startActivity(new Intent(context, BillingActivity.class));
    }

    /**
     * Check if this is a build for the Amazon app store.
     */
    public static boolean isAmazonVersion() {
        return "amazon".equals(BuildConfig.FLAVOR);
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end of, and below the
     * text.  Use 0 if you do not want a Drawable there. The Drawables' bounds will be set to their
     * intrinsic bounds.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(Button button, int left,
            int top, int right, int bottom) {
        if (AndroidUtils.isJellyBeanMR1OrHigher()) {
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(left, top, right, bottom);
            return;
        }

        final Resources resources = button.getContext().getResources();
        setCompoundDrawablesRelativeWithIntrinsicBounds(
                button,
                left != 0 ? resources.getDrawable(left) : null,
                top != 0 ? resources.getDrawable(top) : null,
                right != 0 ? resources.getDrawable(right) : null,
                bottom != 0 ? resources.getDrawable(bottom) : null);
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end of, and below the
     * text.  Use null if you do not want a Drawable there. The Drawables' bounds will be set to
     * their intrinsic bounds.
     */
    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(Button button,
            Drawable left, Drawable top, Drawable right, Drawable bottom) {
        if (left != null) {
            left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
        }
        if (right != null) {
            right.setBounds(0, 0, right.getIntrinsicWidth(), right.getIntrinsicHeight());
        }
        if (top != null) {
            top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
        }
        if (bottom != null) {
            bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
        }
        button.setCompoundDrawables(left, top, right, bottom);
    }

    public static void setValueOrPlaceholder(View view, final String value) {
        TextView field = (TextView) view;
        if (value == null || value.length() == 0) {
            field.setText(R.string.unknown);
        } else {
            field.setText(value);
        }
    }

    /**
     * If the given string is not null or empty, will make the label and value field {@link
     * View#VISIBLE}. Otherwise both {@link View#GONE}.
     *
     * @return True if the views are visible.
     */
    public static boolean setLabelValueOrHide(View label, TextView text, final String value) {
        if (TextUtils.isEmpty(value)) {
            label.setVisibility(View.GONE);
            text.setVisibility(View.GONE);
            return false;
        } else {
            label.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
            text.setText(value);
            return true;
        }
    }

    /**
     * If the given double is larger than 0, will make the label and value field {@link
     * View#VISIBLE}. Otherwise both {@link View#GONE}.
     *
     * @return True if the views are visible.
     */
    public static boolean setLabelValueOrHide(View label, TextView text, double value) {
        if (value > 0.0) {
            label.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
            text.setText(String.valueOf(value));
            return true;
        } else {
            label.setVisibility(View.GONE);
            text.setVisibility(View.GONE);
            return false;
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
                file.delete();
            }
        }
    }

    /**
     * Tries to load the given TVDb show poster into the given {@link android.widget.ImageView}
     * without any resizing or cropping.
     */
    public static void loadPoster(Context context, ImageView imageView,
            String posterPath) {
        ServiceUtils.getPicasso(context)
                .load(TheTVDB.buildPosterUrl(posterPath))
                .noFade()
                .into(imageView);
    }

    /**
     * Tries to load the given TVDb show poster into the given {@link android.widget.ImageView}
     * without any resizing or cropping. In addition sets alpha on the view.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void loadPosterBackground(Context context, ImageView imageView,
            String posterPath) {
        if (AndroidUtils.isJellyBeanOrHigher()) {
            imageView.setImageAlpha(30);
        } else {
            imageView.setAlpha(30);
        }

        loadPoster(context, imageView, posterPath);
    }

    /**
     * Tries to load a down-sized, center cropped version of the given TVDb show poster into the
     * given {@link android.widget.ImageView}.
     *
     * <p> The resize dimensions are those used for posters in the show list.
     */
    public static void loadPosterThumbnail(Context context, ImageView imageView,
            String posterPath) {
        if (TextUtils.isEmpty(posterPath)) {
            // there is no image available
            imageView.setImageBitmap(null);
            return;
        }

        ServiceUtils.getPicasso(context).load(TheTVDB.buildPosterUrl(posterPath))
                .centerCrop()
                .resizeDimen(R.dimen.show_poster_width, R.dimen.show_poster_height)
                .error(R.drawable.ic_image_missing)
                .into(imageView);
    }

    /**
     * Sets the global app theme variable. Applied by all activities once they are created.
     */
    public static synchronized void updateTheme(String themeIndex) {
        int theme = Integer.valueOf(themeIndex);
        switch (theme) {
            case 1:
                SeriesGuidePreferences.THEME = R.style.Theme_SeriesGuide_DarkBlue;
                break;
            case 2:
                SeriesGuidePreferences.THEME = R.style.Theme_SeriesGuide_Light;
                break;
            default:
                SeriesGuidePreferences.THEME = R.style.Theme_SeriesGuide;
                break;
        }
    }

    /**
     * Set the alpha value of the {@code color} to be the given {@code alpha} value.
     */
    public static int setColorAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Track a screen view. This is commonly called in {@link android.support.v4.app.Fragment#onStart()}.
     */
    public static void trackView(Context context, String screenName) {
        Tracker tracker = Analytics.getTracker(context);
        tracker.setScreenName(screenName);
        tracker.send(new HitBuilders.AppViewBuilder().build());
    }

    /**
     * Track a custom event that does not fit the {@link #trackAction(android.content.Context,
     * String, String)}, {@link #trackContextMenu(android.content.Context, String, String)} or
     * {@link #trackClick(android.content.Context, String, String)} trackers. Commonly important
     * status information.
     */
    public static void trackCustomEvent(Context context, String tag, String action,
            String label) {
        Analytics.getTracker(context).send(new HitBuilders.EventBuilder()
                .setCategory(tag)
                .setAction(action)
                .setLabel(label)
                .build());
    }

    /**
     * Track an action event, e.g. when an action item is clicked.
     */
    public static void trackAction(Context context, String tag, String label) {
        Analytics.getTracker(context).send(new HitBuilders.EventBuilder()
                .setCategory(tag)
                .setAction("Action Item")
                .setLabel(label)
                .build());
    }

    /**
     * Track a context menu event, e.g. when a context item is clicked.
     */
    public static void trackContextMenu(Context context, String tag, String label) {
        Analytics.getTracker(context).send(new HitBuilders.EventBuilder()
                .setCategory(tag)
                .setAction("Context Item")
                .setLabel(label)
                .build());
    }

    /**
     * Track a generic click that does not fit {@link #trackAction(android.content.Context, String,
     * String)} or {@link #trackContextMenu(android.content.Context, String, String)}.
     */
    public static void trackClick(Context context, String tag, String label) {
        Analytics.getTracker(context).send(new HitBuilders.EventBuilder()
                .setCategory(tag)
                .setAction("Click")
                .setLabel(label)
                .build());
    }

    /**
     * Returns true if there is an active connection which is approved by the user for large data
     * downloads (e.g. images).
     *
     * @param showOfflineToast If true, displays a toast asking the user to connect to a network.
     */
    public static boolean isAllowedLargeDataConnection(Context context, boolean showOfflineToast) {
        boolean isConnected;
        boolean largeDataOverWifiOnly = UpdateSettings.isLargeDataOverWifiOnly(context);

        // check connection state
        if (largeDataOverWifiOnly) {
            isConnected = AndroidUtils.isWifiConnected(context);
        } else {
            isConnected = AndroidUtils.isNetworkConnected(context);
        }

        // display optional offline toast
        if (showOfflineToast && !isConnected) {
            Toast.makeText(context,
                    largeDataOverWifiOnly ? R.string.offline_no_wifi : R.string.offline,
                    Toast.LENGTH_LONG).show();
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
     * Calls {@link Context#startActivity(Intent)} with the given <b>implicit</b> {@link Intent}
     * after making sure there is an {@link Activity} to handle it. Can show an error toast, if not.
     * <br> <br> This may happen if e.g. the web browser has been disabled through restricted
     * profiles.
     *
     * @return Whether there was an {@link Activity} to handle the given {@link Intent}.
     */
    public static boolean tryStartActivity(Context context, Intent intent, boolean displayError) {
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        } else if (displayError) {
            Toast.makeText(context, R.string.app_not_available, Toast.LENGTH_LONG).show();
        }
        return false;
    }

    /**
     * Resolves the given attribute to the resource id for the given theme.
     */
    public static int resolveAttributeToResourceId(Resources.Theme theme, int attributeResId) {
        TypedValue outValue = new TypedValue();
        theme.resolveAttribute(attributeResId, outValue, true);
        return outValue.resourceId;
    }

    /**
     * Tries to launch a web browser loading the given URL. Sets a flag to exit the browser if
     * coming back to the app.
     */
    public static void launchWebsite(Context context, String url, String logTag, String logItem) {
        if (context == null || TextUtils.isEmpty(url)) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        // try to launch web browser
        Utils.tryStartActivity(context, intent, true);

        Utils.trackAction(context, logTag, logItem);
    }

    /**
     * Executes the {@link android.os.AsyncTask} on the {@link android.os.AsyncTask#SERIAL_EXECUTOR},
     * e.g. one after another.
     *
     * <p> This is useful for executing non-blocking operations (e.g. NO network activity, etc.).
     */
    @SafeVarargs
    public static <T> AsyncTask executeInOrder(AsyncTask<T, ?, ?> task, T... args) {
        return task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, args);
    }

    /**
     * Returns an {@link java.io.InputStream} using {@link java.net.HttpURLConnection} to connect to
     * the given URL. <p/> Responses are downloaded and cached using the default HTTP client
     * instance (see {@link com.battlelancer.seriesguide.util.ServiceUtils}.
     */
    public static InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);

        HttpURLConnection conn = ServiceUtils.getUrlFactory().open(url);
        conn.connect();

        return conn.getInputStream();
    }

    /**
     * Returns an {@link java.io.InputStream} using {@link java.net.HttpURLConnection} to connect to
     * the given URL. <p/> Responses are downloaded and cached using the default HTTP client
     * instance (see {@link com.battlelancer.seriesguide.util.ServiceUtils}.
     */
    public static InputStream downloadAndCacheUrl(Context context, String urlString)
            throws IOException {
        URL url = new URL(urlString);

        HttpURLConnection conn = ServiceUtils.getCachingUrlFactory(context).open(url);
        conn.connect();

        return conn.getInputStream();
    }
}
