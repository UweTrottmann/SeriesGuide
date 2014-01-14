
/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.battlelancer.seriesguide.provider.SeriesContract.Lists;
import com.battlelancer.seriesguide.ui.ListsFragment;
import com.uwetrottmann.seriesguide.R;

/**
 * Returns {@link ListsFragment}s for every list in the database, makes sure
 * there is always at least one.
 */
public class ListsPagerAdapter extends MultiPagerAdapter {

    private Context mContext;

    private Cursor mLists;

    public ListsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;

        mLists = mContext.getContentResolver().query(Lists.CONTENT_URI, new String[] {
                Lists.LIST_ID, Lists.NAME
        }, null, null, null);

        // precreate first list
        if (mLists != null && mLists.getCount() == 0) {
            String listName = mContext.getString(R.string.first_list);

            ContentValues values = new ContentValues();
            values.put(Lists.LIST_ID, Lists.generateListId(listName));
            values.put(Lists.NAME, listName);
            mContext.getContentResolver().insert(Lists.CONTENT_URI, values);

            onListsChanged();
        }
    }

    @Override
    public Fragment getItem(int position) {
        if (mLists == null) {
            return null;
        }

        mLists.moveToPosition(position);
        return ListsFragment.newInstance(mLists.getString(0));
    }

    @Override
    public int getCount() {
        if (mLists == null) {
            return 1;
        }
        return mLists.getCount();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (mLists == null) {
            return "";
        }

        mLists.moveToPosition(position);
        return mLists.getString(1);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public String getListId(int position) {
        if (mLists == null) {
            return null;
        }

        mLists.moveToPosition(position);
        return mLists.getString(0);
    }

    public void onListsChanged() {
        if (mLists != null && !mLists.isClosed()) {
            Cursor newCursor = mContext.getContentResolver().query(Lists.CONTENT_URI,
                    new String[] {
                            Lists.LIST_ID, Lists.NAME
                    }, null, null, null);

            Cursor oldCursor = mLists;
            mLists = newCursor;
            oldCursor.close();

            notifyDataSetChanged();
        }
    }

    /**
     * Close the {@link Cursor} backing this {@link ListsPagerAdapter}.
     */
    public void onCleanUp() {
        if (mLists != null && !mLists.isClosed()) {
            mLists.close();
            mLists = null;
        }
    }
}
