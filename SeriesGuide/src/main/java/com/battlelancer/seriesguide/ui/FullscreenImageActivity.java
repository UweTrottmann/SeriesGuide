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

import android.app.ActionBar;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.SystemUiHider;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Displays a full screen image of a TV show's poster, or the image provided for a specific
 * episode. If a URI instead of a file name is provided, it will be attempted to load the image
 * from
 * the internet.
 */
public class FullscreenImageActivity extends FragmentActivity {

    public interface InitBundle {
        String IMAGE_PATH = "fullscreenimageactivity.intent.extra.image";
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
        setContentView(R.layout.activity_fullscreen_image);

        setupActionBar();
        setupViews();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(
                getResources().getDrawable(R.drawable.background_actionbar_gradient));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setIcon(R.drawable.ic_actionbar);
        actionBar.setDisplayShowTitleEnabled(false);
    }

    private void setupViews() {
        mContentView = (PhotoView) findViewById(R.id.fullscreen_content);

        // Load the requested image
        String imagePath = getIntent().getStringExtra(InitBundle.IMAGE_PATH);
        //noinspection ConstantConditions
        if (!TextUtils.isEmpty(imagePath) && imagePath.startsWith("http")) {
            // load from network, typically for high resolution show posters
            ServiceUtils.getPicasso(this).load(imagePath).into(mContentView);
        } else {
            // load from network or external cache, typically for episode images
            Picasso picasso = ServiceUtils.getExternalPicasso(this);
            if (picasso == null) {
                mContentView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                mContentView.setImageResource(R.drawable.ic_image_missing);
            } else {
                picasso.load(TheTVDB.buildScreenshotUrl(imagePath))
                        .error(R.drawable.ic_image_missing)
                        .into(mContentView, new Callback() {
                            @Override
                            public void onSuccess() {
                                mContentView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            }

                            @Override
                            public void onError() {
                                mContentView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            }
                        });
            }
        }

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

    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing()) {
            Picasso picasso = ServiceUtils.getExternalPicasso(this);
            if (picasso != null) {
                // Always cancel the request here, this is safe to call even if the image has been loaded.
                // This ensures that the anonymous callback we have does not prevent the activity from
                // being garbage collected. It also prevents our callback from getting invoked even after the
                // activity has finished.
                picasso.cancelRequest(mContentView);
            }
        }
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
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right_exit);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
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
