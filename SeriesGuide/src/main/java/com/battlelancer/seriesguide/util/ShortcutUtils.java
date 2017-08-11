package com.battlelancer.seriesguide.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;
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
        // do not pass activity reference to AsyncTask, activity might leak if destroyed
        final Context context = localContext.getApplicationContext();

        AsyncTask<Void, Void, Void> shortCutTask = new AsyncTask<Void, Void, Void>() {

            @Nullable private Bitmap posterBitmap;

            @Override
            protected Void doInBackground(Void... unused) {
                // Try to get the show poster
                try {
                    final String posterUrl = TvdbImageTools.smallSizeUrl(posterPath);
                    if (posterUrl != null) {
                        RequestCreator requestCreator = Picasso.with(context)
                                .load(posterUrl)
                                .centerCrop()
                                .memoryPolicy(MemoryPolicy.NO_STORE)
                                .networkPolicy(NetworkPolicy.NO_STORE)
                                .resizeDimen(R.dimen.show_poster_width_shortcut,
                                        R.dimen.show_poster_height_shortcut);
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            // on O+ we use 108x108dp adaptive icon, no need to cut its corners
                            // pre-O full bitmap is displayed, so cut corners for nicer icon shape
                            requestCreator.transform(
                                    new RoundedCornerTransformation(posterUrl, 10f));
                        }
                        posterBitmap = requestCreator.get();
                    }
                } catch (IOException e) {
                    Timber.e(e, "Could not load show poster for shortcut %s", posterPath);
                    posterBitmap = null;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                // Intent used when the shortcut is tapped
                final Intent shortcutIntent = new Intent(context, OverviewActivity.class);
                shortcutIntent.putExtra(OverviewActivity.EXTRA_INT_SHOW_TVDBID, showTvdbId);
                shortcutIntent.setAction(Intent.ACTION_MAIN);
                shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ShortcutManager shortcutManager =
                            context.getSystemService(ShortcutManager.class);
                    if (shortcutManager.isRequestPinShortcutSupported()) {
                        ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context,
                                "shortcut-show-" + showTvdbId)
                                .setIntent(shortcutIntent)
                                .setShortLabel(showTitle);
                        if (posterBitmap == null) {
                            builder.setIcon(
                                    Icon.createWithResource(context, R.drawable.ic_shortcut_show));
                        } else {
                            builder.setIcon(Icon.createWithAdaptiveBitmap(posterBitmap));
                        }
                        ShortcutInfo pinShortcutInfo = builder.build();

                        // Create the PendingIntent object only if your app needs to be notified
                        // that the user allowed the shortcut to be pinned. Note that, if the
                        // pinning operation fails, your app isn't notified. We assume here that the
                        // app has implemented a method called createShortcutResultIntent() that
                        // returns a broadcast intent.
                        Intent pinnedShortcutCallbackIntent =
                                shortcutManager.createShortcutResultIntent(pinShortcutInfo);

                        // Configure the intent so that your app's broadcast receiver gets
                        // the callback successfully.
                        PendingIntent successCallback = PendingIntent.getBroadcast(context, 0,
                                pinnedShortcutCallbackIntent, 0);

                        shortcutManager.requestPinShortcut(pinShortcutInfo,
                                successCallback.getIntentSender());
                    }
                } else {
                    // Intent that actually creates the shortcut
                    final Intent intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, showTitle);
                    if (posterBitmap == null) {
                        // Fall back to the app icon
                        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                                Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_app));
                    } else {
                        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, posterBitmap);
                    }
                    intent.setAction(ACTION_INSTALL_SHORTCUT);
                    context.sendBroadcast(intent);

                    // drop to home screen, launcher should animate to new shortcut
                    context.startActivity(
                            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            }
        };
        // Do all the above async
        shortCutTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
            //noinspection UnusedAssignment
            source = null;

            // Release any references to avoid memory leaks
            p.setShader(null);
            c.setBitmap(null);
            //noinspection UnusedAssignment
            p = null;
            //noinspection UnusedAssignment
            c = null;
            return transformed;
        }

        @Override
        public String key() {
            return mKey;
        }
    }
}
