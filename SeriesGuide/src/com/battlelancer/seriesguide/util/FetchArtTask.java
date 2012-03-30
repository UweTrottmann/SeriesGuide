
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.thetvdbapi.ImageCache;
import com.battlelancer.thetvdbapi.TheTVDB;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class FetchArtTask extends AsyncTask<Void, Void, Bitmap> {

    private String mPath;

    private ImageView mImageView;

    private Context mContext;

    private View mProgressContainer;

    private View mContainer;

    private ImageCache mImageCache;

    private boolean mAnimate;

    public FetchArtTask(String path, View container, Context context) {
        mPath = path;
        mContainer = container;
        mContext = context;
        mImageCache = ImageCache.getInstance(context);
    }

    @Override
    protected void onPreExecute() {
        // immediately hide container if there is no image
        if (mPath.length() == 0) {
            mContainer.setVisibility(View.GONE);
            return;
        }

        mImageView = (ImageView) mContainer.findViewById(R.id.ImageViewEpisodeImage);
        mProgressContainer = mContainer.findViewById(R.id.progress_container);
        // only make progress container visible if we will do long running op
        if (!mImageCache.isCached(mPath)) {
            mProgressContainer.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.GONE);
        }
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        if (isCancelled()) {
            return null;
        }

        if (mPath.length() != 0) {

            final Bitmap bitmap = mImageCache.get(mPath);
            if (bitmap != null) {
                // image is in cache
                mAnimate = false;
                return bitmap;

            } else {

                if (isCancelled()) {
                    return null;
                }

                // download image from TVDb
                if (TheTVDB.fetchArt(mPath, false, mContext)) {
                    mAnimate = true;
                    return mImageCache.get(mPath);
                }

            }
        }

        return null;
    }

    @Override
    protected void onCancelled(Bitmap result) {
        releaseReferences();
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            mImageView.setImageBitmap(bitmap);

            // make image view visible
            if (mAnimate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(mContext,
                        android.R.anim.fade_out));
                mImageView.startAnimation(AnimationUtils.loadAnimation(mContext,
                        android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mImageView.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mImageView.setVisibility(View.VISIBLE);
        } else {
            mContainer.setVisibility(View.GONE);
        }

        releaseReferences();
    }

    private void releaseReferences() {
        mContext = null;
        mContainer = null;
        mProgressContainer = null;
        mImageView = null;
        mImageCache = null;
    }
}
