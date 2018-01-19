package com.battlelancer.seriesguide.ui.shows;

import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.AbsListView;
import com.battlelancer.seriesguide.R;

/**
 * Hides the floating action button when scrolling down, shows it when scrolling up. If the end of
 * the list will be reached, shows the button also.
 *
 * <p>Built upon https://github.com/makovkastar/FloatingActionButton scroll detectors.
 */
class FabAbsListViewScrollDetector implements AbsListView.OnScrollListener {

    private final FloatingActionButton button;
    private int lastScrollY;
    private int previousFirstVisibleItem;
    private int scrollThreshold;

    FabAbsListViewScrollDetector(@NonNull FloatingActionButton button) {
        this.button = button;
        scrollThreshold = button.getContext()
                .getResources()
                .getDimensionPixelOffset(R.dimen.fab_scroll_threshold);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (!view.hasFocus() || totalItemCount == 0) {
            return;
        }

        // always show if scrolled to bottom
        if (firstVisibleItem + visibleItemCount == totalItemCount) {
            button.show();
            return;
        }

        // still on the same row?
        if (firstVisibleItem == previousFirstVisibleItem) {
            int newScrollY = getTopItemScrollY(view);
            boolean isSignificantDelta = Math.abs(lastScrollY - newScrollY) > scrollThreshold;
            if (isSignificantDelta) {
                if (lastScrollY > newScrollY) {
                    button.hide();
                } else {
                    button.show();
                }
            }
            lastScrollY = newScrollY;
        } else {
            if (firstVisibleItem > previousFirstVisibleItem) {
                button.hide();
            } else {
                button.show();
            }

            lastScrollY = getTopItemScrollY(view);
            previousFirstVisibleItem = firstVisibleItem;
        }
    }

    private int getTopItemScrollY(AbsListView view) {
        if (view == null) {
            return 0;
        }
        View topChild = view.getChildAt(0);
        if (topChild == null) {
            return 0;
        }
        return topChild.getTop();
    }
}
