
package com.battlelancer.seriesguide;

import android.annotation.TargetApi;
import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

@TargetApi(8)
public class PrefBackupHelper extends BackupAgentHelper {

    // A key to uniquely identify the set of backup data
    static final String PREFS_BACKUP_KEY = "prefs";

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this,
                this.getPackageName() + "_preferences");
        addHelper(PREFS_BACKUP_KEY, helper);
    }
}
