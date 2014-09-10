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

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Very primitive timestamp based search history backed by {@link android.content.SharedPreferences}.
 * It's only fast for a low number of history entries.
 */
public class SearchHistory {

    private static final String KEY_SEARCH_HISTORY_BASE = "recent-searches-";
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int ISO_8601_DATETIME_NOMILLIS_UTC_LENGTH = 20;
    private static final int DATETIME_PREFIX_LENGTH = ISO_8601_DATETIME_NOMILLIS_UTC_LENGTH + 1;
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER
            = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();

    private final Context context;
    private final String settingsKey;
    private final TreeMap<String, String> searchHistory;

    /**
     * Initialize search history by loading last saved history from {@link
     * android.content.SharedPreferences}.
     *
     * @param historyTag Appended to settings key where history is stored.
     */
    public SearchHistory(Context context, String historyTag) {
        this.context = context;
        this.settingsKey = KEY_SEARCH_HISTORY_BASE + historyTag;

        // load current history
        Set<String> storedSearchHistory = PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(settingsKey, null);
        if (storedSearchHistory == null) {
            this.searchHistory = new TreeMap<>();
        } else {
            TreeMap<String, String> map = new TreeMap<>();
            for (String historyEntry : storedSearchHistory) {
                String query = historyEntry.substring(DATETIME_PREFIX_LENGTH,
                        historyEntry.length());
                map.put(query, historyEntry);
            }
            this.searchHistory = map;
        }
    }

    public synchronized List<String> getSearchHistory() {
        return new ArrayList<>(searchHistory.keySet());
    }

    /**
     * Save a search query to history. If the query already exists, its timestamp is updated.
     *
     * @return {@code false} if the query was already in search history.
     */
    public synchronized boolean saveRecentSearch(String query) {
        boolean queryIsNew = !searchHistory.containsKey(query);

        // add/replace entry
        String now = new DateTime().toString(ISO_DATETIME_FORMATTER);
        searchHistory.put(query, now + " " + query);

        // trim to size
        if (searchHistory.size() > MAX_HISTORY_SIZE) {
            while (searchHistory.size() > MAX_HISTORY_SIZE) {
                // remove oldest entry (= lowest date value, sorted first)
                searchHistory.remove(searchHistory.firstKey());
            }
        }

        // save history
        Set<String> historySet = new HashSet<>(searchHistory.values());
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(settingsKey, historySet)
                .apply();

        return queryIsNew;
    }

    /**
     * Clear the search history from memory, starts a request to clear it from disk as well.
     */
    public synchronized void clearHistory() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(settingsKey).apply();
        searchHistory.clear();
    }
}
