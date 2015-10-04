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

package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.OverviewFragment;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Transformation;
import com.uwetrottmann.androidutils.AndroidUtils;

import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.IOException;

import timber.log.Timber;

import static android.graphics.Shader.TileMode;

/**
 * Helpers for creating launcher shortcuts
 */
public final class ShortcutUtils {

    /** {@link Intent} action used to create the shortcut */
    private static final String ACTION_INSTALL_SHORTCUT
            = "com.android.launcher.action.INSTALL_SHORTCUT";

    /** This class is never initialized */
    private ShortcutUtils() {
    }

    /**
     * Adds a shortcut from the overview page of the given show to the Home screen.
     *
     * @param showTitle The name of the shortcut.
     * @param posterPath A TVDb show poster path.
     * @param showTvdbId The TVDb ID of the show.
     */
    public static void createShortcut(Context localContext, final String showTitle,
            final String posterPath, final int showTvdbId) {
        final Context context = localContext.getApplicationContext();

        AsyncTask<Void, Void, Intent> shortCutTask = new AsyncTask<Void, Void, Intent>() {

            @Override
            protected Intent doInBackground(Void... unused) {
                // Try to get the show poster
                Bitmap posterBitmap;

                try {
                    final String posterUrl = TheTVDB.buildPosterUrl(posterPath);
                    posterBitmap = ServiceUtils.getPicasso(context)
                            .load(posterUrl)
                            .centerCrop()
                            .memoryPolicy(MemoryPolicy.NO_STORE)
                            .networkPolicy(NetworkPolicy.NO_STORE)
                            .resizeDimen(R.dimen.show_poster_small_width,
                                    R.dimen.show_poster_small_height)
                            .transform(new RoundedCornerTransformation(posterUrl, 10f))
                            .get();
                } catch (IOException e) {
                    Timber.e(e, "Could not load show poster for shortcut " + posterPath);
                    posterBitmap = null;
                }

                // Intent used when the icon is touched
                final Intent shortcutIntent = new Intent(context, OverviewActivity.class);
                shortcutIntent.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, showTvdbId);
                shortcutIntent.setAction(Intent.ACTION_MAIN);
                shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // Intent that actually creates the shortcut
                final Intent intent = new Intent();
                intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, showTitle);
                if (posterBitmap == null) {
                    // Fall back to the app icon
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                            ShortcutIconResource.fromContext(context, R.drawable.ic_launcher));
                } else {
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, posterBitmap);
                }
                intent.setAction(ACTION_INSTALL_SHORTCUT);
                context.sendBroadcast(intent);

                return null;
            }
        };
        // Do all the above async
        AndroidUtils.executeOnPool(shortCutTask);
    }

    /** A {@link Transformation} used to draw a {@link Bitmap} with round corners */
    private static final class RoundedCornerTransformation implements Transformation {

        /** A key used to uniquely identify this {@link Transformation} */
        private final String mKey;
        /** The corner radius */
        private final float mRadius;

        /** Constructor for {@code RoundedCornerTransformation} */
        private RoundedCornerTransformation(@NonNull String key, float radius) {
            mKey = key;
            mRadius = radius;
        }

        @Override
        public Bitmap transform(Bitmap source) {
            final int w = source.getWidth();
            final int h = source.getHeight();

            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            p.setShader(new BitmapShader(source, TileMode.CLAMP, TileMode.CLAMP));

            final Bitmap transformed = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(transformed);
            c.drawRoundRect(new RectF(0f, 0f, w, h), mRadius, mRadius, p);

            // Picasso requires the original Bitmap to be recycled if we aren't returning it
            source.recycle();
            source = null;

            // Release any references to avoid memory leaks
            p.setShader(null);
            c.setBitmap(null);
            p = null;
            c = null;
            return transformed;
        }

        @Override
        public String key() {
            return mKey;
        }
    }
}
