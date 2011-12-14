
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.SearchResult;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

public class AddFragment extends ListFragment {

    protected ArrayAdapter<SearchResult> mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // so we don't have to do a network op each config change
        setRetainInstance(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        SearchResult result = mAdapter.getItem(position);
        AddDialogFragment.showAddDialog(result, getFragmentManager());
    }

    protected void setSearchResults(List<SearchResult> searchResults) {
        mAdapter.clear();
        if (Utils.isHoneycomb()) {
            mAdapter.addAll(searchResults);
        } else {
            for (SearchResult searchResult : searchResults) {
                mAdapter.add(searchResult);
            }
        }
        setListAdapter(mAdapter);
    }
}
