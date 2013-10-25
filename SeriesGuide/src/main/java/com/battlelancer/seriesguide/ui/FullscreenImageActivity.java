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
 * Modified by Uwe Trottmann.
 * 
 */

package com.battlelancer.seriesguide.ui;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.SystemUiHider;
import com.uwetrottmann.seriesguide.R;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * This {@link Activity} is used to display a full screen image of a TV show's
 * poster, or the image provided for a specific episode.
 */
public class FullscreenImageActivity extends SherlockFragmentActivity {

    public interface InitBundle {
        String IMAGE_PATH = "fullscreenimageactivity.intent.extra.image";
        String IMAGE_TITLE = "fullscreenimageactivity.intent.extra.title";
        String IMAGE_SUBTITLE = "fullscreenimageactivity.intent.extra.subtitle";
    }

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
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen_image_activity);

        setupActionBar();
        setupViews();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources()
                .getColor(R.color.black_overlay)));

        // set a title and subtitle if available
        String title = getIntent().getExtras().getString(InitBundle.IMAGE_TITLE);
        if (TextUtils.isEmpty(title)) {
            actionBar.setDisplayShowTitleEnabled(false);
        } else {
            actionBar.setTitle(title);
            String subtitle = getIntent().getExtras().getString(InitBundle.IMAGE_SUBTITLE);
            if (subtitle != null) actionBar.setSubtitle(subtitle);
        }
    }

    private void setupViews() {
        mContentView = (PhotoView) findViewById(R.id.fullscreen_content);

        // Load the requested image
        String imagePath = getIntent().getExtras().getString(InitBundle.IMAGE_PATH);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
