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

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.battlelancer.seriesguide.R;

/**
 * Adds 8dp spacing in between cards in a grid. Meaning, there will be no margin added at the outer
 * edges of the grid.
 */
public class GridInsetDecoration extends RecyclerView.ItemDecoration {

    private int insetHorizontal;
    private int insetVertical;

    public GridInsetDecoration(Context context) {
        insetHorizontal = context.getResources()
                .getDimensionPixelSize(R.dimen.grid_horizontal_spacing);
        insetVertical = context.getResources()
                .getDimensionPixelOffset(R.dimen.grid_vertical_spacing);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {
        GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
        GridLayoutManager.SpanSizeLookup spanSizeLookup = layoutManager.getSpanSizeLookup();

        int spanCount = layoutManager.getSpanCount();
        int position = parent.getChildPosition(view);
        int itemSpanIndex = spanSizeLookup.getSpanIndex(position, spanCount);
        int itemSpanGroupIndex = spanSizeLookup.getSpanGroupIndex(position, spanCount);

        // add edge margin only if item edge is not the grid edge
        // is left grid edge?
        outRect.left = itemSpanIndex == 0 ? 0 : insetHorizontal;
        // is top edge?
        outRect.top = itemSpanGroupIndex == 0 ? 0 : insetVertical;
        outRect.right = 0;
        outRect.bottom = 0;
    }
}
