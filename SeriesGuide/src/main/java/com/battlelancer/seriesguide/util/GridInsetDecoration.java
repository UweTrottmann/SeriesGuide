package com.battlelancer.seriesguide.util;

import android.content.res.Resources;
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
    private final int topRowSpanSize;

    public GridInsetDecoration(Resources resources) {
        this(resources, 1);
    }

    public GridInsetDecoration(Resources resources, int topRowSpanSize) {
        insetHorizontal = resources.getDimensionPixelSize(R.dimen.grid_horizontal_spacing);
        insetVertical = resources.getDimensionPixelOffset(R.dimen.grid_vertical_spacing);
        this.topRowSpanSize = topRowSpanSize;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {
        GridLayoutManager.LayoutParams layoutParams
                = (GridLayoutManager.LayoutParams) view.getLayoutParams();

        int position = layoutParams.getViewLayoutPosition();
        if (position == RecyclerView.NO_POSITION) {
            outRect.set(0, 0, 0, 0);
            return;
        }

        // add edge margin only if item edge is not the grid edge
        int itemSpanIndex = layoutParams.getSpanIndex();
        // is item at left grid edge?
        outRect.left = itemSpanIndex == 0 ? 0 : insetHorizontal;
        // is item at top grid edge?
        outRect.top = itemSpanIndex / topRowSpanSize == position ? 0 : insetVertical;
        outRect.right = 0;
        outRect.bottom = 0;
    }
}
