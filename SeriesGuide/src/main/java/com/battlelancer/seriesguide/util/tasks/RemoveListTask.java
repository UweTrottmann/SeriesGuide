package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.ListsActivity;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.lists.Lists;
import org.greenrobot.eventbus.EventBus;
import java.io.IOException;

/**
 * Task to remove a list.
 */
public class RemoveListTask extends BaseActionTask {

    @NonNull protected final String listId;

    public RemoveListTask(@NonNull Context context, @NonNull String listId) {
        super(context);
        this.listId = listId;
    }

    @Override
    protected boolean isSendingToTrakt() {
        return false;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (isSendingToHexagon()) {
            if (!AndroidUtils.isNetworkConnected(getContext())) {
                return ERROR_NETWORK;
            }

            Lists listsService = HexagonTools.getListsService(getContext());
            if (listsService == null) {
                return ERROR_HEXAGON_API; // no longer signed in
            }

            // send list to be removed from hexagon
            try {
                listsService.remove(listId).execute();
            } catch (IOException e) {
                HexagonTools.trackFailedRequest(getContext(), "remove list", e);
                return ERROR_HEXAGON_API;
            }
        }

        // update local state
        if (!doDatabaseUpdate()) {
            return ERROR_DATABASE;
        }

        return SUCCESS;
    }

    private boolean doDatabaseUpdate() {
        // delete the list
        int deleted = getContext().getContentResolver()
                .delete(SeriesGuideContract.Lists.buildListUri(listId), null, null);
        if (deleted == 0) {
            return false;
        }
        // delete all items of the list
        getContext().getContentResolver()
                .delete(SeriesGuideContract.ListItems.CONTENT_URI,
                        SeriesGuideContract.ListItems.SELECTION_LIST, new String[] {
                                listId
                        });
        // count of deleted items is not returned, so do not check

        // notify lists activity
        EventBus.getDefault().post(new ListsActivity.ListsChangedEvent());

        return true;
    }

    @Override
    protected int getSuccessTextResId() {
        return 0; // display no success message
    }
}
