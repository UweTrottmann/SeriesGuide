package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.seriesguide.backend.lists.Lists;
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
    protected Integer doBackgroundAction(Void... params) {
        if (isSendingToHexagon()) {
            HexagonTools hexagonTools = SgApp.getServicesComponent(getContext()).hexagonTools();
            Lists listsService = hexagonTools.getListsService();
            if (listsService == null) {
                return ERROR_HEXAGON_API; // no longer signed in
            }

            // send list to be removed from hexagon
            try {
                listsService.remove(listId).execute();
            } catch (IOException e) {
                Errors.logAndReportHexagon("remove list", e);
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
        // delete all items of the list before list to avoid violating foreign key constraints
        getContext().getContentResolver()
                .delete(SeriesGuideContract.ListItems.CONTENT_URI,
                        SeriesGuideContract.ListItems.SELECTION_LIST, new String[] {
                                listId
                        });
        // count of deleted items is not returned, so do not check

        // delete the list
        int deleted = getContext().getContentResolver()
                .delete(SeriesGuideContract.Lists.buildListUri(listId), null, null);
        return deleted != 0;
    }

    @Override
    protected int getSuccessTextResId() {
        return 0; // display no success message
    }
}
