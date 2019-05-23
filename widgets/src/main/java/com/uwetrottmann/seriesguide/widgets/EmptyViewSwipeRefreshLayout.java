package com.uwetrottmann.seriesguide.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * A {@link SwipeRefreshLayout} which only checks the (scrollable!) views
 * set through {@link #setSwipeableChildren(int...)} if they can scroll up to determine whether to
 * trigger the refresh gesture.
 */
public class EmptyViewSwipeRefreshLayout extends SwipeRefreshLayout {

    private View[] swipeableChildren;

    public EmptyViewSwipeRefreshLayout(Context context) {
        super(context);
    }

    public EmptyViewSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Set the children to be checked if they can scroll up and hence should prevent the refresh
     * gesture from being triggered.
     */
    public void setSwipeableChildren(final int... ids) {
        if (ids == null) {
            return;
        }

        // find child views
        swipeableChildren = new View[ids.length];
        for (int i = 0; i < ids.length; i++) {
            View view = findViewById(ids[i]);
            if (view == null) {
                throw new IllegalArgumentException(
                        "Supplied view ids need to exist in this layout.");
            }
            swipeableChildren[i] = view;
        }
    }

    @Override
    public boolean canChildScrollUp() {
        if (swipeableChildren != null) {
            // check if any supplied swipeable children can scroll up
            for (View view : swipeableChildren) {
                if (view.isShown() && view.canScrollVertically(-1)) {
                    // prevent refresh gesture
                    return true;
                }
            }
        }
        return false;
    }
}
