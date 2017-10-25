package com.battlelancer.seriesguide.appwidget;

import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.RemoteViews;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;

/**
 * Hosts a {@link ListWidgetPreferenceFragment} to allow changing settings of the associated app
 * widget.
 * <p> Does specifically NOT extend {@link com.battlelancer.seriesguide.ui.BaseActivity} to avoid
 * triggering update and backup mechanisms.
 * <p> The list widget is only available on API level 11 and above.
 */
public class ListWidgetConfigure extends AppCompatActivity {

    private int appWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set a theme based on user preference
        setTheme(SeriesGuidePreferences.THEME);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        // get given app widget id
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // if the user backs out, no widget gets added
        setWidgetResult(RESULT_CANCELED);

        if (savedInstanceState == null) {
            ListWidgetPreferenceFragment f = ListWidgetPreferenceFragment.newInstance(appWidgetId);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.content_frame, f);
            ft.commit();
        }
    }

    private void setupActionBar() {
        Toolbar toolbar = findViewById(R.id.sgToolbar);
        setSupportActionBar(toolbar);
    }

    protected void updateWidget() {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        RemoteViews views = ListWidgetProvider
                .buildRemoteViews(this, appWidgetManager, appWidgetId);
        appWidgetManager.updateAppWidget(appWidgetId, views);
        // note: broken for API 25 Google stock launcher, work around by delaying notify.
        // https://code.google.com/p/android/issues/detail?id=228575
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_view);
            }
        };
        new Handler().postDelayed(runnable, 300);

        setWidgetResult(RESULT_OK);
        finish();
    }

    private void setWidgetResult(int resultCode) {
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(resultCode, resultValue);
    }
}
