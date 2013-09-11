/*
 * Copyright 2013 Andrew Neal
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
 * 
 * Modified by Uwe Trottmann to better work with newer devices.
 * 
 */

package com.battlelancer.seriesguide.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.ShareUtils.ShareMethod;
import com.battlelancer.seriesguide.util.SystemUiHider;
import com.battlelancer.seriesguide.util.SystemUiHider.OnVisibilityChangeListener;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.seriesguide.R;

/**
 * This {@link Activity} is used to display a full screen image of a TV show's
 * poster, or the image provided for a specific episode.
 */
public class FullscreenImageActivity extends SherlockFragmentActivity implements Runnable {

    /** Log tag */
    private static final String TAG = "FullscreenImageActivity";

    /** The {@link Intent} extra used to deliver the path to the requested image */
    public static final String PATH = ShareItems.IMAGE;

    /**
     * The number of milliseconds to wait after user interaction before hiding
     * the system UI
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /** The flags to pass to {@link SystemUiHider#getInstance} */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /** The {@link Handler} used to schedule System UI changes */
    private final Handler mHideHandler = new Handler();

    /** The instance of the {@link SystemUiHider} for this activity */
    private SystemUiHider mSystemUiHider;

    /** Displays the poster or episode preview */
    private ImageView mContentView;

    /** The {@link Bundle} passed into this activity */
    private Bundle mArgs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen_image_activity);
        mContentView = (ImageView) findViewById(R.id.fullscreen_content);

        // Set up the ActionBar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Load the requested image
        mArgs = getIntent().getExtras();
        String imagePath = mArgs.getString(PATH);
        mContentView.setImageBitmap(ImageProvider.getInstance(this).getImage(imagePath, false));

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity
        mSystemUiHider = SystemUiHider.getInstance(this, mContentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider.setOnVisibilityChangeListener(new OnVisibilityChangeListener() {
            @Override
            public void onVisibilityChange(boolean visible) {
                if (visible) {
                    // Show the ActionBar
                    actionBar.show();
                    // Schedule a hide().
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                } else {
                    // Hide the ActionBar
                    actionBar.hide();
                }
            }
        });

        // Set up the user interaction to manually show or hide the system UI
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSystemUiHider.toggle();
            }
        });

        // hide() right away
        delayedHide(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.fullscreen_image_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.menu_share) {
            ShareUtils.onShareEpisode(this, mArgs, ShareMethod.OTHER_SERVICES);
            EasyTracker.getTracker().sendEvent(TAG, "Action Item", "Share", null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDetachedFromWindow() {
        // Release any references to the ImageView
        mContentView.setImageDrawable(null);
        mContentView = null;
        // Release any references to the Handler
        mHideHandler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }

    @Override
    public void run() {
        mSystemUiHider.hide();
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(this);
        mHideHandler.postDelayed(this, delayMillis);
    }

}
