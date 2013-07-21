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
 * Modified by Uwe Trottmann (see git log) for better density support, other.
 */

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.OverviewFragment;
import com.uwetrottmann.seriesguide.R;

public final class ShortcutUtils {

    /** {@link Intent} action used to create the shortcut */
    private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    /* This class is never initialized */
    private ShortcutUtils() {
    }

    /**
     * Adds a shortcut to the overview page of the given show to the Home
     * screen.
     * 
     * @param showTitle The name of the shortcut.
     * @param posterPath The path to the cached (with {@link ImageProvider})
     *            image to be used for the shortcut icon.
     * @param showTvdbId The TVDb ID of the show.
     */
    public static void createShortcut(Context context, String showTitle, String posterPath,
            int showTvdbId) {
        // The shortcut icon
        Bitmap icon = ImageProvider.getInstance(context).getImage(posterPath, false);

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
        intent.putExtra(
                Intent.EXTRA_SHORTCUT_ICON,
                resizeAndCropCenter(icon,
                        context.getResources().getDimensionPixelSize(R.dimen.shortcut_icon_size)));
        intent.setAction(ACTION_INSTALL_SHORTCUT);
        context.sendBroadcast(intent);
    }

    /**
     * Resizes and center crops the given {@link Bitmap} into a square of the
     * given size.
     * 
     * @param bitmap The {@link Bitmap} to resize.
     * @param size The size in pixels of the returned {@link Bitmap}.
     */
    public static Bitmap resizeAndCropCenter(Bitmap bitmap, int size) {
        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        if (w == size && h == size) {
            return bitmap;
        }

        // Scale the image so that the shorter side equals to the target;
        // the longer side will be center-cropped
        final float scale = (float) size / Math.min(w, h);
        final float width = (size - Math.round(scale * w)) / 2f;
        final float height = (size - Math.round(scale * h)) / 2f;

        final Bitmap target = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        final Canvas canvas = new Canvas(target);
        canvas.translate(width, height);
        canvas.scale(scale, scale);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return target;
    }

}
