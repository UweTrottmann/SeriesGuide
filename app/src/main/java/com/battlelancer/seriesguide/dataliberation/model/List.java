// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2025 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation.model;

import android.content.ContentValues;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;

public class List {

    public String list_id;
    public String name;
    public int order;

    public java.util.List<ListItem> items;

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(Lists.LIST_ID, list_id);
        values.put(Lists.NAME, name);
        values.put(Lists.ORDER, order);
        return values;
    }
}
