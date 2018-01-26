package com.battlelancer.seriesguide.ui.movies;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.DimenRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import com.battlelancer.seriesguide.R;

public class AutoGridLayoutManager extends GridLayoutManager {

    private final int columnWidth;
    private final int minItemSpanSize;
    private final int minSpanCount;
    private boolean columnWidthChanged;

    public AutoGridLayoutManager(Context context, @DimenRes int itemWidthRes, int minItemSpanSize,
            int minSpanCount) {
        super(context, minSpanCount);

        Resources resources = context.getResources();
        int itemWidth = resources.getDimensionPixelSize(itemWidthRes);
        if (itemWidth < 1) {
            throw new IllegalArgumentException("Item width should be 1 or bigger.");
        }
        int itemMargin = resources.getDimensionPixelSize(R.dimen.grid_item_margin_horizontal);
        this.columnWidth = itemWidth + 2 * itemMargin;
        columnWidthChanged = true;

        if (minItemSpanSize < 1) {
            throw new IllegalArgumentException("Max item span size should be 1 or bigger.");
        }
        this.minItemSpanSize = minItemSpanSize;

        if (minSpanCount < 1) {
            throw new IllegalArgumentException("Min span count should be 1 or bigger.");
        }
        this.minSpanCount = minSpanCount;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int width = getWidth();
        int height = getHeight();
        if (columnWidthChanged && width > 0 && height > 0) {
            columnWidthChanged = false;
            int totalSpace;
            if (getOrientation() == VERTICAL) {
                totalSpace = width - getPaddingRight() - getPaddingLeft();
            } else {
                totalSpace = height - getPaddingTop() - getPaddingBottom();
            }
            int spanCount = Math.max(minSpanCount, (totalSpace / columnWidth) * minItemSpanSize);
            setSpanCount(spanCount);
        }
        super.onLayoutChildren(recycler, state);
    }
}
