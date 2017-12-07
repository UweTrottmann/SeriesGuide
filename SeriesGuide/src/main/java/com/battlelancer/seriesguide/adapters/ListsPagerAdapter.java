
package com.battlelancer.seriesguide.adapters;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.ui.ListsFragment;

/**
 * Returns {@link ListsFragment}s for every list in the database, makes sure there is always at
 * least one.
 */
public class ListsPagerAdapter extends MultiPagerAdapter {

    private final ListsDataSetObserver dataSetObserver;
    @Nullable private Cursor cursorLists;
    private boolean dataValid;

    public ListsPagerAdapter(FragmentManager fm) {
        super(fm);
        this.dataSetObserver = new ListsDataSetObserver();
    }

    public void swapCursor(@Nullable Cursor cursorNew) {
        if (cursorLists == cursorNew) {
            return;
        }
        Cursor oldCursor = cursorLists;
        if (oldCursor != null) {
            oldCursor.unregisterDataSetObserver(dataSetObserver);
        }
        cursorLists = cursorNew;

        boolean cursorPresent = cursorNew != null;
        dataValid = cursorPresent;
        if (cursorPresent) {
            cursorNew.registerDataSetObserver(dataSetObserver);
            notifyDataSetChanged();
        }
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

    private class ListsDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            dataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            dataValid = false;
        }
    }

    public interface ListsQuery {
        String[] PROJECTION = new String[] {
                Lists.LIST_ID,
                Lists.NAME
        };

        int ID = 0;
        int NAME = 1;
    }
}
