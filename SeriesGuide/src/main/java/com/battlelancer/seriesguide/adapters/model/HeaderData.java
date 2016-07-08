package com.battlelancer.seriesguide.adapters.model;

/**
 * Stores information about a header, used by
 * {@link com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter}
 * adapters.
 */
public class HeaderData {

    private int mCount;

    private int mRefPosition;

    public HeaderData(int refPosition) {
        mRefPosition = refPosition;
        mCount = 0;
    }

    public int getCount() {
        return mCount;
    }

    public int getRefPosition() {
        return mRefPosition;
    }

    public void incrementCount() {
        mCount++;
    }
}