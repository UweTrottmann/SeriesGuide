
package com.battlelancer.seriesguide.appwidget;

import com.battlelancer.seriesguide.R;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class AppWidgetLarge extends AppWidget {
    private static final String LIMIT = "7";

    private static final int LAYOUT = R.layout.appwidget_big;

    private static final int ITEMLAYOUT = R.layout.appwidget_big_item;

    @Override
    public Intent createUpdateIntent(Context context) {
        Intent i = new Intent(context, UpdateServiceLarge.class);
        return i;
    }

    public static class UpdateServiceLarge extends UpdateService {

        @Override
        public void onHandleIntent(Intent intent) {
            ComponentName me = new ComponentName(this, AppWidgetLarge.class);
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);

            Intent i = new Intent(this, AppWidgetLarge.class);
            mgr.updateAppWidget(me, buildUpdate(this, LIMIT, LAYOUT, ITEMLAYOUT, i));
        }
    }
}
