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

import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.SystemUiHider;
import com.battlelancer.seriesguide.util.SystemUiHider.OnVisibilityChangeListener;
import com.uwetrottmann.seriesguide.R;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * This {@link Activity} is used to display a full screen image of a TV show's
 * poster, or the image provided for a specific episode.
 */
public class FullscreenImageActivity extends Activity {

    /**
     * The {@link Intent} extra used to deliver the path to the requested image
     */
    public static final String PATH = "fullscreenimageactivity.intent.extra.image";

    /**
     * The number of milliseconds to wait after user interaction before hiding
     * the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    /**
     * Displays the poster or episode preview
     */
    private ImageView mContentView;

    /**
     * Handles all zooming
     */
    private PhotoViewAttacher mAttacher;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen_image_activity);

        setupViews();
    }

    private void setupViews() {
        mContentView = (ImageView) findViewById(R.id.fullscreen_content);

        // Load the requested image
        String imagePath = getIntent().getExtras().getString(PATH);
        mContentView.setImageBitmap(ImageProvider.getInstance(this).getImage(imagePath, false));

        // Attach a PhotoViewAttacher, which takes care of all of the zooming functionality.
        mAttacher = new PhotoViewAttacher(mContentView);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, mContentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider.setOnVisibilityChangeListener(new OnVisibilityChangeListener() {
            @Override
            public void onVisibilityChange(boolean visible) {
                if (visible) {
                    // Schedule a hide().
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
            }
        });

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSystemUiHider.toggle();
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        delayedHide(100);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetachedFromWindow() {
        // Release any references to the ImageView
        mContentView.setImageDrawable(null);
        mContentView = null;
        super.onDetachedFromWindow();
    }

    Handler mHideHandler = new Handler();

    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

}
