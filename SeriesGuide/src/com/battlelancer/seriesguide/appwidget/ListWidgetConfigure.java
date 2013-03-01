/*
 * Copyright 2012 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.appwidget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RemoteViews;

import com.actionbarsherlock.app.SherlockActivity;
import com.battlelancer.seriesguide.enums.WidgetListType;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.AppSettings;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

public class ListWidgetConfigure extends SherlockActivity {

    private int mAppWidgetId;

    private RadioGroup mRadioGroupType;

    private RadioButton mRadioButtonRecent;

    private CheckBox mCheckUnwatched;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set a theme based on user preference
        setTheme(SeriesGuidePreferences.THEME);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.listwidget_configure);

        // if the user backs out, no widget gets added
        setResult(RESULT_CANCELED);

        findViewById(R.id.buttonConfigDone).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onUpdateWidget();
            }
        });

        mRadioGroupType = (RadioGroup) findViewById(R.id.radioGroupListType);
        mRadioButtonRecent = (RadioButton) findViewById(R.id.radioRecent);
        mCheckUnwatched = (CheckBox) findViewById(R.id.checkBoxUnwatched);

        // non-supporters need to click the widget to get to recent
        if (!Utils.isSupporterChannel(this)) {
            mRadioButtonRecent.setEnabled(false);
            mRadioButtonRecent.setText(getString(R.string.recent) + " ("
                    + getString(R.string.onlyx) + ")");
        }

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
        }

        // restore settings for this widget
        int listType = AppSettings.getWidgetListType(this, mAppWidgetId);
        mRadioGroupType.check(WidgetListType.values()[listType].id);

        boolean hidesWatched = AppSettings.getWidgetHidesWatched(this, mAppWidgetId);
        mCheckUnwatched.setChecked(hidesWatched);
    }

    private void onUpdateWidget() {
        // save values for this widget to be used by ListWidgetService
        AppSettings.saveWidgetConfiguration(this, mAppWidgetId,
                WidgetListType.fromId(mRadioGroupType.getCheckedRadioButtonId()).index,
                mCheckUnwatched.isChecked());

        // update widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        RemoteViews views = ListWidgetProvider.buildRemoteViews(this, mAppWidgetId);
        appWidgetManager.updateAppWidget(mAppWidgetId, views);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
