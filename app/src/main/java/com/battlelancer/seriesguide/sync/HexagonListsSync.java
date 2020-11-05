package com.battlelancer.seriesguide.sync;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.text.TextUtils;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.lists.ListsTools;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Errors;
import com.google.api.client.util.DateTime;
import com.uwetrottmann.seriesguide.backend.lists.Lists;
import com.uwetrottmann.seriesguide.backend.lists.model.SgList;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListIds;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListItem;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import timber.log.Timber;

public class HexagonListsSync {

    private static final int LISTS_MAX_BATCH_SIZE = 10;

    private Context context;
    private HexagonTools hexagonTools;

    public HexagonListsSync(Context context, HexagonTools hexagonTools) {
        this.context = context;
        this.hexagonTools = hexagonTools;
    }

    public boolean download(boolean hasMergedLists) {
        long currentTime = System.currentTimeMillis();
        DateTime lastSyncTime = new DateTime(HexagonSettings.getLastListsSyncTime(context));

        if (hasMergedLists) {
            Timber.d("download: lists changed since %s.", lastSyncTime);
        } else {
            Timber.d("download: all lists.");
        }

        HashSet<String> localListIds = ListsTools.getListIds(context);
        List<SgList> lists;
        String cursor = null;
        do {
            try {
                // get service each time to check if auth was removed
                Lists listsService = hexagonTools.getListsService();
                if (listsService == null) {
                    return false; // no longer signed in
                }

                Lists.Get request = listsService.get(); // use default server limit
                if (hasMergedLists) {
                    request.setUpdatedSince(lastSyncTime);
                }
                if (!TextUtils.isEmpty(cursor)) {
                    request.setCursor(cursor);
                }

                SgListList response = request.execute();
                if (response == null) {
                    Timber.d("download: failed, response is null.");
                    break;
                }

                cursor = response.getCursor();
                lists = response.getLists();
            } catch (IOException | IllegalArgumentException e) {
                // Note: JSON parser may throw IllegalArgumentException.
                Errors.logAndReportHexagon("get lists", e);
                return false;
            }

            if (lists == null || lists.size() == 0) {
                break; // empty response, assume we are done
            }

            if (!doListsDatabaseUpdate(lists, localListIds, hasMergedLists)) {
                return false; // database update failed, abort
            }
        } while (!TextUtils.isEmpty(cursor)); // fetch next batch

        // set new last sync time
        if (hasMergedLists) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(HexagonSettings.KEY_LAST_SYNC_LISTS, currentTime)
                    .apply();
        }

