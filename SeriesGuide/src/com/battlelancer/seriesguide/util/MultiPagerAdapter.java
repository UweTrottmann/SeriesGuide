
package com.battlelancer.seriesguide.util;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.view.ViewGroup;

/**
 * Based on {@link FragmentPagerAdapter}, but removes fragments returning
 * {@link #POSITION_NONE} in addition to detaching them.
 */
public abstract class MultiPagerAdapter extends FragmentPagerAdapter {
    private FragmentManager mFragmentManager;

    private FragmentTransaction mCurTransaction;

    public MultiPagerAdapter(FragmentManager fm) {
        super(fm);
        mFragmentManager = fm;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (getItemPosition(object) == POSITION_NONE) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            mCurTransaction.remove((Fragment) object);
        } else {
            super.destroyItem(container, position, object);
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        super.finishUpdate(container);

        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }
}
