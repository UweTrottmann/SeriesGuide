// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.ui;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.DimenRes;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AutoGridLayoutManager extends GridLayoutManager {

    public interface SpanCountListener {
        void onSetSpanCount(int spanCount);
    }

    private final int columnWidth;
    private final int itemSpanSize;
    private final int minSpanCount;
    @Nullable private final SpanCountListener spanCountListener;
    private boolean columnWidthChanged;

    /**
     * @param columnWidthRes Expected width of column to use to calculate span count.
     * @param itemSpanSize Span size of item to use to calculate span count.
     * @param minSpanCount Grid will have at least this many spans.
     */
    public AutoGridLayoutManager(Context context, @DimenRes int columnWidthRes, int itemSpanSize,
            int minSpanCount) {
        this(context, columnWidthRes, itemSpanSize, minSpanCount, null);
    }

    /**
     * @param columnWidthRes Expected width of column to use to calculate span count.
     * @param itemSpanSize Span size of item to use to calculate span count.
     * @param minSpanCount Grid will have at least this many spans.
     * @param spanCountListener Called once the span count has been determined.
     */
    public AutoGridLayoutManager(Context context, @DimenRes int columnWidthRes, int itemSpanSize,
            int minSpanCount, @Nullable SpanCountListener spanCountListener) {
        super(context, minSpanCount);
        this.spanCountListener = spanCountListener;

        Resources resources = context.getResources();
        int columnWidth = resources.getDimensionPixelSize(columnWidthRes);
        if (columnWidth < 1) {
            throw new IllegalArgumentException("Item width should be 1 or bigger.");
        }
        this.columnWidth = columnWidth;
        columnWidthChanged = true;

        if (itemSpanSize < 1) {
            throw new IllegalArgumentException("Max item span size should be 1 or bigger.");
        }
        this.itemSpanSize = itemSpanSize;

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
            columnWidthChanged = false; // only calculate span count once
            int totalSpace;
            if (getOrientation() == RecyclerView.VERTICAL) {
                totalSpace = width - getPaddingRight() - getPaddingLeft();
            } else {
                totalSpace = height - getPaddingTop() - getPaddingBottom();
            }
            int spanCount = Math.max(minSpanCount, (totalSpace / columnWidth) * itemSpanSize);
            setSpanCount(spanCount);
            if (spanCountListener != null) {
                spanCountListener.onSetSpanCount(spanCount);
            }
        }
        super.onLayoutChildren(recycler, state);
    }
}
