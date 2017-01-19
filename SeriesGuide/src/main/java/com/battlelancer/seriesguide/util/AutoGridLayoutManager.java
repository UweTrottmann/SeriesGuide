package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.support.annotation.DimenRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

public class AutoGridLayoutManager extends GridLayoutManager {

    private final int columnWidth;
    private final int minItemSpanSize;
    private final int minSpanCount;
    private boolean columnWidthChanged;

    public AutoGridLayoutManager(Context context, @DimenRes int columnWidthRes, int minItemSpanSize,
            int minSpanCount) {
        super(context, minSpanCount);

        int columnWidth = context.getResources().getDimensionPixelSize(columnWidthRes);
        if (columnWidth < 1) {
            throw new IllegalArgumentException("Column width should be 1 or bigger.");
        }
        this.columnWidth = columnWidth;
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
