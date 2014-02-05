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

package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.getglueapi.GetGlueCheckin;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.util.TraktTask;
import com.uwetrottmann.androidutils.AndroidUtils;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;

import de.greenrobot.event.EventBus;

/**
 * Blank activity, just used to quickly check into a show/episode on GetGlue/trakt.
 */
public class QuickCheckInActivity extends SherlockFragmentActivity {

    private CheckInDialogFragment mCheckInDialogFragment;

    public interface InitBundle {

        String EPISODE_TVDBID = "episode_tvdbid";
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle arg0) {
        // make the activity show the wallpaper, nothing else
        if (AndroidUtils.isHoneycombOrHigher()) {
            setTheme(android.R.style.Theme_Holo_Wallpaper_NoTitleBar);
        } else {
            setTheme(android.R.style.Theme_Translucent_NoTitleBar);
        }
        super.onCreate(arg0);

        int episodeTvdbId = getIntent().getIntExtra(InitBundle.EPISODE_TVDBID, 0);
        if (episodeTvdbId == 0) {
            finish();
            return;
        }

        // show check-in dialog
        mCheckInDialogFragment = CheckInDialogFragment.newInstance(this, episodeTvdbId);
        mCheckInDialogFragment.show(getSupportFragmentManager(), "checkin-dialog");
    }

    @Override
    protected void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    public void onEvent(GetGlueCheckin.GetGlueCheckInTask.GetGlueCheckInCompleteEvent event) {
        // display status toast about GetGlue check-in
        event.handle(this);
        if (mCheckInDialogFragment == null || !mCheckInDialogFragment.isInLayout()) {
            // if check-in dialog is done, so should we be
            finish();
        }
    }

    public void onEvent(TraktTask.TraktActionCompleteEvent event) {
        if (event.mTraktAction != TraktAction.CHECKIN_EPISODE) {
            return;
        }
        // display status toast about trakt action
        event.handle(this);
        if (mCheckInDialogFragment == null || !mCheckInDialogFragment.isInLayout()) {
            // if check-in dialog is done, so should we be
            finish();
        }
    }

}
