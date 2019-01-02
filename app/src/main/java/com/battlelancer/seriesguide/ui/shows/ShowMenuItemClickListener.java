package com.battlelancer.seriesguide.ui.shows;

import android.content.Context;
import android.view.MenuItem;
import android.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.lists.ManageListsDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;

/**
* Item click listener for show item popup menu.
*/
public class ShowMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

    private final Context context;
    private final ShowTools showTools;
    private final FragmentManager fragmentManager;
    private final int showTvdbId;
    private final int episodeTvdbId;

    public ShowMenuItemClickListener(Context context, FragmentManager fm, int showTvdbId,
            int episodeTvdbId) {
        this.context = context;
        this.fragmentManager = fm;
        this.showTvdbId = showTvdbId;
        this.episodeTvdbId = episodeTvdbId;
        this.showTools = SgApp.getServicesComponent(context).showTools();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_shows_watched_next: {
                DBUtils.markNextEpisode(context, showTvdbId, episodeTvdbId);
                return true;
            }
            case R.id.menu_action_shows_favorites_add: {
                showTools.storeIsFavorite(showTvdbId, true);
                return true;
            }
            case R.id.menu_action_shows_favorites_remove: {
                showTools.storeIsFavorite(showTvdbId, false);
                return true;
            }
            case R.id.menu_action_shows_hide: {
                showTools.storeIsHidden(showTvdbId, true);
                return true;
            }
            case R.id.menu_action_shows_unhide: {
                showTools.storeIsHidden(showTvdbId, false);
                return true;
            }
            case R.id.menu_action_shows_manage_lists: {
                ManageListsDialogFragment.show(fragmentManager, showTvdbId,
                        SeriesGuideContract.ListItemTypes.SHOW);
                return true;
            }
            case R.id.menu_action_shows_update: {
                SgSyncAdapter.requestSyncSingleImmediate(context, true, showTvdbId);
                return true;
            }
            case R.id.menu_action_shows_remove: {
                RemoveShowDialogFragment.show(context, fragmentManager, showTvdbId);
                return true;
            }
            default:
                return false;
        }
    }

}
