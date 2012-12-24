/*
 * Copyright 2012 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.seriesguide.R;

/**
 * Displays a menu to allow quick navigation within the app.
 */
public class SlidingMenuFragment extends ListFragment {

    public static final String TAG = "Menu";

    private MenuAdapter mAdapter;

    private SharedPreferences mPrefs;

    private static final int MENU_ITEM_SHOWS_ID = 0;
    private static final int MENU_ITEM_LISTS_ID = 1;
    private static final int MENU_ITEM_CHECKIN_ID = 2;
    private static final int MENU_ITEM_ACTIVITY_ID = 3;
    private static final int MENU_ITEM_SEARCH_ID = 4;
    private static final int MENU_ITEM_ADD_SHOWS_ID = 5;

    private static final int PAGE_SHOWS = 0;
    private static final int PAGE_LISTS = 1;
    private static final int PAGE_ACTIVITY = 2;

    /**
     * Google Analytics helper method for easy event tracking.
     */
    public void fireTrackerEvent(String label) {
        EasyTracker.getTracker().trackEvent(TAG, "Click", label, (long) 0);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // main views
        mAdapter = new MenuAdapter(getActivity());
        mAdapter.add(new MenuItem(getString(R.string.shows), R.drawable.ic_launcher,
                MENU_ITEM_SHOWS_ID));
        mAdapter.add(new MenuItem(getString(R.string.lists), R.drawable.ic_action_list,
                MENU_ITEM_LISTS_ID));
        mAdapter.add(new MenuItem(getString(R.string.activity), R.drawable.ic_action_upcoming,
                MENU_ITEM_ACTIVITY_ID));

        // actions
        mAdapter.add(new MenuCategory());
        mAdapter.add(new MenuItem(getString(R.string.checkin), R.drawable.ic_action_checkin,
                MENU_ITEM_CHECKIN_ID));
        mAdapter.add(new MenuItem(getString(R.string.search), R.drawable.ic_action_search,
                MENU_ITEM_SEARCH_ID));

        // add shows
        mAdapter.add(new MenuCategory());
        mAdapter.add(new MenuItem(getString(R.string.add_show), R.drawable.ic_action_add,
                MENU_ITEM_ADD_SHOWS_ID));

        setListAdapter(mAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // close menu any way
        if (getActivity() instanceof BaseActivity) {
            BaseActivity activity = (BaseActivity) getActivity();
            activity.showContent();
        }

        switch (((MenuItem) mAdapter.getItem(position)).mId) {
            case MENU_ITEM_SHOWS_ID:
                startActivity(new Intent(getActivity(), ShowsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                storeSelectedPage(PAGE_SHOWS);
                break;
            case MENU_ITEM_LISTS_ID:
                startActivity(new Intent(getActivity(), ListsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                storeSelectedPage(PAGE_LISTS);
                break;
            case MENU_ITEM_ACTIVITY_ID:
                startActivity(new Intent(getActivity(), UpcomingRecentActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                storeSelectedPage(PAGE_ACTIVITY);
                break;
            case MENU_ITEM_CHECKIN_ID:
                startActivity(new Intent(getActivity(), CheckinActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case MENU_ITEM_SEARCH_ID:
                startActivity(new Intent(getActivity(), SearchActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case MENU_ITEM_ADD_SHOWS_ID:
                startActivity(new Intent(getActivity(), AddActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
        }
    }

    private void storeSelectedPage(int page) {
        mPrefs.edit().putInt(SeriesGuidePreferences.KEY_SELECTED_PAGE, page).commit();
    }

    private class MenuItem {

        String mTitle;
        int mIconRes;
        int mId;

        public MenuItem(String title, int iconRes, int id) {
            mTitle = title;
            mIconRes = iconRes;
            mId = id;
        }
    }

    private class MenuCategory {

        public MenuCategory() {
        }
    }

    public class MenuAdapter extends ArrayAdapter<Object> {

        public MenuAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position) instanceof MenuItem ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position) instanceof MenuItem;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            Object item = getItem(position);

            if (item instanceof MenuItem) {
                ViewHolder holder;
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(
                            R.layout.sliding_menu_row_item, parent, false);
                    holder = new ViewHolder();
                    holder.attach(convertView);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                MenuItem menuItem = (MenuItem) item;
                holder.icon.setImageResource(menuItem.mIconRes);
                holder.title.setText(menuItem.mTitle);
            } else {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(
                            R.layout.sliding_menu_row_category, parent, false);
                }
            }

            return convertView;
        }
    }

    static class ViewHolder {

        public TextView title;

        public ImageView icon;

        public void attach(View v) {
            icon = (ImageView) v.findViewById(R.id.menu_icon);
            title = (TextView) v.findViewById(R.id.menu_title);
        }
    }

}
