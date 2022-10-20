package com.battlelancer.seriesguide.shows.search.discover;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.ItemAddshowBinding;
import com.battlelancer.seriesguide.enums.NetworkResult;
import com.battlelancer.seriesguide.shows.tools.AddShowTask;
import com.battlelancer.seriesguide.shows.tools.ShowTools2;
import com.battlelancer.seriesguide.ui.widgets.EmptyView;
import com.battlelancer.seriesguide.util.ImageTools;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Super class for fragments displaying a list of shows and allowing to add them to the database.
 */
public abstract class AddFragment extends Fragment {

    public static class OnAddingShowEvent {
        /**
         * Is -1 if adding all shows of a tab. Not updating other tabs then.
         */
        public final int showTmdbId;

        public OnAddingShowEvent(int showTmdbId) {
            this.showTmdbId = showTmdbId;
        }

        /**
         * Sets TMDB id to -1 to indicate all shows of a tab are added.
         */
        OnAddingShowEvent() {
            this(-1);
        }
    }

    protected List<SearchResult> searchResults;
    protected AddAdapter adapter;

    /**
     * Implementers should inflate their own layout and provide views through getters.
     */
    @Override
    public abstract View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    public abstract View getContentContainer();

    public abstract View getProgressBar();

    public abstract GridView getResultsGridView();

    public abstract EmptyView getEmptyView();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupEmptyView(getEmptyView());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // basic setup of grid view
        getResultsGridView().setEmptyView(getEmptyView());
        // enable app bar scrolling out of view
        ViewCompat.setNestedScrollingEnabled(getResultsGridView(), true);

        // restore an existing adapter
        if (adapter != null) {
            getResultsGridView().setAdapter(adapter);
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
    protected void setEmptyMessage(CharSequence message) {
        getEmptyView().setMessage(message);
    }

    public void setSearchResults(List<SearchResult> searchResults) {
        this.searchResults = searchResults;
        adapter.clear();
        adapter.addAll(searchResults);
        getResultsGridView().setAdapter(adapter);
    }

    /**
     * Hides the content container and shows a progress bar.
     */
    protected void setProgressVisible(boolean visible, boolean animate) {
        if (animate) {
            Animation out = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
            Animation in = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
            getContentContainer().startAnimation(visible ? out : in);
            getProgressBar().startAnimation(visible ? in : out);
        }
        getContentContainer().setVisibility(visible ? View.GONE : View.VISIBLE);
        getProgressBar().setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Called if the user triggers adding a single new show through the add dialog. The show is not
     * actually added, yet.
     *
     * @see #onEvent(AddShowTask.OnShowAddedEvent)
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OnAddingShowEvent event) {
        if (event.showTmdbId > 0) {
            adapter.setStateForTmdbId(event.showTmdbId, SearchResult.STATE_ADDING);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AddShowTask.OnShowAddedEvent event) {
        if (event.successful) {
            setShowAdded(event.showTmdbId);
        } else if (event.showTmdbId > 0) {
            setShowNotAdded(event.showTmdbId);
        } else {
            adapter.setAllPendingNotAdded();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ShowTools2.OnShowRemovedEvent event) {
        if (event.getResultCode() == NetworkResult.SUCCESS) {
            setShowNotAdded(event.getShowTmdbId());
        }
    }

    private void setShowAdded(int showTmdbId) {
        adapter.setStateForTmdbId(showTmdbId, SearchResult.STATE_ADDED);
    }

    private void setShowNotAdded(int showTmdbId) {
        adapter.setStateForTmdbId(showTmdbId, SearchResult.STATE_ADD);
    }

    public static class AddAdapter extends ArrayAdapter<SearchResult> {

        public interface OnItemClickListener {
            void onItemClick(SearchResult item);

            void onAddClick(SearchResult item);

            void onMenuWatchlistClick(View view, int showTmdbId);
        }

        private final OnItemClickListener menuClickListener;
        private final boolean showMenuWatchlist;

        public AddAdapter(Activity activity, List<SearchResult> objects,
                OnItemClickListener menuClickListener, boolean showMenuWatchlist) {
            super(activity, 0, objects);
            this.menuClickListener = menuClickListener;
            this.showMenuWatchlist = showMenuWatchlist;
        }

        @Nullable
        private SearchResult getItemForShowTmdbId(int showTmdbId) {
            int count = getCount();
            for (int i = 0; i < count; i++) {
                SearchResult item = getItem(i);
                if (item != null && item.getTmdbId() == showTmdbId) {
                    return item;
                }
            }
            return null;
        }

        public void setStateForTmdbId(int showTmdbId, int state) {
            SearchResult item = getItemForShowTmdbId(showTmdbId);
            if (item != null) {
                item.setState(state);
                notifyDataSetChanged();
            }
        }

        public void setAllPendingNotAdded() {
            int count = getCount();
            for (int i = 0; i < count; i++) {
                SearchResult item = getItem(i);
                if (item != null && item.getState() == SearchResult.STATE_ADDING) {
                    item.setState(SearchResult.STATE_ADD);
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                ItemAddshowBinding binding = ItemAddshowBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false);
                holder = new ViewHolder(binding, menuClickListener);
                convertView = binding.getRoot();
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final SearchResult item = getItem(position);
            if (item == null) {
                return convertView; // all bets are off!
            }
            holder.bindTo(item, getContext(), showMenuWatchlist);

            return convertView;
        }

        static class ViewHolder {

            private final ItemAddshowBinding binding;
            @Nullable private SearchResult item;

            public ViewHolder(ItemAddshowBinding binding,
                    final OnItemClickListener onItemClickListener) {
                this.binding = binding;
                binding.getRoot().setOnClickListener(v -> {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(item);
                    }
                });
                binding.addIndicatorAddShow.setOnAddClickListener(v -> {
                    if (onItemClickListener != null) {
                        onItemClickListener.onAddClick(item);
                    }
                });
                binding.buttonItemAddMore.setOnClickListener(v -> {
                    if (onItemClickListener != null && item != null) {
                        onItemClickListener.onMenuWatchlistClick(v, item.getTmdbId());
                    }
                });
            }

            public void bindTo(@Nullable SearchResult item, Context context, boolean showMenuWatchlist) {
                this.item = item;
                // hide watchlist menu if not useful
                binding.buttonItemAddMore.setVisibility(
                        showMenuWatchlist ? View.VISIBLE : View.GONE);
                if (item == null) {
                    binding.addIndicatorAddShow.setState(SearchResult.STATE_ADD);
                    binding.addIndicatorAddShow.setContentDescriptionAdded(null);
                    binding.textViewAddTitle.setText(null);
                    binding.textViewAddDescription.setText(null);
                    binding.imageViewAddPoster.setImageDrawable(null);
                } else {
                    // display added indicator instead of add button if already added that show
                    binding.addIndicatorAddShow.setState(item.getState());
                    String showTitle = item.getTitle();
                    binding.addIndicatorAddShow.setContentDescriptionAdded(
                            context.getString(R.string.add_already_exists, showTitle));

                    // set text properties immediately
                    binding.textViewAddTitle.setText(showTitle);
                    binding.textViewAddDescription.setText(item.getOverview());

                    // only local shows will have a poster path set
                    // try to fall back to the TMDB poster for all others
                    String posterUrl = ImageTools.posterUrlOrResolve(item.getPosterPath(),
                            item.getTmdbId(),
                            item.getLanguage(),
                            context);
                    ImageTools.loadShowPosterUrlResizeCrop(context, binding.imageViewAddPoster,
                            posterUrl);
                }
            }
        }
    }
}
