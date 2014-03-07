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
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.api.internal.IncomingConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import timber.log.Timber;

public class ExtensionManager {

    private static ExtensionManager _instance;

    public static synchronized ExtensionManager getInstance(Context context) {
        if (_instance == null) {
            _instance = new ExtensionManager(context);
        }
        return _instance;
    }

    private final Context mContext;

    private ComponentName mSubscriberComponentName;

    private Map<ComponentName, String> mEnabledExtensions;

    private ExtensionManager(Context context) {
        mContext = context.getApplicationContext();
        mEnabledExtensions = new HashMap<>();
        mSubscriberComponentName = new ComponentName(context, ExtensionSubscriberService.class);
        // TODO restore enabled extensions
    }

    public List<Extension> queryAllAvailableExtensions() {
        Intent queryIntent = new Intent(SeriesGuideExtension.ACTION_SERIESGUIDE_EXTENSION);
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(queryIntent,
                PackageManager.GET_META_DATA);

        List<Extension> extensions = new ArrayList<>();
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

            extensions.add(extension);
        }

        return extensions;
    }

    public void enableExtension(ComponentName extension) {
        if (extension == null) {
            Timber.e("enableExtension: empty extension");
        }

        synchronized (this) {
            if (mEnabledExtensions.containsKey(extension)) {
                // already subscribed
                return;
            }

            // subscribe
            String token = UUID.randomUUID().toString();
            mEnabledExtensions.put(extension, token);
            mContext.startService(new Intent(IncomingConstants.ACTION_SUBSCRIBE)
                    .setComponent(extension)
                    .putExtra(IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT,
                            mSubscriberComponentName)
                    .putExtra(IncomingConstants.EXTRA_TOKEN, token));
            // TODO persist enabled extensions
        }

        // TODO notify about enabled extension
    }

    public void disableExtension(ComponentName extension) {
        if (extension == null) {
            Timber.e("disableExtension: empty extension");
        }

        synchronized (this) {
            if (!mEnabledExtensions.containsKey(extension)) {
                return;
            }

            // unsubscribe
            mContext.startService(new Intent(IncomingConstants.ACTION_SUBSCRIBE)
                    .setComponent(extension)
                    .putExtra(IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT,
                            mSubscriberComponentName)
                    .putExtra(IncomingConstants.EXTRA_TOKEN, (String) null));
            mEnabledExtensions.remove(extension);
            // TODO persist enabled extensions
        }

        // TODO notify about disabled extension
    }

    public void handlePublishedAction(String token, Action action) {
        // TODO check if token is the one we initially subscribed with

        // TODO check if action episode identifier is for an episode we requested actions for

        // TODO store updated action for this episode

        // TODO notify via event that actions for an episode were updated
    }

    public class Extension {
        public Drawable icon;
        public String label;
        public ComponentName componentName;
        public String description;
        public ComponentName settingsActivity;
    }
}
