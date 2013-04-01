
package com.battlelancer.seriesguide.dataliberation;

import android.os.Bundle;

import com.battlelancer.seriesguide.ui.BaseActivity;

public class DataLiberationActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            DataLiberationFragment f = new DataLiberationFragment();
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, f).commit();
        }
    }

}
