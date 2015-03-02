/*
 * Copyright 2015 Uwe Trottmann
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

package com.battlelancer.seriesguide.util;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AbsListView;
import com.melnykov.fab.FloatingActionButton;

/**
 * Hides the floating action button when scrolling down, shows it when scrolling up. If the end of
 * the list will be reached, shows the button also.
 *
 * <p>Built upon https://github.com/makovkastar/FloatingActionButton scroll detectors.
 */
public class FabAbsListViewScrollDetector implements AbsListView.OnScrollListener {

    private final FloatingActionButton button;
    private int lastScrollY;
    private int previousFirstVisibleItem;
    private int scrollThreshold;

    public FabAbsListViewScrollDetector(@NonNull FloatingActionButton button) {
        this.button = button;
        scrollThreshold = button.getContext().getResources().getDimensionPixelOffset(
                com.melnykov.fab.R.dimen.fab_scroll_threshold);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (totalItemCount == 0) {
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
