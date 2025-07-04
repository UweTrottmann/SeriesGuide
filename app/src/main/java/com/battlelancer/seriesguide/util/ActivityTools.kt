// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2025 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.battlelancer.seriesguide.R

/**
 * Catches [ActivityNotFoundException] from [ActivityResultLauncher.launch]
 * and displays an error toast.
 */
fun <I> ActivityResultLauncher<I>.tryLaunch(input: I, context: Context) {
    try {
        launch(input)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.app_not_available, Toast.LENGTH_LONG).show()
    }
}

/**
 * Uses [ActivityOptions.makeScaleUpAnimation] on the [sourceView].
 */
fun Context.startActivityWithAnimation(intent: Intent, sourceView: View) {
    startActivity(
        intent,
        ActivityOptions
            .makeScaleUpAnimation(sourceView, 0, 0, sourceView.width, sourceView.height)
            .toBundle()
    )
}

/**
 * Calls [Context.startActivity] with the given [Intent]. Returns false if
 * no activity found to handle it. If [displayError] shows an error toast on failure.
 *
 * This is useful for example for an implicit intent that may fail to open the web browser
 * app if it is disabled in a restricted profile.
 */
@SuppressLint("LogNotTimber")
fun Context.tryStartActivity(intent: Intent, displayError: Boolean): Boolean {
    // Note: Android docs suggest to use resolveActivity,
    // but won't work on Android 11+ due to package visibility changes.
    // https://developer.android.com/about/versions/11/privacy/package-visibility
    var handled: Boolean
    try {
        startActivity(intent)
        handled = true
    } catch (e: ActivityNotFoundException) {
        // catch failure to handle explicit intents
        // log in release builds to help extension developers debug
        Log.i("Utils", "Failed to launch intent.", e)
        handled = false
    } catch (e: SecurityException) {
        Log.i("Utils", "Failed to launch intent.", e)
        handled = false
    }

    if (displayError && !handled) {
        Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_LONG).show()
    }

    return handled
}

/**
 * Tries to start the given intent as a new document (e.g. opening a website, other app) so it
 * appears as a new entry in the task switcher using [tryStartActivity].
 */
fun Context.openNewDocument(intent: Intent): Boolean {
    // launch as a new document (separate entry in task switcher)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    return tryStartActivity(intent, true)
}
