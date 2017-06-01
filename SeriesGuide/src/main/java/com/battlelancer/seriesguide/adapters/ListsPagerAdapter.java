
package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.ui.ListsFragment;
import com.battlelancer.seriesguide.util.ListsTools;

/**
 * Returns {@link ListsFragment}s for every list in the database, makes sure there is always at
 * least one.
 */
public class ListsPagerAdapter extends MultiPagerAdapter {

    private final Context context;
    private final ListsDataSetObserver dataSetObserver;
    @Nullable private Cursor cursorLists;
    private boolean dataValid;

    public ListsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
        this.dataSetObserver = new ListsDataSetObserver();

        cursorLists = queryLists();

        // precreate first list
        if (cursorLists != null && cursorLists.getCount() == 0) {
            String listName = this.context.getString(R.string.first_list);
            ListsTools.addList(this.context, listName);
        }
    }

    @Nullable
    private Cursor queryLists() {
        // load lists, order by order number, then name
        Cursor cursorNew = context.getContentResolver()
                .query(Lists.CONTENT_URI, ListsQuery.PROJECTION, null, null,
                        Lists.SORT_ORDER_THEN_NAME);
        boolean cursorPresent = cursorNew != null;
        dataValid = cursorPresent;
        if (cursorPresent) {
            cursorNew.registerDataSetObserver(dataSetObserver);
        }
        return cursorNew;
    }

    @Override
    public Fragment getItem(int position) {
        if (cursorLists == null || !dataValid) {
            return null;
        }
        cursorLists.moveToPosition(position);
        return ListsFragment.newInstance(cursorLists.getString(0));
    }

    @Override
    public int getCount() {
        if (cursorLists == null || !dataValid) {
            return 0;
        }
        return cursorLists.getCount();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (cursorLists == null || !dataValid) {
            return "";
        }
        cursorLists.moveToPosition(position);
        return cursorLists.getString(ListsQuery.NAME);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Nullable
    public String getListId(int position) {
        if (cursorLists == null || !dataValid) {
            return null;
        }
        cursorLists.moveToPosition(position);
        return cursorLists.getString(ListsQuery.ID);
    }

    public void onListsChanged() {
        Cursor oldCursor = cursorLists;
        if (oldCursor != null) {
            oldCursor.unregisterDataSetObserver(dataSetObserver);
            oldCursor.close();
        }

        cursorLists = queryLists();

        notifyDataSetChanged();
    }

    /**
     * Close the {@link Cursor} backing this {@link ListsPagerAdapter}.
     */
    public void onCleanUp() {
        if (cursorLists != null && !cursorLists.isClosed()) {
            cursorLists.unregisterDataSetObserver(dataSetObserver);
            cursorLists.close();
            cursorLists = null;
            dataValid = false;
        }
    }

    private class ListsDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            dataValid = true;
        }

        @Override
        public void onInvalidated() {
            dataValid = false;
        }
    }

    interface ListsQuery {
        String[] PROJECTION = new String[] {
                Lists.LIST_ID,
                Lists.NAME
        };

        int ID = 0;
        int NAME = 1;
    }
}
