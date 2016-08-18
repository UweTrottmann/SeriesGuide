package com.battlelancer.seriesguide.util;

import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.widget.PopupMenu;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.RemoveShowDialogFragment;

/**
* Item click listener for show item popup menu.
*/
public class ShowMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

    private final SgApp app;
    private final FragmentManager fragmentManager;
    private final int showTvdbId;
    private final int episodeTvdbId;
    private final String logTag;

    public ShowMenuItemClickListener(SgApp app, FragmentManager fm, int showTvdbId,
            int episodeTvdbId, String logTag) {
        this.app = app;
        this.fragmentManager = fm;
        this.showTvdbId = showTvdbId;
        this.episodeTvdbId = episodeTvdbId;
        this.logTag = logTag;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_shows_watched_next: {
                DBUtils.markNextEpisode(app, showTvdbId, episodeTvdbId);
                Utils.trackContextMenu(app, logTag, "Mark next episode");
                return true;
            }
            case R.id.menu_action_shows_favorites_add: {
                ShowTools.get(app).storeIsFavorite(showTvdbId, true);
                Utils.trackContextMenu(app, logTag, "Favorite show");
                return true;
            }
            case R.id.menu_action_shows_favorites_remove: {
                ShowTools.get(app).storeIsFavorite(showTvdbId, false);
                Utils.trackContextMenu(app, logTag, "Unfavorite show");
                return true;
            }
            case R.id.menu_action_shows_hide: {
                ShowTools.get(app).storeIsHidden(showTvdbId, true);
                Utils.trackContextMenu(app, logTag, "Hide show");
                return true;
            }
            case R.id.menu_action_shows_unhide: {
                ShowTools.get(app).storeIsHidden(showTvdbId, false);
                Utils.trackContextMenu(app, logTag, "Unhide show");
                return true;
            }
            case R.id.menu_action_shows_manage_lists: {
                ManageListsDialogFragment.showListsDialog(showTvdbId,
                        SeriesGuideContract.ListItemTypes.SHOW,
                        fragmentManager);
                Utils.trackContextMenu(app, logTag, "Manage lists");
                return true;
            }
            case R.id.menu_action_shows_update: {
                SgSyncAdapter.requestSyncImmediate(app,
                        SgSyncAdapter.SyncType.SINGLE, showTvdbId, true);
                Utils.trackContextMenu(app, logTag, "Update show");
                return true;
            }
            case R.id.menu_action_shows_remove: {
                if (!SgSyncAdapter.isSyncActive(app, true)) {
                    RemoveShowDialogFragment.show(fragmentManager, showTvdbId);
                }
                Utils.trackContextMenu(app, logTag, "Delete show");
                return true;
            }
            default:
                return false;
        }
    }

}
