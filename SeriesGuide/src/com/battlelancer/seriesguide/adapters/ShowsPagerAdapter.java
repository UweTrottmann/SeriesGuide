
package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.battlelancer.seriesguide.ui.FirstRunFragment;
import com.battlelancer.seriesguide.ui.ShowsFragment;

public class ShowsPagerAdapter extends MultiPagerAdapter {

    private Context mContext;

    public ShowsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        if (!FirstRunFragment.hasSeenFirstRunFragment(mContext)) {
            return FirstRunFragment.newInstance();
        } else {
            return ShowsFragment.newInstance();
        }
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "";
    }

    @Override
    public int getItemPosition(Object object) {
        if (object instanceof ShowsFragment) {
            return POSITION_UNCHANGED;
        } else {
            return POSITION_NONE;
        }
    }
}
