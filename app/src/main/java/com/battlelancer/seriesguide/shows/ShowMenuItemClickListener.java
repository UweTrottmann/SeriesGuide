// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows;

import android.content.Context;
import android.view.MenuItem;
import android.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.lists.ManageListsDialogFragment;
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools;
import com.battlelancer.seriesguide.shows.tools.ShowSync;
import com.battlelancer.seriesguide.shows.tools.ShowTools2;

/**
* Item click listener for show item popup menu.
*/
public class ShowMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

    private final Context context;
    private final ShowTools2 showTools;
    private final FragmentManager fragmentManager;
    private final long showId;
    private final long nextEpisodeId;

    public ShowMenuItemClickListener(Context context, FragmentManager fm, long showId,
            long nextEpisodeId) {
        this.context = context;
        this.fragmentManager = fm;
        this.showId = showId;
        this.nextEpisodeId = nextEpisodeId;
        this.showTools = SgApp.getServicesComponent(context).showTools();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_shows_watched_next) {
            EpisodeTools.episodeWatchedIfNotZero(context, nextEpisodeId);
            return true;
        } else if (itemId == R.id.menu_action_shows_favorites_add) {
            showTools.storeIsFavorite(showId, true);
            return true;
        } else if (itemId == R.id.menu_action_shows_favorites_remove) {
            showTools.storeIsFavorite(showId, false);
            return true;
        } else if (itemId == R.id.menu_action_shows_hide) {
            showTools.storeIsHidden(showId, true);
            return true;
        } else if (itemId == R.id.menu_action_shows_unhide) {
            showTools.storeIsHidden(showId, false);
            return true;
        } else if (itemId == R.id.menu_action_shows_manage_lists) {
            ManageListsDialogFragment.show(fragmentManager, showId);
            return true;
        } else if (itemId == R.id.menu_action_shows_update) {
            ShowSync.triggerDeltaSync(context, showId);
            return true;
        } else if (itemId == R.id.menu_action_shows_remove) {
            RemoveShowDialogFragment.show(showId, fragmentManager, context);
            return true;
        }
        return false;
    }

}
