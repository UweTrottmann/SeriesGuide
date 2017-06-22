package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.AsyncTaskCompat;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.tasks.AddListTask;
import com.battlelancer.seriesguide.util.tasks.ChangeListItemListsTask;
import com.battlelancer.seriesguide.util.tasks.RemoveListItemTask;
import com.battlelancer.seriesguide.util.tasks.RemoveListTask;
import com.battlelancer.seriesguide.util.tasks.RenameListTask;
import com.battlelancer.seriesguide.util.tasks.ReorderListsTask;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Helper tools for SeriesGuide lists.
 */
public class ListsTools {

    private static final String[] SELECTION_ARG = new String[1];

    public interface Query {
        String[] PROJECTION_LIST_ID = new String[] {
                SeriesGuideContract.Lists.LIST_ID
        };
        String[] PROJECTION_LIST = new String[] {
                SeriesGuideContract.Lists.LIST_ID,
                SeriesGuideContract.Lists.NAME,
                SeriesGuideContract.Lists.ORDER
        };
        int LIST_ID = 0;
        int NAME = 1;
        int ORDER = 2;

        String[] PROJECTION_LIST_ITEMS = new String[] {
                SeriesGuideContract.ListItems.LIST_ITEM_ID
        };
        int LIST_ITEM_ID = 0;
    }

    private ListsTools() {
    }

    public static void addList(@NonNull Context context, @NonNull String listName) {
        AsyncTaskCompat.executeParallel(new AddListTask(context, listName));
    }

    public static void renameList(@NonNull Context context, @NonNull String listId,
            @NonNull String listName) {
        AsyncTaskCompat.executeParallel(new RenameListTask(context, listId, listName));
    }

    public static void removeList(@NonNull Context context, @NonNull String listId) {
        AsyncTaskCompat.executeParallel(new RemoveListTask(context, listId));
    }

    public static void reorderLists(@NonNull Context context,
            @NonNull List<String> listIdsInOrder) {
        AsyncTaskCompat.executeParallel(new ReorderListsTask(context, listIdsInOrder));
    }

    public static void changeListsOfItem(@NonNull Context context, int itemTvdbId, int itemType,
            @NonNull List<String> addToTheseLists, @NonNull List<String> removeFromTheseLists) {
        AsyncTaskCompat.executeParallel(
                new ChangeListItemListsTask(context, itemTvdbId, itemType, addToTheseLists,
                        removeFromTheseLists));
    }

    public static void removeListItem(@NonNull Context context, @NonNull String listItemId) {
        AsyncTaskCompat.executeParallel(new RemoveListItemTask(context, listItemId));
    }

    /**
     * Returns a all list ids in the local database.
     *
     * @return null if there was an error, empty list if there are no lists.
     */
    public static HashSet<String> getListIds(Context context) {
        Cursor query = context.getContentResolver().query(SeriesGuideContract.Lists.CONTENT_URI,
                Query.PROJECTION_LIST_ID, null, null, null);
        if (query == null) {
            return null;
        }

        HashSet<String> listIds = new HashSet<>();
        while (query.moveToNext()) {
            listIds.add(query.getString(Query.LIST_ID));
        }

        query.close();

        return listIds;
    }

    /**
     * Returns a all list item ids of the given list in the local database.
     *
     * @return null if there was an error, empty list if there are no list items in this list.
     */
    public static HashSet<String> getListItemIds(Context context, String listId) {
        SELECTION_ARG[0] = listId;
        Cursor query = context.getContentResolver().query(SeriesGuideContract.ListItems.CONTENT_URI,
                Query.PROJECTION_LIST_ITEMS,
                SeriesGuideContract.ListItems.SELECTION_LIST,
                SELECTION_ARG, null);
        if (query == null) {
            return null;
        }

        HashSet<String> listItemIds = new HashSet<>();
        while (query.moveToNext()) {
            listItemIds.add(query.getString(Query.LIST_ITEM_ID));
        }

        query.close();

        return listItemIds;
    }

    @Nullable
    public static List<SgListItem> getListItems(Context context, String listId) {
        SELECTION_ARG[0] = listId;
        Cursor query = context.getContentResolver()
                .query(SeriesGuideContract.ListItems.CONTENT_URI,
                        Query.PROJECTION_LIST_ITEMS,
                        SeriesGuideContract.ListItems.SELECTION_LIST,
                        SELECTION_ARG, null);
        if (query == null) {
            return null; // query failed
        }

        int itemCount = query.getCount();
        if (itemCount == 0) {
            query.close();
            return null; // no items in this list
        }

        List<SgListItem> items = new ArrayList<>(itemCount);
        while (query.moveToNext()) {
            SgListItem item = new SgListItem();
            item.setListItemId(query.getString(Query.LIST_ITEM_ID));
            items.add(item);
        }

        query.close();

        return items;
    }
}
