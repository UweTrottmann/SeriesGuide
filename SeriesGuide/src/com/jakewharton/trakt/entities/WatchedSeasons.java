package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

import java.util.List;

public class WatchedSeasons implements TraktEntity {
    private static final long serialVersionUID = -8933663277406468172L;
    
    private Integer season;
    private List<Integer> episodes;
    
    public Integer getSeason() {
        return this.season;
    }
    public List<Integer> getEpisodes() {
        return this.episodes;
    }
}
