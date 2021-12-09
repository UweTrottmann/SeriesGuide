package com.battlelancer.seriesguide.util

import android.content.ActivityNotFoundException
import android.content.Context
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