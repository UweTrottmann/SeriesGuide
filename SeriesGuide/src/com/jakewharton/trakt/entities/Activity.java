package com.jakewharton.trakt.entities;

import com.jakewharton.trakt.TraktEntity;

import java.util.Date;
import java.util.List;

public class Activity implements TraktEntity {
    private static final long serialVersionUID = -3180174955865068567L;

    public static class Timestamps implements TraktEntity {
        private static final long serialVersionUID = 7812411503074767278L;

        public Date start;
        public Date current;
    }

    public Timestamps timestamps;
    public List<ActivityItem> activity;
}
