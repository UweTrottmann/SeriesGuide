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
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * A {@link android.support.v4.widget.SwipeRefreshLayout} hosting a {@link android.view.ViewGroup}
 * with an empty view wrapped in a {@link android.widget.ScrollView} as its first and an {@link
 * android.widget.AdapterView} second child.
 */
public class EmptyViewSwipeRefreshLayout extends SwipeRefreshLayout {

    public EmptyViewSwipeRefreshLayout(Context context) {
        super(context);
    }

    public EmptyViewSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean canChildScrollUp() {
        // find content view
        ViewGroup target = null;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof ImageView) {
                continue;
            }
            target = (ViewGroup) child;
        }

        if (target == null) {
            return false;
        }

        // check if adapter view is visible
        View scrollableView = target.getChildAt(1);
        if (scrollableView.getVisibility() == GONE) {
            // use empty view layout instead
            scrollableView = target.getChildAt(0);
        }

        return ViewCompat.canScrollVertically(scrollableView, -1);
    }
}
