package com.battlelancer.seriesguide.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.EmptyView;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Super class for fragments displaying a list of shows and allowing to add them to the database.
 */
public abstract class AddFragment extends Fragment {

    public static class AddShowEvent {
    }

    @BindView(R.id.containerAddContent) View contentContainer;
    @BindView(R.id.progressBarAdd) View progressBar;
    @BindView(android.R.id.list) GridView resultsGridView;
    @BindView(R.id.emptyViewAdd) EmptyView emptyView;

    protected List<SearchResult> searchResults;
    protected AddAdapter adapter;

    /**
     * Implementers should inflate their own layout and inject views with {@link
     * butterknife.ButterKnife}.
     */
    @Override
    public abstract View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState);

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupEmptyView(emptyView);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // basic setup of grid view
        resultsGridView.setEmptyView(emptyView);
        resultsGridView.setOnItemClickListener(mItemClickListener);
        // enable app bar scrolling out of view only on L or higher
        ViewCompat.setNestedScrollingEnabled(resultsGridView, AndroidUtils.isLollipopOrHigher());

        // restore an existing adapter
        if (adapter != null) {
            resultsGridView.setAdapter(adapter);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    protected abstract void setupEmptyView(EmptyView buttonEmptyView);

    /**
     * Changes the empty message.
     */
    public void setEmptyMessage(CharSequence message) {
        emptyView.setMessage(message);
    }

    public void setSearchResults(List<SearchResult> searchResults) {
        this.searchResults = searchResults;
        adapter.clear();
        if (AndroidUtils.isHoneycombOrHigher()) {
            adapter.addAll(searchResults);
        } else {
            for (SearchResult searchResult : searchResults) {
                adapter.add(searchResult);
            }
        }
        resultsGridView.setAdapter(adapter);
    }

    /**
     * Hides the content container and shows a progress bar.
     */
    public void setProgressVisible(boolean visible, boolean animate) {
        if (animate) {
            Animation out = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
            Animation in = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
            contentContainer.startAnimation(visible ? out : in);
            progressBar.startAnimation(visible ? in : out);
        }
        contentContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    protected AdapterView.OnItemClickListener mItemClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // display more details in a dialog
            SearchResult show = adapter.getItem(position);
            AddShowDialogFragment.showAddDialog(show, getFragmentManager());
        }
    };

    /**
     * Called if the user adds a new show through the dialog.
     */
    @SuppressWarnings("UnusedParameters")
    @Subscribe
    public void onEvent(AddShowEvent event) {
        adapter.notifyDataSetChanged();
    }

    protected static class AddAdapter extends ArrayAdapter<SearchResult> {

        public interface OnContextMenuClickListener {
            void onClick(View view, int showTvdbId);
        }

        private final SgApp app;
        private final OnContextMenuClickListener menuClickListener;
        private final boolean hideContextMenuIfAdded;
        private final LayoutInflater inflater;

        public AddAdapter(Activity activity, List<SearchResult> objects,
                OnContextMenuClickListener menuClickListener,
                boolean hideContextMenuIfAdded) {
            super(activity, 0, objects);
            this.app = SgApp.from(activity);
            this.menuClickListener = menuClickListener;
            this.hideContextMenuIfAdded = hideContextMenuIfAdded;
            this.inflater = LayoutInflater.from(activity);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_addshow, parent, false);
                holder = new ViewHolder(convertView, menuClickListener);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final SearchResult item = getItem(position);
            if (item == null) {
                return convertView; // all bets are off!
            }
            holder.showTvdbId = item.tvdbid;

            // hide context menu if not useful
            holder.buttonContextMenu.setVisibility(menuClickListener == null ||
                    (hideContextMenuIfAdded && item.isAdded) ? View.GONE : View.VISIBLE);
            // display added indicator instead of add button if already added that show
            holder.addbutton.setVisibility(item.isAdded ? View.GONE : View.VISIBLE);
            holder.addedIndicator.setVisibility(item.isAdded ? View.VISIBLE : View.GONE);
            String showTitle = item.title;
            holder.addedIndicator.setContentDescription(
                    getContext().getString(R.string.add_already_exists, showTitle));
            holder.addbutton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.isAdded = true;
                    EventBus.getDefault().post(new AddShowEvent());

                    TaskManager.getInstance(getContext()).performAddTask(app, item);
                }
            });

            // set text properties immediately
            holder.title.setText(showTitle);
            holder.description.setText(item.overview);

            // only local shows will have a poster path set
            // try to fall back to the first uploaded TVDB poster for all others
            if (item.poster == null) {
                // only load posters from caching server that are not from local shows
                // because those are still loaded from TVDB directly
                // and we do not want to cache them on device twice
                Utils.loadTvdbShowPosterFromCache(getContext(), holder.poster,
                        TvdbTools.buildFallbackPosterPath(item.tvdbid));
            } else {
                Utils.loadTvdbShowPoster(getContext(), holder.poster, item.poster);
            }

            return convertView;
        }

        static class ViewHolder {

            public int showTvdbId;

            public TextView title;
            public TextView description;
            public ImageView poster;
            public View addbutton;
            public View addedIndicator;
            public ImageView buttonContextMenu;

            public ViewHolder(View view,
                    final OnContextMenuClickListener contextMenuClickListener) {
                title = (TextView) view.findViewById(R.id.textViewAddTitle);
                description = (TextView) view.findViewById(R.id.textViewAddDescription);
                poster = (ImageView) view.findViewById(R.id.imageViewAddPoster);
                addbutton = view.findViewById(R.id.viewAddButton);
                addedIndicator = view.findViewById(R.id.imageViewAddedIndicator);
                buttonContextMenu = (ImageView) view.findViewById(R.id.buttonItemAddMore);
                buttonContextMenu.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (contextMenuClickListener != null) {
                            contextMenuClickListener.onClick(v, showTvdbId);
                        }
                    }
                });
            }
        }
    }
}
