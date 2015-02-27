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

import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.OverviewFragment;
import com.squareup.picasso.NetworkPolicy;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.io.IOException;
import timber.log.Timber;

public final class ShortcutUtils {

    /** {@link Intent} action used to create the shortcut */
    private static final String ACTION_INSTALL_SHORTCUT
            = "com.android.launcher.action.INSTALL_SHORTCUT";

    /* This class is never initialized */
    private ShortcutUtils() {
    }

    /**
     * Adds a shortcut to the overview page of the given show to the Home screen.
     *
     * @param showTitle  The name of the shortcut.
     * @param posterPath A TVDb show poster path.
     * @param showTvdbId The TVDb ID of the show.
     */
    public static void createShortcut(Context localContext, final String showTitle,
            final String posterPath, final int showTvdbId) {
        final Context context = localContext.getApplicationContext();

        AsyncTask<Void, Void, Intent> shortCutTask = new AsyncTask<Void, Void, Intent>() {

            @Override
            protected Intent doInBackground(Void... params) {
                // try to get the show poster
                Bitmap posterBitmap;

                try {
                    posterBitmap = ServiceUtils.getPicasso(context)
                            .load(TheTVDB.buildPosterUrl(posterPath))
                            .networkPolicy(NetworkPolicy.NO_STORE)
                            .centerCrop()
                            .resizeDimen(R.dimen.shortcut_icon_size, R.dimen.shortcut_icon_size)
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
                    // fall back to the app icon
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
        // do all the above async
        AndroidUtils.executeOnPool(shortCutTask);
    }
}