        return true;
    }

    private boolean doListsDatabaseUpdate(List<SgList> lists, HashSet<String> localListIds,
            boolean hasMergedLists) {
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (SgList list : lists) {
            // add or update the list
            String listId = list.getListId();
            ContentProviderOperation.Builder builder = null;
            if (localListIds.contains(listId)) {
                // update
                if (hasMergedLists) {
                    // only overwrite name and order if data was already merged
                    // use case: user disconnected for a while, changed lists, then reconnects
                    builder = ContentProviderOperation
                            .newUpdate(SeriesGuideContract.Lists.buildListUri(listId));
                }
            } else {
                // insert
                builder = ContentProviderOperation
                        .newInsert(SeriesGuideContract.Lists.CONTENT_URI)
                        .withValue(SeriesGuideContract.Lists.LIST_ID, listId);
            }
            if (builder != null) {
                builder.withValue(SeriesGuideContract.Lists.NAME, list.getName());
                if (list.getOrder() != null) {
                    builder.withValue(SeriesGuideContract.Lists.ORDER, list.getOrder());
                }
                batch.add(builder.build());
            }

            // keep track of items not in the list on hexagon
            HashSet<String> listItemsToRemove = null;
            if (hasMergedLists) {
                listItemsToRemove = ListsTools.getListItemIds(context, listId);
                if (listItemsToRemove == null) {
                    return false; // list item query failed
                }
            }
            // add or update items of the list
            List<SgListItem> listItems = list.getListItems();
            if (listItems != null) {
                for (SgListItem listItem : listItems) {
                    String listItemId = listItem.getListItemId();
                    String[] brokenUpId = SeriesGuideContract.ListItems.splitListItemId(listItemId);
                    if (brokenUpId == null) {
                        continue; // could not break up list item id
                    }
                    int itemTvdbId = -1;
                    int itemType = -1;
                    try {
                        itemTvdbId = Integer.parseInt(brokenUpId[0]);
                        itemType = Integer.parseInt(brokenUpId[1]);
                    } catch (NumberFormatException ignored) {
                    }
                    if (itemTvdbId == -1 || !SeriesGuideContract.ListItems.isValidItemType(
                            itemType)) {
                        continue; // failed to extract item TVDB id or item type not known
                    }

                    // just insert the list item, if the id already exists it will be replaced
                    builder = ContentProviderOperation
                            .newInsert(SeriesGuideContract.ListItems.CONTENT_URI)
                            .withValue(SeriesGuideContract.ListItems.LIST_ITEM_ID, listItemId)
                            .withValue(SeriesGuideContract.ListItems.ITEM_REF_ID, itemTvdbId)
                            .withValue(SeriesGuideContract.ListItems.TYPE, itemType)
                            .withValue(SeriesGuideContract.Lists.LIST_ID, listId);
                    batch.add(builder.build());

                    if (hasMergedLists) {
                        // do not remove this list item
                        listItemsToRemove.remove(listItemId);
                    }
                }
            }
            if (hasMergedLists) {
                // remove items no longer in the list
                for (String listItemId : listItemsToRemove) {
                    builder = ContentProviderOperation
                            .newDelete(SeriesGuideContract.ListItems.buildListItemUri(listItemId));
                    batch.add(builder.build());
                }
            }
        }

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "doListsDatabaseUpdate: failed.");
            return false;
        }

        return true;
    }

    public boolean pruneRemovedLists() {
        Timber.d("pruneRemovedLists");
        HashSet<String> localListIds = ListsTools.getListIds(context);
        if (localListIds == null) {
            return false; // query failed
        }
        if (localListIds.size() <= 1) {
            return true; // one or no list, can not remove any list
        }

        // get list of ids of lists on hexagon
        List<String> hexagonListIds = new ArrayList<>(localListIds.size());
        String cursor = null;
        do {
            try {
                // get service each time to check if auth was removed
                Lists listsService = hexagonTools.getListsService();
                if (listsService == null) {
                    return false; // no longer signed in
                }

                Lists.GetIds request = listsService.getIds();
                if (!TextUtils.isEmpty(cursor)) {
                    request.setCursor(cursor);
                }

                SgListIds response = request.execute();
                if (response == null) {
                    Timber.d("pruneRemovedLists: failed, response is null.");
                    return false;
                }

                List<String> listIds = response.getListIds();
                if (listIds == null || listIds.size() == 0) {
                    break; // empty response, assume we got all ids
                }
                hexagonListIds.addAll(listIds);

                cursor = response.getCursor();
            } catch (IOException | IllegalArgumentException e) {
                // Note: JSON parser may throw IllegalArgumentException.
                Errors.logAndReportHexagon("get list ids", e);
                return false;
            }
        } while (!TextUtils.isEmpty(cursor)); // fetch next batch

        if (hexagonListIds.size() <= 1) {
            return true; // one or no list on hexagon, can not remove any list
        }

        // exclude any lists that are on hexagon
        for (String listId : hexagonListIds) {
            localListIds.remove(listId);
        }

        // remove any list not on hexagon
        if (localListIds.size() > 0) {
            ArrayList<ContentProviderOperation> batch = new ArrayList<>();
            for (String listId : localListIds) {
                // note: this matches what RemoveListTask does
                // delete all list items before the list to avoid violating foreign key constraints
                batch.add(ContentProviderOperation
                        .newDelete(SeriesGuideContract.ListItems.CONTENT_URI)
                        .withSelection(SeriesGuideContract.ListItems.SELECTION_LIST,
                                new String[]{listId})
                        .build());
                // delete list
                batch.add(ContentProviderOperation
                        .newDelete(SeriesGuideContract.Lists.buildListUri(listId))
                        .build());
            }
            try {
                DBUtils.applyInSmallBatches(context, batch);
            } catch (OperationApplicationException e) {
                Timber.e(e, "pruneRemovedLists: deleting lists failed.");
                return false;
            }
        }

        return true;
    }

    public boolean uploadAll() {
        Timber.d("uploadAll");

        SgListList listsWrapper = new SgListList();
        List<SgList> lists = new ArrayList<>(LISTS_MAX_BATCH_SIZE);
        listsWrapper.setLists(lists);

        Cursor listsQuery = context.getContentResolver()
                .query(SeriesGuideContract.Lists.CONTENT_URI,
                        ListsTools.Query.PROJECTION_LIST, null, null, null);
        if (listsQuery == null) {
            return false; // query failed
        }

        while (listsQuery.moveToNext()) {
            SgList list = new SgList();
            // add list properties
            String listId = listsQuery.getString(ListsTools.Query.LIST_ID);
            String listName = listsQuery.getString(ListsTools.Query.NAME);
            if (TextUtils.isEmpty(listId)) {
                continue; // skip, no list id
            }
            list.setListId(listId);
            list.setName(listName);
            int order = listsQuery.getInt(ListsTools.Query.ORDER);
            if (order != 0) {
                list.setOrder(order);
            }
            // add list items
            List<SgListItem> listItems = ListsTools.getListItems(context, listId);
            if (listItems != null) {
                list.setListItems(listItems);
            } else {
                Timber.d("uploadAll: no items to upload for list %s.", listId);
            }

            lists.add(list);

            if (lists.size() == LISTS_MAX_BATCH_SIZE || listsQuery.isLast()) {
                if (doUploadSomeLists(listsWrapper)) {
                    lists.clear();
                } else {
                    return false; // part upload failed, next sync will try again
                }
            }
        }
        listsQuery.close();

        return true;
    }

    private boolean doUploadSomeLists(SgListList listsWrapper) {
        Lists listsService = hexagonTools.getListsService();
        if (listsService == null) {
            return false; // no longer signed in
        }
        try {
            listsService.save(listsWrapper).execute();
        } catch (IOException e) {
            Errors.logAndReportHexagon("save lists", e);
            return false;
        }
        return true;
    }
}
