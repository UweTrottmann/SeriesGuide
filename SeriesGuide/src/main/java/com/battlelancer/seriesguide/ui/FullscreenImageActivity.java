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

import uk.co.senab.photoview.PhotoView;
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
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    /**
     * Displays the poster or episode preview
     */
    private PhotoView mContentView;

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
        mContentView = (PhotoView) findViewById(R.id.fullscreen_content);

        // Load the requested image
        String imagePath = getIntent().getExtras().getString(PATH);
        mContentView.setImageBitmap(ImageProvider.getInstance(this).getImage(imagePath, false));

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, mContentView,
                SystemUiHider.FLAG_FULLSCREEN);
        mSystemUiHider.setup();

        mContentView.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
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
