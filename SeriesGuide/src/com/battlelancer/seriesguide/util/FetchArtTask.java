
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.R;
import com.battlelancer.thetvdbapi.ImageCache;
import com.battlelancer.thetvdbapi.TheTVDB;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

public class FetchArtTask extends AsyncTask<Void, Void, Integer> {
    private static final int SUCCESS = 0;

    private static final int ERROR = 1;

    private String mPath;

    private ImageView mImageView;

    private Context mContext;

    public FetchArtTask(String path, ImageView imageView, Context context) {
        mPath = path;
        mImageView = imageView;
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        mImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        mImageView.setImageResource(R.drawable.ic_action_refresh);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        int resultCode;

        if (isCancelled()) {
            return null;
        }

        if (TheTVDB.fetchArt(mPath, false, mContext)) {
            resultCode = SUCCESS;
        } else {
            resultCode = ERROR;
        }

        return resultCode;
    }

    @Override
    protected void onPostExecute(Integer resultCode) {
        switch (resultCode) {
            case SUCCESS:
                Bitmap bitmap = ImageCache.getInstance(mContext).get(mPath);
                if (bitmap != null) {
                    mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    mImageView.setImageBitmap(bitmap);
                    return;
                }
                // no break because image could be null (got deleted, ...)
            default:
                // fallback
                mImageView.setImageResource(R.drawable.show_generic);
                break;
        }
    }
}
