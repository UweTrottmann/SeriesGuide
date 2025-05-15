// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2025 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation.model;

public class ListItem {

    public String list_item_id;

    /**
     * Used in legacy backup files.
     */
    public int tvdb_id;

    /**
     * TMDB ID for new list items, TVDB ID for legacy list items.
     */
    public String externalId;

    public String type;

}
