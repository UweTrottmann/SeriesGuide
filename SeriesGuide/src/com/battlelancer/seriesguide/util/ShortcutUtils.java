/*
 * Copyright (C) 2013 Andrew Neal
 */

package com.battlelancer.seriesguide.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.OverviewFragment;

/**
 * @author Andrew Neal (andrew@seeingpixels.org)
 */
public final class ShortcutUtils {

    /** {@link Intent} action used to create the shortcut */
    private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    /** Shortcut icon size */
    private static final int ICON_SIZE = 128;

    /* This class is never initialized */
    private ShortcutUtils() {
    }

    /**
     * Used to add a shortcut to the Launcher app
     * 
     * @param activity The {@link Activity} to use
     * @param name The name to give the shortcut
     * @param path The path to the cached image
     * @param id The id of the show
     */
    public static void createShortcut(Activity activity, String name, String path, int id) {
        // The shortcut icon
        Bitmap icon = ImageProvider.getInstance(activity).getImage(path, false);

        // Intent used when the icon is touched
        final Intent shortcutIntent = new Intent(activity, OverviewActivity.class);
        shortcutIntent.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, id);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Intent that actually creates the shortcut
        final Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, resizeAndCropCenter(icon, ICON_SIZE));
        intent.setAction(ACTION_INSTALL_SHORTCUT);
        activity.sendBroadcast(intent);
    }

    /**
     * Used to create launcher shortcut icons
     * 
     * @param bitmap The bitmap to resize
     * @param size The new size of the bitmap
     * @return A new bitmap that has been resized and center cropped
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
