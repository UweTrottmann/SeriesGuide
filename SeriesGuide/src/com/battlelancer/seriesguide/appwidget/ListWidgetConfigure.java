
package com.battlelancer.seriesguide.appwidget;

import com.actionbarsherlock.app.SherlockActivity;
import com.battlelancer.seriesguide.enums.WidgetListType;
import com.battlelancer.seriesguide.util.Utils;

import android.R;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RemoteViews;

public class ListWidgetConfigure extends SherlockActivity {

    public static final String PREFS_NAME = "ListWidgetPreferences";

    public static final String PREF_LISTTYPE_KEY = "listtype_";

    public static final String PREF_WATCHEDONLY_KEY = "unwatched_";

    private int mAppWidgetId;

    private RadioGroup mRadioGroupType;

    private RadioButton mRadioButtonRecent;

    private CheckBox mCheckUnwatched;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listwidget_configure);

        // if the user backs out, no widget gets added
        setResult(RESULT_CANCELED);

        findViewById(R.id.buttonConfigDone).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onSetupWidget();
            }
        });

        mRadioGroupType = (RadioGroup) findViewById(R.id.radioGroupListType);
        mRadioGroupType.check(R.id.radioUpcoming);
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
    }

    private void onSetupWidget() {
        // save values for this widget to be used by ListWidgetService
        SharedPreferences.Editor prefs = getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_LISTTYPE_KEY + mAppWidgetId,
                WidgetListType.fromId(mRadioGroupType.getCheckedRadioButtonId()).index);
        prefs.putBoolean(PREF_WATCHEDONLY_KEY + mAppWidgetId, mCheckUnwatched.isChecked());
        prefs.commit();

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
