package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

public class TvEntity implements TraktEntity {
    private static final long serialVersionUID = 4535846809492296227L;

    public TvShow show;
    public TvShowEpisode episode;
}
