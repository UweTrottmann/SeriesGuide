package com.battlelancer.seriesguide.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.battlelancer.seriesguide.provider.SeriesGuideProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import timber.log.Timber;

/**
 * Helper for building selection clauses for {@link SQLiteDatabase}. Each
 * appended clause is combined using {@code AND}. This class is <em>not</em>
 * thread safe.
 */
public class SelectionBuilder {

    private String table = null;
    private Map<String, String> projectionMap = new HashMap<>();
    private StringBuilder selection = new StringBuilder();
    private ArrayList<String> selectionArgs = new ArrayList<>();

    /**
     * Reset any internal state, allowing this builder to be recycled.
     */
    public SelectionBuilder reset() {
        table = null;
        selection.setLength(0);
        selectionArgs.clear();
        return this;
    }

    /**
     * Append the given selection clause to the internal state. Each clause is
     * surrounded with parenthesis and combined using {@code AND}.
     */
    public SelectionBuilder where(String selection, String... selectionArgs) {
        if (TextUtils.isEmpty(selection)) {
            if (selectionArgs != null && selectionArgs.length > 0) {
                throw new IllegalArgumentException(
                        "Valid selection required when including arguments=");
            }

            // Shortcut when clause is empty
            return this;
        }

        if (this.selection.length() > 0) {
            this.selection.append(" AND ");
        }

        this.selection.append("(").append(selection).append(")");
        if (selectionArgs != null) {
            Collections.addAll(this.selectionArgs, selectionArgs);
        }

        return this;
    }

    public SelectionBuilder table(String table) {
        this.table = table;
        return this;
    }

    private void assertTable() {
        if (table == null) {
            throw new IllegalStateException("Table not specified");
        }
    }

    public SelectionBuilder mapToTable(String column, String table) {
        projectionMap.put(column, table + "." + column);
        return this;
    }

    public SelectionBuilder map(String fromColumn, String toClause) {
        projectionMap.put(fromColumn, toClause + " AS " + fromColumn);
        return this;
    }

    /**
     * Return selection string for current internal state.
     * 
     * @see #getSelectionArgs()
     */
    public String getSelection() {
        return selection.toString();
    }

    /**
     * Return selection arguments for current internal state.
     * 
     * @see #getSelection()
     */
    public String[] getSelectionArgs() {
        return selectionArgs.toArray(new String[selectionArgs.size()]);
    }

    private void mapColumns(String[] columns) {
        for (int i = 0; i < columns.length; i++) {
            final String target = projectionMap.get(columns[i]);
            if (target != null) {
                columns[i] = target;
            }
        }
    }

    @Override
    public String toString() {
        return "SelectionBuilder[table=" + table + ", selection=" + getSelection()
                + ", selectionArgs=" + Arrays.toString(getSelectionArgs()) + "]";
    }

    /**
     * Execute query using the current internal state as {@code WHERE} clause.
     */
    public Cursor query(SQLiteDatabase db, String[] columns, String orderBy) {
        return query(db, columns, null, null, orderBy, null);
    }

    /**
     * Execute query using the current internal state as {@code WHERE} clause.
     */
    public Cursor query(SQLiteDatabase db, String[] columns, String groupBy, String having,
            String orderBy, String limit) {
        assertTable();
        if (columns != null)
            mapColumns(columns);
        if (SeriesGuideProvider.LOGV)
            Timber.v("query(columns=" + Arrays.toString(columns) + ") " + this);
        return db.query(table, columns, getSelection(), getSelectionArgs(), groupBy, having,
                orderBy, limit);
    }

    /**
     * Execute update using the current internal state as {@code WHERE} clause.
     */
    public int update(SQLiteDatabase db, ContentValues values) {
        assertTable();
        if (SeriesGuideProvider.LOGV)
            Timber.v("update() " + this);
        return db.update(table, values, getSelection(), getSelectionArgs());
    }

    /**
     * Execute delete using the current internal state as {@code WHERE} clause.
     */
    public int delete(SQLiteDatabase db) {
        assertTable();
        if (SeriesGuideProvider.LOGV)
            Timber.v("delete() " + this);
        return db.delete(table, getSelection(), getSelectionArgs());
    }
}
