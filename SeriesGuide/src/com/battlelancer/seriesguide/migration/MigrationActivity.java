package com.battlelancer.seriesguide.migration;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import com.battlelancer.seriesguide.ui.BaseActivity;
import com.uwetrottmann.seriesguide.R;

/**
 * Helps users migrate their show database to the free version of SeriesGuide.
 */
public class MigrationActivity extends BaseActivity {

    private static final String KEY_MIGRATION_OPT_OUT = "com.battlelancer.seriesguide.migration.optout";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_migration);

        setupViews();
    }

    private void setupViews() {
        findViewById(R.id.buttonMigrationExport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO backup shows
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // TODO check if backup already exists

        // TODO check if SeriesGuide is already installed
    }

    public static boolean hasOptedOutOfMigration(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_MIGRATION_OPT_OUT,
                false);
    }
}
