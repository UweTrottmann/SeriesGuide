
package com.battlelancer.seriesguide.enums;

import com.battlelancer.seriesguide.R;

import java.util.HashMap;
import java.util.Map;

public enum WidgetListType {
    UPCOMING(0, R.id.radioUpcoming), RECENT(1, R.id.radioRecent);

    public int index;

    public int id;

    private WidgetListType(int index, int id) {
        this.index = index;
        this.id = id;
    }

    private static final Map<Integer, WidgetListType> MAPPING = new HashMap<Integer, WidgetListType>();

    static {
        for (WidgetListType via : WidgetListType.values()) {
            MAPPING.put(via.id, via);
        }
    }

    public static WidgetListType fromId(int id) {
        return MAPPING.get(id);
    }
}
