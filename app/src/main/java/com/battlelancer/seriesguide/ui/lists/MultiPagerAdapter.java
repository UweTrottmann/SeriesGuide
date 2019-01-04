
package com.battlelancer.seriesguide.ui.lists;

import android.annotation.SuppressLint;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;

/**
 * Based on {@link FragmentPagerAdapter}, but removes fragments returning
 * {@link #POSITION_NONE} in addition to detaching them.
 */
public abstract class MultiPagerAdapter extends FragmentPagerAdapter {

    private FragmentManager fragmentManager;
    private FragmentTransaction currentTransaction;

    public MultiPagerAdapter(FragmentManager fm) {
        super(fm);
        fragmentManager = fm;
    }

    @SuppressLint("CommitTransaction")
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (getItemPosition(object) == POSITION_NONE) {
            if (currentTransaction == null) {
                // transaction is committed in #finishUpdate
                currentTransaction = fragmentManager.beginTransaction();
            }
            currentTransaction.remove((Fragment) object);
        } else {
            super.destroyItem(container, position, object);
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        super.finishUpdate(container);

        if (currentTransaction != null) {
            currentTransaction.commitAllowingStateLoss();
            currentTransaction = null;
            fragmentManager.executePendingTransactions();
        }
    }
}
