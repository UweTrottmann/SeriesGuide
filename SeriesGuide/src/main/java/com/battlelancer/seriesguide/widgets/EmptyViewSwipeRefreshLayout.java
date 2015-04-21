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

package com.battlelancer.seriesguide.widgets;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.View;

/**
 * A {@link android.support.v4.widget.SwipeRefreshLayout} which only checks the (scrollable!) views
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
                if (view.isShown() && ViewCompat.canScrollVertically(view, -1)) {
                    // prevent refresh gesture
                    return true;
                }
            }
        }
        return false;
    }
}
