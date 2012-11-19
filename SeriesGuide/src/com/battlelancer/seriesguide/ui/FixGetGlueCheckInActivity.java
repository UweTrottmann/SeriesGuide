
package com.battlelancer.seriesguide.ui;

import android.os.Bundle;

import com.uwetrottmann.seriesguide.R;

/**
 * Displays a list of GetGlue search results to choose from which are used to
 * provide object ids for GetGlue check ins.
 */
public class FixGetGlueCheckInActivity extends BaseActivity {

    public interface InitBundle {
        String SHOW_ID = "showid";
        String SEARCH_TERM = "searchterm";
    }

    @Override
    protected void onCreate(Bundle args) {
        super.onCreate(args);
        setContentView(R.layout.activity_fix_get_glue);
    }

}
