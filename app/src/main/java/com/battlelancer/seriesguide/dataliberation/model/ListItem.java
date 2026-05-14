// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright 2013-2025 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation.model;

import androidx.annotation.Nullable;

public class ListItem {

    public String list_item_id;

    /**
     * Null in new backups. Used in legacy backup files.
     */
    @Nullable
    public Integer tvdb_id;

    /**
     * TMDB ID for new list items, TVDB ID for legacy list items.
     */
    public String externalId;

    public String type;

}
