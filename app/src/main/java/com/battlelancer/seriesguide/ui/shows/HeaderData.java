package com.battlelancer.seriesguide.ui.shows;

/**
 * Stores information about a header, used by
 * {@link com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter}
 * adapters.
 */
public class HeaderData {

    private int count;

    private int refPosition;

    public HeaderData(int refPosition) {
        this.refPosition = refPosition;
        count = 0;
    }

    public int getCount() {
        return count;
    }

    public int getRefPosition() {
        return refPosition;
    }

    public void incrementCount() {
        count++;
    }
}