/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.battlelancer.seriesguide.ui.shows;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.WrapperListAdapter;
import java.util.ArrayList;

/**
 * A {@link GridView} that supports adding header rows. See {@link HeaderGridView#addHeaderView(View,
 * Object, boolean)}.
 *
 * A copy from https://android.googlesource.com/platform/packages/apps/Gallery2/+/master/src/com/android/photos/views/HeaderGridView.java
 * adapted for this app.
 */
public class HeaderGridView extends GridView {

    /**
     * A class that represents a fixed view in a list, for example a header at the top or a footer
     * at the bottom.
     */
    private static class FixedViewInfo {
        /** The view to add to the grid */
        public View view;
        public ViewGroup viewContainer;
        /** The data backing the view. This is returned from {@link ListAdapter#getItem(int)}. */
        public Object data;
        /** <code>true</code> if the fixed view should be selectable in the grid */
        public boolean isSelectable;
    }

    private ArrayList<FixedViewInfo> headerViewInfos = new ArrayList<>();

    private void initHeaderGridView() {
        super.setClipChildren(false);
    }

    public HeaderGridView(Context context) {
        super(context);
        initHeaderGridView();
    }

    public HeaderGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initHeaderGridView();
    }

    public HeaderGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initHeaderGridView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ListAdapter adapter = getAdapter();
        if (adapter != null && adapter instanceof HeaderViewGridAdapter) {
            ((HeaderViewGridAdapter) adapter).setNumColumns(getNumColumns());
        }
    }

    @Override
    public void setClipChildren(boolean clipChildren) {
        // Ignore, since the header rows depend on not being clipped
    }

    /**
     * Add a fixed view to appear at the top of the grid. If addHeaderView is called more than once,
     * the views will appear in the order they were added. Views added using this call can take
     * focus if they want. <p> NOTE: Call this before calling setAdapter. This is so HeaderGridView
     * can wrap the supplied cursor with one that will also account for header views.
     *
     * @param v The view to add.
     * @param data Data to associate with this view
     * @param isSelectable whether the item is selectable
     */
    public void addHeaderView(View v, Object data, boolean isSelectable) {
        ListAdapter adapter = getAdapter();
        if (adapter != null && !(adapter instanceof HeaderViewGridAdapter)) {
            throw new IllegalStateException(
                    "Cannot add header view to grid -- setAdapter has already been called.");
        }
        FixedViewInfo info = new FixedViewInfo();
        FrameLayout fl = new FullWidthFixedViewLayout(getContext());
        fl.addView(v);
        info.view = v;
        info.viewContainer = fl;
        info.data = data;
        info.isSelectable = isSelectable;
        headerViewInfos.add(info);
        // in the case of re-adding a header view, or adding one later on,
        // we need to notify the observer
        if (adapter != null) {
            ((HeaderViewGridAdapter) adapter).notifyDataSetChanged();
        }
    }

    /**
     * Add a fixed view to appear at the top of the grid. If addHeaderView is called more than once,
     * the views will appear in the order they were added. Views added using this call can take
     * focus if they want. <p> NOTE: Call this before calling setAdapter. This is so HeaderGridView
     * can wrap the supplied cursor with one that will also account for header views.
     *
     * @param v The view to add.
     */
    public void addHeaderView(View v) {
        addHeaderView(v, null, true);
    }

    public int getHeaderViewCount() {
        return headerViewInfos.size();
    }

    /**
     * Removes a previously-added header view.
     *
     * @param v The view to remove
     * @return true if the view was removed, false if the view was not a header view
     */
    public boolean removeHeaderView(View v) {
        if (headerViewInfos.size() > 0) {
            boolean result = false;
            ListAdapter adapter = getAdapter();
            if (adapter != null && ((HeaderViewGridAdapter) adapter).removeHeader(v)) {
                result = true;
            }
            removeFixedViewInfo(v, headerViewInfos);
            return result;
        }
        return false;
    }

    private void removeFixedViewInfo(View v, ArrayList<FixedViewInfo> where) {
        int len = where.size();
        for (int i = 0; i < len; ++i) {
            FixedViewInfo info = where.get(i);
            if (info.view == v) {
                where.remove(i);
                break;
            }
        }
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (headerViewInfos.size() > 0) {
            HeaderViewGridAdapter hadapter = new HeaderViewGridAdapter(headerViewInfos, adapter);
            int numColumns = getNumColumns();
            if (numColumns > 1) {
                hadapter.setNumColumns(numColumns);
            }
            super.setAdapter(hadapter);
        } else {
            super.setAdapter(adapter);
        }
    }

    private class FullWidthFixedViewLayout extends FrameLayout {
        public FullWidthFixedViewLayout(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int targetWidth = HeaderGridView.this.getMeasuredWidth()
                    - HeaderGridView.this.getPaddingLeft()
                    - HeaderGridView.this.getPaddingRight();
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(targetWidth,
                    MeasureSpec.getMode(widthMeasureSpec));
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * ListAdapter used when a HeaderGridView has header views. This ListAdapter wraps another one
     * and also keeps track of the header views and their associated data objects. <p>This is
     * intended as a base class; you will probably not need to use this class directly in your own
     * code.
     */
    private static class HeaderViewGridAdapter implements WrapperListAdapter, Filterable {
        // This is used to notify the container of updates relating to number of columns
        // or headers changing, which changes the number of placeholders needed
        private final DataSetObservable dataSetObservable = new DataSetObservable();
        private final ListAdapter adapter;
        private int numColumns = 1;
        // This ArrayList is assumed to NOT be null.
        ArrayList<FixedViewInfo> headerViewInfos;
        boolean areAllFixedViewsSelectable;
        private final boolean isFilterable;

        public HeaderViewGridAdapter(ArrayList<FixedViewInfo> headerViewInfos,
                ListAdapter adapter) {
            this.adapter = adapter;
            isFilterable = adapter instanceof Filterable;
            if (headerViewInfos == null) {
                throw new IllegalArgumentException("headerViewInfos cannot be null");
            }
            this.headerViewInfos = headerViewInfos;
            areAllFixedViewsSelectable = areAllListInfosSelectable(this.headerViewInfos);
        }

        public int getHeadersCount() {
            return headerViewInfos.size();
        }

        @Override
        public boolean isEmpty() {
            return (adapter == null || adapter.isEmpty()) && getHeadersCount() == 0;
        }

        public void setNumColumns(int numColumns) {
            if (numColumns < 1) {
                throw new IllegalArgumentException("Number of columns must be 1 or more");
            }
            if (this.numColumns != numColumns) {
                this.numColumns = numColumns;
                notifyDataSetChanged();
            }
        }

        private boolean areAllListInfosSelectable(ArrayList<FixedViewInfo> infos) {
            if (infos != null) {
                for (FixedViewInfo info : infos) {
                    if (!info.isSelectable) {
                        return false;
                    }
                }
            }
            return true;
        }

        public boolean removeHeader(View v) {
            for (int i = 0; i < headerViewInfos.size(); i++) {
                FixedViewInfo info = headerViewInfos.get(i);
                if (info.view == v) {
                    headerViewInfos.remove(i);
                    areAllFixedViewsSelectable = areAllListInfosSelectable(headerViewInfos);
                    dataSetObservable.notifyChanged();
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getCount() {
            if (adapter != null) {
                return getHeadersCount() * numColumns + adapter.getCount();
            } else {
                return getHeadersCount() * numColumns;
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            if (adapter != null) {
                return areAllFixedViewsSelectable && adapter.areAllItemsEnabled();
            } else {
                return true;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            int numHeadersAndPlaceholders = getHeadersCount() * numColumns;
            if (position < numHeadersAndPlaceholders) {
                return (position % numColumns == 0)
                        && headerViewInfos.get(position / numColumns).isSelectable;
            }
            // Adapter
            final int adjPosition = position - numHeadersAndPlaceholders;
            int adapterCount;
            if (adapter != null) {
                adapterCount = adapter.getCount();
                if (adjPosition < adapterCount) {
                    return adapter.isEnabled(adjPosition);
                }
            }
            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public Object getItem(int position) {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            int numHeadersAndPlaceholders = getHeadersCount() * numColumns;
            if (position < numHeadersAndPlaceholders) {
                if (position % numColumns == 0) {
                    return headerViewInfos.get(position / numColumns).data;
                }
                return null;
            }
            // Adapter
            final int adjPosition = position - numHeadersAndPlaceholders;
            int adapterCount;
            if (adapter != null) {
                adapterCount = adapter.getCount();
                if (adjPosition < adapterCount) {
                    return adapter.getItem(adjPosition);
                }
            }
            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public long getItemId(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * numColumns;
            if (adapter != null && position >= numHeadersAndPlaceholders) {
                int adjPosition = position - numHeadersAndPlaceholders;
                int adapterCount = adapter.getCount();
                if (adjPosition < adapterCount) {
                    return adapter.getItemId(adjPosition);
                }
            }
            return -1;
        }

        @Override
        public boolean hasStableIds() {
            return adapter != null && adapter.hasStableIds();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            int numHeadersAndPlaceholders = getHeadersCount() * numColumns;
            if (position < numHeadersAndPlaceholders) {
                View headerViewContainer = headerViewInfos
                        .get(position / numColumns).viewContainer;
                if (position % numColumns == 0) {
                    return headerViewContainer;
                } else {
                    if (convertView == null) {
                        convertView = new View(parent.getContext());
                    }
                    // We need to do this because GridView uses the height of the last item
                    // in a row to determine the height for the entire row.
                    convertView.setVisibility(View.INVISIBLE);
                    convertView.setMinimumHeight(headerViewContainer.getHeight());
                    return convertView;
                }
            }
            // Adapter
            final int adjPosition = position - numHeadersAndPlaceholders;
            int adapterCount;
            if (adapter != null) {
                adapterCount = adapter.getCount();
                if (adjPosition < adapterCount) {
                    return adapter.getView(adjPosition, convertView, parent);
                }
            }
            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public int getItemViewType(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * numColumns;
            if (position < numHeadersAndPlaceholders && (position % numColumns != 0)) {
                // Placeholders get the last view type number
                return adapter != null ? adapter.getViewTypeCount() : 1;
            }
            if (adapter != null && position >= numHeadersAndPlaceholders) {
                int adjPosition = position - numHeadersAndPlaceholders;
                int adapterCount = adapter.getCount();
                if (adjPosition < adapterCount) {
                    return adapter.getItemViewType(adjPosition);
                }
            }
            return AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
        }

        @Override
        public int getViewTypeCount() {
            if (adapter != null) {
                return adapter.getViewTypeCount() + 1;
            }
            return 2;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            dataSetObservable.registerObserver(observer);
            if (adapter != null) {
                adapter.registerDataSetObserver(observer);
            }
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            dataSetObservable.unregisterObserver(observer);
            if (adapter != null) {
                adapter.unregisterDataSetObserver(observer);
            }
        }

        @Override
        public Filter getFilter() {
            if (isFilterable) {
                return ((Filterable) adapter).getFilter();
            }
            return null;
        }

        @Override
        public ListAdapter getWrappedAdapter() {
            return adapter;
        }

        public void notifyDataSetChanged() {
            dataSetObservable.notifyChanged();
        }
    }
}