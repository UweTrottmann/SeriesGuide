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
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.TextUtils;
import java.util.List;
import timber.log.Timber;

/**
 * Broadcast receiver watching for changes to installed packages on the device. Removes uninstalled
 * extensions or clears caches triggering a data refresh if an extension was updated.
 *
 * <p> Adapted from <a href="https://github.com/romannurik/muzei">muzei's</a>
 * SourcePackageChangeReceiver.
 */
public class ExtensionPackageChangeReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }

        String changedPackage = intent.getData().getSchemeSpecificPart();
        ExtensionManager extensionManager = ExtensionManager.getInstance(context);
        List<ComponentName> enabledExtensions = extensionManager.getEnabledExtensions();
        int affectedExtensionIndex = -1;
        for (int i = 0; i < enabledExtensions.size(); i++) {
            ComponentName componentName = enabledExtensions.get(i);
            if (TextUtils.equals(changedPackage, componentName.getPackageName())) {
                affectedExtensionIndex = i;
                break;
            }
        }
        if (affectedExtensionIndex == -1) {
            return;
        }

        // temporarily unsubscribe from extension
        ComponentName changedExtension = enabledExtensions.remove(affectedExtensionIndex);
        extensionManager.setEnabledExtensions(enabledExtensions);

        try {
            context.getPackageManager().getServiceInfo(changedExtension, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Timber.i(e, "Extension no longer available: removed");
            return;
        }

        // changed or updated
        Timber.i("Extension package changed or replaced: re-subscribing");
        enabledExtensions.add(affectedExtensionIndex, changedExtension);
        extensionManager.setEnabledExtensions(enabledExtensions);
    }
}
