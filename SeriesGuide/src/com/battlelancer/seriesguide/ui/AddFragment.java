
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.SherlockListFragment;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.util.ImageDownloader;
import com.battlelancer.seriesguide.util.Utils;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class AddFragment extends SherlockListFragment {

    protected AddAdapter mAdapter;

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
        if (Utils.isHoneycombOrHigher()) {
            mAdapter.addAll(searchResults);
        } else {
            for (SearchResult searchResult : searchResults) {
                mAdapter.add(searchResult);
            }
        }
        setListAdapter(mAdapter);
    }

    public class AddAdapter extends ArrayAdapter<SearchResult> {

        private LayoutInflater mLayoutInflater;

        private int mLayout;

        private ImageDownloader mImageDownloader;

        public AddAdapter(Context context, int layout, List<SearchResult> objects) {
            super(context, layout, objects);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = layout;
            mImageDownloader = ImageDownloader.getInstance(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(mLayout, null);

                viewHolder = new ViewHolder();
                viewHolder.title = (TextView) convertView.findViewById(R.id.title);
                viewHolder.description = (TextView) convertView.findViewById(R.id.description);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.poster);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // set text properties immediately
            SearchResult item = getItem(position);
            viewHolder.title.setText(item.title);
            viewHolder.description.setText(item.overview);
            if (item.poster != null) {
                mImageDownloader.download(item.poster, viewHolder.poster, false);
            }

            return convertView;
        }
    }

    public final class ViewHolder {

        public TextView title;

        public TextView description;

        public ImageView poster;
    }
}
