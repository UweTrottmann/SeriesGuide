package com.battlelancer.seriesguide.util.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.seriesguide.backend.lists.Lists;
import com.uwetrottmann.seriesguide.backend.lists.model.SgList;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Task to add a new list.
 */
public class AddListTask extends BaseActionTask {

    @NonNull protected final String listName;

    public AddListTask(@NonNull Context context, @NonNull String listName) {
        super(context);
        this.listName = listName;
    }

    @Override
    protected boolean isSendingToTrakt() {
        return false;
    }

    @Override
    protected Integer doBackgroundAction(Void... params) {
        String listId = getListId();

        if (isSendingToHexagon()) {
            HexagonTools hexagonTools = SgApp.getServicesComponent(getContext()).hexagonTools();
            Lists listsService = hexagonTools.getListsService();
            if (listsService == null) {
                return ERROR_HEXAGON_API; // no longer signed in
            }

            // send list to be added to hexagon
            SgListList wrapper = new SgListList();
            List<SgList> lists = buildList(listId, listName);
            wrapper.setLists(lists);
            try {
                listsService.save(wrapper).execute();
            } catch (IOException e) {
                Errors.logAndReportHexagon("add list", e);
                return ERROR_HEXAGON_API;
            }
        }

        // update local state
        if (!doDatabaseUpdate(getContext().getContentResolver(), listId)) {
            return ERROR_DATABASE;
        }

        return SUCCESS;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public String getListId() {
        return SeriesGuideContract.Lists.generateListId(listName);
    }

    @NonNull
    private static List<SgList> buildList(@NonNull String listId, @NonNull String listName) {
        List<SgList> lists = new ArrayList<>(1);
        SgList list = new SgList();
        list.setListId(listId);
        list.setName(listName);
        lists.add(list);
        return lists;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public boolean doDatabaseUpdate(ContentResolver contentResolver, String listId) {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Lists.LIST_ID, listId);
        values.put(SeriesGuideContract.Lists.NAME, listName);
        // default value
        values.put(SeriesGuideContract.Lists.ORDER, 0);
        contentResolver.insert(SeriesGuideContract.Lists.CONTENT_URI, values);
        return true;
    }

    @Override
    protected int getSuccessTextResId() {
        return 0; // display no success message
    }
}
