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

import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.uwetrottmann.seriesguide.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RemoteViews;

/**
 * Hosts a {@link ListWidgetPreferenceFragment} to allow changing settings of the associated app
 * widget.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ListWidgetConfigure extends Activity {

    private int mAppWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set a theme based on user preference
        setTheme(SeriesGuidePreferences.THEME);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.listwidget_configure);

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

        ListWidgetPreferenceFragment f = ListWidgetPreferenceFragment.newInstance(mAppWidgetId);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_container, f);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean isLightTheme = SeriesGuidePreferences.THEME == R.style.SeriesGuideThemeLight;
        getMenuInflater()
                .inflate(isLightTheme ? R.menu.widget_config_menu_light : R.menu.widget_config_menu,
                        menu);
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
