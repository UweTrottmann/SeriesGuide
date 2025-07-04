// SPDX-License-Identifier: Apache-2.0
// Copyright 2011-2025 Uwe Trottmann

package com.battlelancer.seriesguide.util;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.battlelancer.seriesguide.R;

/**
 * Various generic helper methods that do not fit other tool categories.
 */
public class Utils {

    private Utils() {
        // prevent instantiation
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
