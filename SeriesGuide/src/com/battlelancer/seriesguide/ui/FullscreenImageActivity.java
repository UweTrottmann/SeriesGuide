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
 */

package com.battlelancer.seriesguide.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.battlelancer.seriesguide.util.ImageProvider;
import com.uwetrottmann.seriesguide.R;

/**
 * This {@link Activity} is used to display a full screen image of a TV show's
 * poster, or the image provided for a specific episode.
 * 
 * @author Andrew Neal (andrew@seeingpixels.org)
 */
public class FullscreenImageActivity extends Activity {

    /** The {@link Intent} extra used to deliver the path to the requested image */
    public static final String PATH = "fullscreenimageactivity.intent.extra.image";

    /** Displays the poster or episode preview */
    private ImageView mImage;

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
        mImage = (ImageView) findViewById(R.id.fullscreenImageView);

        // Load the requested image
        String imagePath = getIntent().getExtras().getString(PATH);
        mImage.setImageBitmap(ImageProvider.getInstance(this).getImage(imagePath, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetachedFromWindow() {
        // Release any references to the ImageView
        mImage.setImageDrawable(null);
        mImage = null;
        super.onDetachedFromWindow();
    }

}
