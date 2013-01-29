/*
 * Copyright 2011 Uwe Trottmann
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

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.battlelancer.thetvdbapi.TheTVDB;
import com.uwetrottmann.seriesguide.R;

public class FetchArtTask extends AsyncTask<Void, Void, Bitmap> {

    private String mImagePath;

    private ImageView mImageView;

    private Context mContext;

    private View mProgressContainer;

    private View mContainer;

    public FetchArtTask(String path, View container, Context context) {
        mImagePath = path;
        mContainer = container;
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        // immediately hide container if there is no image, cancel the task
        if (TextUtils.isEmpty(mImagePath)) {
            mContainer.setVisibility(View.GONE);
            cancel(false);
            return;
        }

        mContainer.setVisibility(View.VISIBLE);
        mImageView = (ImageView) mContainer.findViewById(R.id.ImageViewEpisodeImage);
        mProgressContainer = mContainer.findViewById(R.id.image_progress_container);

        if (mImageView == null || mProgressContainer == null) {
            cancel(true);
        }
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        if (isCancelled()) {
            return null;
        }

        final Bitmap bitmap = ImageProvider.getInstance(mContext).getImage(mImagePath, false);
        if (bitmap != null) {
            // image is in cache
            return bitmap;

        } else {
            // abort if we are cancelled or have no connection
            if (isCancelled() || !Utils.isAllowedConnection(mContext)) {
                return null;
            }

            // download image from TVDb
            publishProgress();
            if (TheTVDB.fetchArt(mImagePath, false, mContext)) {
                return ImageProvider.getInstance(mContext).getImage(mImagePath, false);
            }

        }

        return null;
    }

    @Override
    protected void onCancelled() {
        releaseReferences();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        if (mImageView != null && mProgressContainer != null) {
            // this will only get called if we have to download the image
            mProgressContainer.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            mImageView.setImageBitmap(bitmap);
        } else {
            // we can be sure that there must be an image here, we just couldn't
            // get it somehow, so set a place holder
            mImageView.setImageResource(R.drawable.show_generic);
        }

        // make image view visible
        if (mImageView.getVisibility() == View.GONE) {
            mProgressContainer.startAnimation(AnimationUtils.loadAnimation(mContext,
                    android.R.anim.fade_out));
            mImageView.startAnimation(AnimationUtils
                    .loadAnimation(mContext, android.R.anim.fade_in));
            mProgressContainer.setVisibility(View.GONE);
            mImageView.setVisibility(View.VISIBLE);
        }

        releaseReferences();
    }

    private void releaseReferences() {
        mContext = null;
        mContainer = null;
        mProgressContainer = null;
        mImageView = null;
    }
}
