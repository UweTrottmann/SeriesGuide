// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2024 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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