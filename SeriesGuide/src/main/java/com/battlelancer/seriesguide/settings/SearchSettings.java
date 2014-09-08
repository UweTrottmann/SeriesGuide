/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class SearchSettings {

    private static final String KEY_RECENT_SEARCHES_TVDB = "recent-searches-tvdb";
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int ISO_8601_DATETIME_NOMILLIS_UTC_LENGTH = 20;
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER
            = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();

    public static List<String> getSearchHistoryTheTvdb(Context context) {
        Set<String> searchHistory = PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(KEY_RECENT_SEARCHES_TVDB, null);
        if (searchHistory != null) {
            // strip datetime from history entries
            List<String> searchQueries = new ArrayList<>();
            for (String historyEntry : searchHistory) {
                searchQueries.add(historyEntry.substring(ISO_8601_DATETIME_NOMILLIS_UTC_LENGTH,
                        historyEntry.length()));
            }
            return searchQueries;
        } else {
            return new ArrayList<>();
        }
    }

    public static void clearSearchHistoryTheTvdb(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .remove(KEY_RECENT_SEARCHES_TVDB)
                .apply();
    }

    public static List<String> addRecentSearchTheTvdb(Context context, String query) {
        Set<String> storedSearchHistory = PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(KEY_RECENT_SEARCHES_TVDB, null);

        Set<String> searchHistory;
        if (storedSearchHistory == null) {
            searchHistory = new HashSet<>();
        } else {
            // do not modify original
            searchHistory = new HashSet<>(storedSearchHistory);
        }

        // add entry
        String now = new DateTime().toString(ISO_DATETIME_FORMATTER);
        searchHistory.add(now + " " + query);

        // trim to size
        if (searchHistory.size() > MAX_HISTORY_SIZE) {
            TreeSet<String> sortedSearchHistory = new TreeSet<>(searchHistory);
            while (sortedSearchHistory.size() > MAX_HISTORY_SIZE) {
                // remove oldest entry (= lowest date value)
                sortedSearchHistory.remove(sortedSearchHistory.first());
            }
            searchHistory = sortedSearchHistory;
        }

        // strip datetime from history entries
        List<String> searchQueries = new ArrayList<>();
        for (String historyEntry : searchHistory) {
            searchQueries.add(historyEntry.substring(ISO_8601_DATETIME_NOMILLIS_UTC_LENGTH,
                    historyEntry.length()));
        }

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(KEY_RECENT_SEARCHES_TVDB, searchHistory)
                .apply();

        return searchQueries;
    }
}
