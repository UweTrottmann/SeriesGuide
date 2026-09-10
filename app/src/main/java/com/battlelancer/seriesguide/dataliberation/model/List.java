// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2013 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.dataliberation.model;

public class List {

    public String list_id;
    public String name;
    public int order;

    /**
     * Optional Trakt list ID. Set if this list was synced with Trakt.
     */
    public Integer trakt_id;

    public java.util.List<ListItem> items;

}
