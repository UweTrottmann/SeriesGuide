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

package com.battlelancer.seriesguide.extensions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

public class ExtensionManager {

    private static ExtensionManager _instance;

    public synchronized ExtensionManager getInstance(Context context) {
        if (_instance == null) {
            _instance = new ExtensionManager(context);
        }
        return _instance;
    }

    private final Context mContext;

    private List<Extension> mExtensions;

    private ExtensionManager(Context context) {
        mContext = context.getApplicationContext();
        mExtensions = new ArrayList<>();
    }

    public List<Extension> queryAllAvailableExtensions() {
        mExtensions.clear();

        Intent queryIntent = new Intent(SeriesGuideExtension.ACTION_SERIESGUIDE_EXTENSION);
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(queryIntent,
                PackageManager.GET_META_DATA);

        for (ResolveInfo info : resolveInfos) {
            Extension extension = new Extension();
            // get label, icon and component name
            extension.label = info.loadLabel(pm).toString();
            extension.icon = info.loadIcon(pm);
            extension.componentName = new ComponentName(info.serviceInfo.packageName,
                    info.serviceInfo.name);
            // get description
            Context packageContext;
            try {
                packageContext = mContext.createPackageContext(
                        extension.componentName.getPackageName(), 0);
                Resources packageRes = packageContext.getResources();
                extension.description = packageRes.getString(info.serviceInfo.descriptionRes);
            } catch (SecurityException | PackageManager.NameNotFoundException e) {
                Timber.e(e, "Reading package resources for extension " + extension.componentName
                        + " failed");
            }
            // get (optional) settings activity
            Bundle metaData = info.serviceInfo.metaData;
            if (metaData != null) {
                String settingsActivity = metaData.getString("settingsActivity");
                if (!TextUtils.isEmpty(settingsActivity)) {
                    extension.settingsActivity = ComponentName.unflattenFromString(
                            info.serviceInfo.packageName + "/" + settingsActivity);
                }
            }

            mExtensions.add(extension);
        }

        return mExtensions;
    }

    public class Extension {
        public Drawable icon;
        public String label;
        public ComponentName componentName;
        public String description;
        public ComponentName settingsActivity;
    }
}
