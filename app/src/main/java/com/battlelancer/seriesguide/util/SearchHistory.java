package com.battlelancer.seriesguide.util;

import android.content.Context;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.threeten.bp.Instant;
import org.threeten.bp.temporal.ChronoField;

/**
 * Very primitive timestamp based search history backed by {@link android.content.SharedPreferences}.
 * It's only fast for a low number of history entries.
 */
public class SearchHistory {

    private static final String KEY_SEARCH_HISTORY_BASE = "recent-searches-";
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int ISO_8601_DATETIME_NOMILLIS_UTC_LENGTH = 20;
    private static final int DATETIME_PREFIX_LENGTH = ISO_8601_DATETIME_NOMILLIS_UTC_LENGTH + 1;
    private static final Comparator<String> NATURAL_ORDER_REVERSE =
            (lhs, rhs) -> -lhs.compareTo(rhs);

    private final Context context;
    private final String settingsKey;
    // timestamp+query -> query
    private final TreeMap<String, String> searchHistory;
    // query -> timestamp+query
    private final HashMap<String, String> queryMap;

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
            this.searchHistory = new TreeMap<>(NATURAL_ORDER_REVERSE);
            this.queryMap = new HashMap<>();
        } else {
            TreeMap<String, String> map = new TreeMap<>(NATURAL_ORDER_REVERSE);
            HashMap<String, String> queryMap = new HashMap<>();
            for (String historyEntry : storedSearchHistory) {
                String query = historyEntry.substring(DATETIME_PREFIX_LENGTH);
                map.put(historyEntry, query);
                queryMap.put(query, historyEntry);
            }
            this.searchHistory = map;
            this.queryMap = queryMap;
        }
    }

    public synchronized List<String> getSearchHistory() {
        return new ArrayList<>(searchHistory.values());
    }

    /**
     * Save a search query to history. If the query already exists, its timestamp is updated.
     *
     * @return {@code false} if the query was already in search history.
     */
    public synchronized boolean saveRecentSearch(String query) {
        // prevent duplicate entries
        String historyEntry = queryMap.get(query);
        if (historyEntry != null) {
            searchHistory.remove(historyEntry);
        }

        // add new entry
        String now = Instant.now().with(ChronoField.NANO_OF_SECOND, 0).toString();
        String newHistoryEntry = now + " " + query;
        searchHistory.put(now + " " + query, query);
        queryMap.put(query, newHistoryEntry);

        // trim to size
        if (searchHistory.size() > MAX_HISTORY_SIZE) {
            while (searchHistory.size() > MAX_HISTORY_SIZE) {
                // remove oldest entry (= lowest date value, sorted first)
                String removedQuery = searchHistory.remove(searchHistory.lastKey());
                queryMap.remove(removedQuery);
            }
        }

        // save history
        Set<String> historySet = new HashSet<>(searchHistory.keySet());
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(settingsKey, historySet)
                .apply();

        return historyEntry == null;
    }

    /**
     * Clear the search history from memory, starts a request to clear it from disk as well.
     */
    public synchronized void clearHistory() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(settingsKey).apply();
        searchHistory.clear();
    }
}
