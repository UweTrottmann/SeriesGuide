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
                Utils.trackContextMenu(context, logTag, "Mark next episode");
                return true;
            }
            case R.id.menu_action_shows_favorites_add: {
                ShowTools.get(context).storeIsFavorite(showTvdbId, true);
                Utils.trackContextMenu(context, logTag, "Favorite show");
                return true;
            }
            case R.id.menu_action_shows_favorites_remove: {
                ShowTools.get(context).storeIsFavorite(showTvdbId, false);
                Utils.trackContextMenu(context, logTag, "Unfavorite show");
                return true;
            }
            case R.id.menu_action_shows_hide: {
                ShowTools.get(context).storeIsHidden(showTvdbId, true);
                Utils.trackContextMenu(context, logTag, "Hide show");
                return true;
            }
            case R.id.menu_action_shows_unhide: {
                ShowTools.get(context).storeIsHidden(showTvdbId, false);
                Utils.trackContextMenu(context, logTag, "Unhide show");
                return true;
            }
            case R.id.menu_action_shows_manage_lists: {
                ManageListsDialogFragment.showListsDialog(showTvdbId,
                        SeriesGuideContract.ListItemTypes.SHOW,
                        fragmentManager);
                Utils.trackContextMenu(context, logTag, "Manage lists");
                return true;
            }
            case R.id.menu_action_shows_update: {
                SgSyncAdapter.requestSyncImmediate(context,
                        SgSyncAdapter.SyncType.SINGLE, showTvdbId, true);
                Utils.trackContextMenu(context, logTag, "Update show");
                return true;
            }
            case R.id.menu_action_shows_remove: {
                if (!SgSyncAdapter.isSyncActive(context, true)) {
                    RemoveShowDialogFragment.show(fragmentManager, showTvdbId);
                }
                Utils.trackContextMenu(context, logTag, "Delete show");
                return true;
            }
            default:
                return false;
        }
    }

}
