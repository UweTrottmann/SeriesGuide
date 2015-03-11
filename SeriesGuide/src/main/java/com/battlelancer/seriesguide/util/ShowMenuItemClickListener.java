/*
 * Copyright 2015 Uwe Trottmann
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
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.widget.PopupMenu;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.RemoveShowDialogFragment;

/**
* Item click listener for show item popup menu.
*/
public class ShowMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

    private final Context context;
    private final FragmentManager fragmentManager;
    private final int showTvdbId;
    private final int episodeTvdbId;
    private final String logTag;

    public ShowMenuItemClickListener(Context context, FragmentManager fm, int showTvdbId,
            int episodeTvdbId, String logTag) {
        this.context = context;
        this.fragmentManager = fm;
        this.showTvdbId = showTvdbId;
        this.episodeTvdbId = episodeTvdbId;
        this.logTag = logTag;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_shows_watched_next: {
                DBUtils.markNextEpisode(context, showTvdbId, episodeTvdbId);
                fireTrackerEventContext("Mark next episode");
                return true;
            }
            case R.id.menu_action_shows_favorites_add: {
                ShowTools.get(context).storeIsFavorite(showTvdbId, true);
                fireTrackerEventContext("Favorite show");
                return true;
            }
            case R.id.menu_action_shows_favorites_remove: {
                ShowTools.get(context).storeIsFavorite(showTvdbId, false);
                fireTrackerEventContext("Unfavorite show");
                return true;
            }
            case R.id.menu_action_shows_hide: {
                ShowTools.get(context).storeIsHidden(showTvdbId, true);
                fireTrackerEventContext("Hide show");
                return true;
            }
            case R.id.menu_action_shows_unhide: {
                ShowTools.get(context).storeIsHidden(showTvdbId, false);
                fireTrackerEventContext("Unhide show");
                return true;
            }
            case R.id.menu_action_shows_manage_lists: {
                ManageListsDialogFragment.showListsDialog(showTvdbId,
                        SeriesGuideContract.ListItemTypes.SHOW,
                        fragmentManager);
                fireTrackerEventContext("Manage lists");
                return true;
            }
            case R.id.menu_action_shows_update: {
                SgSyncAdapter.requestSyncImmediate(context,
                        SgSyncAdapter.SyncType.SINGLE, showTvdbId, true);
                fireTrackerEventContext("Update show");
                return true;
            }
            case R.id.menu_action_shows_remove: {
                if (!SgSyncAdapter.isSyncActive(context, true)) {
                    RemoveShowDialogFragment.show(fragmentManager, showTvdbId);
                }
                fireTrackerEventContext("Delete show");
                return true;
            }
            default:
                return false;
        }
    }

    private void fireTrackerEventContext(String label) {
        Utils.trackContextMenu(context, logTag, label);
    }
}
