package com.uwetrottmann.seriesguide;

import java.util.LinkedList;
import java.util.List;

public class ShowList {

    private List<Show> shows = new LinkedList<Show>();

    public List<Show> getShows() {
        return shows;
    }

    public void setShows(List<Show> shows) {
        this.shows = shows;
    }

}
