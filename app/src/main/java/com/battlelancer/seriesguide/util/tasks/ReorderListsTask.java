package com.battlelancer.seriesguide.util.tasks;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.seriesguide.backend.lists.Lists;
import com.uwetrottmann.seriesguide.backend.lists.model.SgList;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

/**
 * Task to reorder all lists.
 */
public class ReorderListsTask extends BaseActionTask {

    @NonNull private final List<String> listIdsInOrder;

    /**
     * The given lists order number will be changed to their position in the given list.
     */
    public ReorderListsTask(@NonNull Context context, @NonNull List<String> listIdsInOrder) {
        super(context);
        this.listIdsInOrder = listIdsInOrder;
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

            // send lists with updated order to hexagon
            SgListList wrapper = new SgListList();
            List<SgList> lists = buildListsList(listIdsInOrder);
            wrapper.setLists(lists);
            try {
                listsService.save(wrapper).execute();
            } catch (IOException e) {
                Errors.logAndReportHexagon("reorder lists", e);
                return ERROR_HEXAGON_API;
            }
        }

        // update local state
        if (!doDatabaseUpdate()) {
            return ERROR_DATABASE;
        }

        return SUCCESS;
    }

    @NonNull
    private List<SgList> buildListsList(List<String> listsToChange) {
        List<SgList> lists = new ArrayList<>(listsToChange.size());
        for (int position = 0; position < listsToChange.size(); position++) {
            String listId = listsToChange.get(position);
            SgList list = new SgList();
            list.setListId(listId);
            list.setOrder(position);
            lists.add(list);
        }
        return lists;
    }

    private boolean doDatabaseUpdate() {
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (int position = 0; position < listIdsInOrder.size(); position++) {
            String listId = listIdsInOrder.get(position);
            batch.add(ContentProviderOperation.newUpdate(
                    SeriesGuideContract.Lists.buildListUri(listId))
                    .withValue(SeriesGuideContract.Lists.ORDER, position)
                    .build());
        }

        try {
            DBUtils.applyInSmallBatches(getContext(), batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "doDatabaseUpdate: failed to save reordered lists.");
            return false;
        }
        return true;
    }

    @Override
    protected int getSuccessTextResId() {
        return 0; // display no success message
    }
}