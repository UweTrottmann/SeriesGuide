
package com.battlelancer.seriesguide.appwidget;

import com.actionbarsherlock.app.SherlockActivity;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.Utils;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RemoteViews;

public class ListWidgetConfigure extends SherlockActivity {

    private int mAppWidgetId;

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

        findViewById(R.id.buttonOnlyX).setEnabled(Utils.isSupporterChannel(this));
    }

    private void onSetupWidget() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // TODO configure

        // update widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        RemoteViews views = ListWidgetProvider.setupRemoteViews(this, mAppWidgetId);
        appWidgetManager.updateAppWidget(mAppWidgetId, views);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
