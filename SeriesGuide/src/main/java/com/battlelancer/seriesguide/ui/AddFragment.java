package com.battlelancer.seriesguide.ui;

import android.app.Activity;
import android.content.Intent;
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
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.NetworkResult;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.util.AddShowTask;
import com.battlelancer.seriesguide.util.RemoveShowWorkerFragment;
import com.battlelancer.seriesguide.util.TabClickEvent;
import com.battlelancer.seriesguide.widgets.AddIndicator;
import com.battlelancer.seriesguide.widgets.EmptyView;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Super class for fragments displaying a list of shows and allowing to add them to the database.
 */
public abstract class AddFragment extends Fragment {

    public static class OnAddingShowEvent {
        /** Is -1 if adding all shows of a tab. Not updating other tabs then. */
        public final int showTvdbId;

        public OnAddingShowEvent(int showTvdbId) {
            this.showTvdbId = showTvdbId;
        }

        /**
         * Sets TVDB id to -1 to indicate all shows of a tab are added.
         */
        public OnAddingShowEvent() {
            this(-1);
        }
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
        adapter.addAll(searchResults);
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
            SearchResult show = adapter.getItem(position);
            if (show != null && show.state != SearchResult.STATE_ADDING) {
                if (show.state == SearchResult.STATE_ADDED) {
                    // already in library, open it
                    startActivity(new Intent(getContext(), OverviewActivity.class).putExtra(
                            OverviewActivity.EXTRA_INT_SHOW_TVDBID, show.tvdbid));
                } else {
                    // guard against onClick called after fragment is paged away (multi-touch)
                    // onSaveInstanceState might already be called
                    if (isResumed()) {
                        // display more details in a dialog
                        AddShowDialogFragment.showAddDialog(show, getFragmentManager());
                    }
                }
            }
        }
    };

    /**
     * Called if the user triggers adding a single new show through the add dialog. The show is not
     * actually added, yet.
     *
     * @see #onEvent(AddShowTask.OnShowAddedEvent)
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OnAddingShowEvent event) {
        if (event.showTvdbId > 0) {
            adapter.setStateForTvdbId(event.showTvdbId, SearchResult.STATE_ADDING);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AddShowTask.OnShowAddedEvent event) {
        if (event.successful) {
            setShowAdded(event.showTvdbId);
        } else if (event.showTvdbId > 0) {
            setShowNotAdded(event.showTvdbId);
        } else {
            adapter.setAllPendingNotAdded();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RemoveShowWorkerFragment.OnShowRemovedEvent event) {
        if (event.resultCode == NetworkResult.SUCCESS) {
            setShowNotAdded(event.showTvdbId);
        }
    }

    private void setShowAdded(int showTvdbId) {
        adapter.setStateForTvdbId(showTvdbId, SearchResult.STATE_ADDED);
    }

    private void setShowNotAdded(int showTvdbId) {
        adapter.setStateForTvdbId(showTvdbId, SearchResult.STATE_ADD);
    }

    /**
     * @return The tab position in the tab view.
     * @see SearchActivity
     */
    protected abstract int getTabPosition();

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventTabClick(TabClickEvent event) {
        if (event.position == getTabPosition()) {
            resultsGridView.smoothScrollToPosition(0);
        }
    }

    protected static class AddAdapter extends ArrayAdapter<SearchResult> {

        public interface OnItemClickListener {
            void onAddClick(SearchResult item);

            void onMenuWatchlistClick(View view, int showTvdbId);
        }

        private final OnItemClickListener menuClickListener;
        private final boolean showMenuWatchlist;
        private final boolean hideMenuWatchlistIfAdded;

        public AddAdapter(Activity activity, List<SearchResult> objects,
                OnItemClickListener menuClickListener, boolean showMenuWatchlist,
                boolean hideMenuWatchlistIfAdded) {
            super(activity, 0, objects);
            this.menuClickListener = menuClickListener;
            this.showMenuWatchlist = showMenuWatchlist;
            this.hideMenuWatchlistIfAdded = hideMenuWatchlistIfAdded;
        }

        @Nullable
        private SearchResult getItemForShowTvdbId(int showTvdbId) {
            int count = getCount();
            for (int i = 0; i < count; i++) {
                SearchResult item = getItem(i);
                if (item != null && item.tvdbid == showTvdbId) {
                    return item;
                }
            }
            return null;
        }

        public void setStateForTvdbId(int showTvdbId, int state) {
            SearchResult item = getItemForShowTvdbId(showTvdbId);
            if (item != null) {
                item.state = state;
                notifyDataSetChanged();
            }
        }

        public void setAllPendingNotAdded() {
            int count = getCount();
            for (int i = 0; i < count; i++) {
                SearchResult item = getItem(i);
                if (item != null && item.state == SearchResult.STATE_ADDING) {
                    item.state = SearchResult.STATE_ADD;
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_addshow, parent, false);
                holder = new ViewHolder(convertView, menuClickListener);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final SearchResult item = getItem(position);
            if (item == null) {
                return convertView; // all bets are off!
            }
            holder.item = item;

            // hide watchlist menu if not useful
            boolean showMenuWatchlistActual = showMenuWatchlist
                    && (!hideMenuWatchlistIfAdded || item.state != SearchResult.STATE_ADDED);
            holder.buttonContextMenu.setVisibility(showMenuWatchlistActual
                    ? View.VISIBLE : View.GONE);
            // display added indicator instead of add button if already added that show
            holder.addIndicator.setState(item.state);
            String showTitle = item.title;
            holder.addIndicator.setContentDescriptionAdded(
                    getContext().getString(R.string.add_already_exists, showTitle));

            // set text properties immediately
            holder.title.setText(showTitle);
            holder.description.setText(item.overview);

            // only local shows will have a poster path set
            // try to fall back to the first uploaded TVDB poster for all others
            String posterUrl = TvdbImageTools.smallSizeOrResolveUrl(item.posterPath, item.tvdbid,
                    item.language);
            TvdbImageTools.loadUrlResizeCrop(getContext(), holder.poster, posterUrl);

            return convertView;
        }

        static class ViewHolder {

            public SearchResult item;

            @BindView(R.id.textViewAddTitle) public TextView title;
            @BindView(R.id.textViewAddDescription) public TextView description;
            @BindView(R.id.imageViewAddPoster) public ImageView poster;
            @BindView(R.id.addIndicatorAddShow) public AddIndicator addIndicator;
            @BindView(R.id.buttonItemAddMore) public ImageView buttonContextMenu;

            public ViewHolder(View view,
                    final OnItemClickListener onItemClickListener) {
                ButterKnife.bind(this, view);
                addIndicator.setOnAddClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (onItemClickListener != null) {
                            onItemClickListener.onAddClick(item);
                        }
                    }
                });
                buttonContextMenu.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (onItemClickListener != null) {
                            onItemClickListener.onMenuWatchlistClick(v, item.tvdbid);
                        }
                    }
                });
            }
        }
    }
}
