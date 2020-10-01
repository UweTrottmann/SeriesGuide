package com.battlelancer.seriesguide.appwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences

/**
 * Hosts a [ListWidgetPreferenceFragment] to allow changing settings of the associated app
 * widget.
 *
 * Does specifically NOT extend [com.battlelancer.seriesguide.ui.BaseActivity] to avoid
 * triggering update and backup mechanisms.
 */
class ListWidgetPreferenceActivity : AppCompatActivity() {

    private var appWidgetId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // Use app theme.
        setTheme(SeriesGuidePreferences.THEME)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_singlepane)
        setupActionBar()

        // Get given app widget id, or finish if none.
        val extras = intent.extras
        if (extras == null) {
            finish()
            return
        }
        appWidgetId = extras.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // If the user backs out, do not add widget.
        setWidgetResult(Activity.RESULT_CANCELED)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.content_frame, ListWidgetPreferenceFragment.newInstance(appWidgetId))
                .commit()
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.sgToolbar))
    }

    /**
     * Called by [ListWidgetPreferenceFragment] to update a widget once preferences have changed.
     */
    fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val views = ListWidgetProvider
            .buildRemoteViews(this, appWidgetManager, appWidgetId)
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Note: broken for API 25 Google stock launcher, work around by delaying notify.
        // https://code.google.com/p/android/issues/detail?id=228575
        val runnable = Runnable {
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_view)
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, 300)

        setWidgetResult(Activity.RESULT_OK)
        finish()
    }

    private fun setWidgetResult(resultCode: Int) {
        setResult(
            resultCode,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )
    }
}