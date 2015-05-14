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

package com.battlelancer.seriesguide.appwidget;

import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
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

    private int mAppWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set a theme based on user preference
        setTheme(SeriesGuidePreferences.THEME);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        // if the user backs out, no widget gets added
        setResult(RESULT_CANCELED);

        // get given app widget id
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        if (savedInstanceState == null) {
            ListWidgetPreferenceFragment f = ListWidgetPreferenceFragment.newInstance(mAppWidgetId);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.content_frame, f);
            ft.commit();
        }
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.sgToolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_config_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_save) {
            onUpdateWidget();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onUpdateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        RemoteViews views = ListWidgetProvider
                .buildRemoteViews(this, appWidgetManager, mAppWidgetId);
        appWidgetManager.updateAppWidget(mAppWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.list_view);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
