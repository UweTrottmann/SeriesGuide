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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.uwetrottmann.seriesguide.R;

/**
 * Displays a menu to allow quick navigation within the app.
 */
public class SlidingMenuFragment extends ListFragment {

    private MenuAdapter mAdapter;

    private static final int MENU_ITEM_SHOWS_ID = 0;
    private static final int MENU_ITEM_LISTS_ID = 1;
    private static final int MENU_ITEM_CHECKIN_ID = 2;
    private static final int MENU_ITEM_ACTIVITY_ID = 3;
    private static final int MENU_ITEM_SEARCH_ID = 4;
    private static final int MENU_ITEM_ADD_SHOWS_ID = 5;
    private static final int MENU_ITEM_HELP_ID = 6;
    private static final int MENU_ITEM_SETTINGS_ID = 7;

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new MenuAdapter(getActivity());
        mAdapter.add(new MenuItem(getString(R.string.shows), R.drawable.ic_launcher,
                MENU_ITEM_SHOWS_ID, false));
        mAdapter.add(new MenuItem(getString(R.string.lists), R.drawable.ic_action_list,
                MENU_ITEM_LISTS_ID, false));
        mAdapter.add(new MenuItem(getString(R.string.activity), R.drawable.ic_action_upcoming,
                MENU_ITEM_ACTIVITY_ID, true));
        mAdapter.add(new MenuItem(getString(R.string.checkin), R.drawable.ic_action_checkin,
                MENU_ITEM_CHECKIN_ID, false));
        mAdapter.add(new MenuItem(getString(R.string.search), R.drawable.ic_action_search,
                MENU_ITEM_SEARCH_ID, true));
        mAdapter.add(new MenuItem(getString(R.string.add_show), R.drawable.ic_action_add,
                MENU_ITEM_ADD_SHOWS_ID, true));
        mAdapter.add(new MenuItem(getString(R.string.preferences), R.drawable.ic_action_settings,
                MENU_ITEM_SETTINGS_ID, false));
        mAdapter.add(new MenuItem(getString(R.string.help), R.drawable.ic_action_help,
                MENU_ITEM_HELP_ID, false));

        setListAdapter(mAdapter);
        
        getListView().setDivider(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // close menu any way
        if (getActivity() instanceof BaseActivity) {
            BaseActivity activity = (BaseActivity) getActivity();
            activity.showContent();
        }

        switch (mAdapter.getItem(position).id) {
            case MENU_ITEM_SHOWS_ID:
                if (getActivity() instanceof ShowsActivity) {
                    break;
                }
                NavUtils.navigateUpTo(getActivity(),
                        new Intent(Intent.ACTION_MAIN).setClass(getActivity(),
                                ShowsActivity.class));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case MENU_ITEM_LISTS_ID:
                startActivity(new Intent(getActivity(), ListsActivity.class));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case MENU_ITEM_ACTIVITY_ID:
                startActivity(new Intent(getActivity(), UpcomingRecentActivity.class));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case MENU_ITEM_CHECKIN_ID:
                startActivity(new Intent(getActivity(), CheckinActivity.class));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case MENU_ITEM_SEARCH_ID:
                startActivity(new Intent(getActivity(), SearchActivity.class));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case MENU_ITEM_ADD_SHOWS_ID:
                startActivity(new Intent(getActivity(), AddActivity.class));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case MENU_ITEM_SETTINGS_ID:
                startActivity(new Intent(getActivity(), SeriesGuidePreferences.class));
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case MENU_ITEM_HELP_ID:
                Intent myIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(SeriesGuidePreferences.HELP_URL));
                startActivity(myIntent);
                break;
        }
    }

    private class MenuItem {
        public String tag;
        public int iconRes;
        public int id;
        public boolean hasSpacer;

        public MenuItem(String tag, int iconRes, int id, boolean hasSpacer) {
            this.tag = tag;
            this.iconRes = iconRes;
            this.id = id;
            this.hasSpacer = hasSpacer;
        }
    }

    public class MenuAdapter extends ArrayAdapter<MenuItem> {

        public MenuAdapter(Context context) {
            super(context, 0);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.sliding_menu_row,
                        null);

                holder = new ViewHolder();
                holder.attach(convertView);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            MenuItem menuItem = getItem(position);
            holder.icon.setImageResource(menuItem.iconRes);
            holder.title.setText(menuItem.tag);

            holder.divider.setVisibility(menuItem.hasSpacer ? View.GONE : View.VISIBLE);
            holder.spacer.setVisibility(menuItem.hasSpacer ? View.VISIBLE : View.GONE);

            return convertView;
        }
    }

    static class ViewHolder {

        public TextView title;

        public ImageView icon;

        public View divider;

        public View spacer;

        public void attach(View v) {
            icon = (ImageView) v.findViewById(R.id.menu_icon);
            title = (TextView) v.findViewById(R.id.menu_title);
            divider = v.findViewById(R.id.menu_divider);
            spacer = v.findViewById(R.id.menu_spacer);
        }
    }

}
