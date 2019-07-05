package com.battlelancer.seriesguide.extensions;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
public class ExtensionPackageChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }

        String changedPackage = intent.getData().getSchemeSpecificPart();
        String appPackageName = context.getPackageName();
        if (appPackageName.equals(changedPackage)) {
            // Ignore changes to SeriesGuide itself. Will re-subscribe to extensions on app restart.
            // E.g. WorkManager enabling RescheduleReceiver triggers ACTION_PACKAGE_CHANGED.
            Timber.i("Ignoring update of ourself.");
            return;
        }

        ExtensionManager extensionManager = ExtensionManager.get(context);
        List<ComponentName> enabledExtensions = extensionManager.getEnabledExtensions(context);
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
        extensionManager.setEnabledExtensions(context, enabledExtensions);

        try {
            context.getPackageManager().getReceiverInfo(changedExtension, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Timber.i("Extension %s no longer available: removed",
                    changedExtension.toShortString());
            return;
        }

        // changed or updated
        Timber.i("Extension %s changed or replaced: re-subscribing",
                changedExtension.toShortString());
        enabledExtensions.add(affectedExtensionIndex, changedExtension);
        extensionManager.setEnabledExtensions(context, enabledExtensions);
    }
}
