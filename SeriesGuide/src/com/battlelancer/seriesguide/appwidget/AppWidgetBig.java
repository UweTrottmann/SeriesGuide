/*
 * Copyright (C) 2011 Uwe Trottmann 
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

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.uwetrottmann.seriesguide.R;

public class AppWidgetBig extends AppWidget {
    private static final String LIMIT = "3";

    private static final int LAYOUT = R.layout.appwidget_big;

    private static final int ITEMLAYOUT = R.layout.appwidget_big_item;

    @Override
    public Intent createUpdateIntent(Context context) {
        Intent i = new Intent(context, UpdateServiceBig.class);
        return i;
    }

    public static class UpdateServiceBig extends UpdateService {

        @Override
        public void onHandleIntent(Intent intent) {
            ComponentName me = new ComponentName(this, AppWidgetBig.class);
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);

            Intent i = new Intent(this, AppWidgetBig.class);
            mgr.updateAppWidget(me, buildUpdate(this, LIMIT, LAYOUT, ITEMLAYOUT, i));
        }
    }
}
