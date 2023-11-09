// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.search.discover

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.GridView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemAddshowBinding
import com.battlelancer.seriesguide.enums.NetworkResult
import com.battlelancer.seriesguide.shows.tools.AddShowTask.OnShowAddedEvent
import com.battlelancer.seriesguide.shows.tools.ShowTools2.OnShowRemovedEvent
import com.battlelancer.seriesguide.ui.widgets.EmptyView
import com.battlelancer.seriesguide.util.ImageTools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Super class for fragments displaying a list of shows that can be added to the database.
 */
abstract class AddFragment : Fragment() {

    class OnAddingShowEvent(
        /**
         * Is -1 if adding all shows this lists.
         */
        val showTmdbId: Int
    ) {
        /**
         * Sets TMDB id to -1 to indicate all shows of this are added.
         */
        internal constructor() : this(-1)
    }

    protected var searchResults: List<SearchResult>? = null
        private set
    protected lateinit var adapter: AddAdapter

    /**
     * Implementers should inflate their own layout and provide views through getters.
     */
    abstract override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View

    abstract val contentContainer: View
    abstract val progressBar: View
    abstract val resultsGridView: GridView
    abstract val emptyView: EmptyView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEmptyView(emptyView)
        // basic setup of grid view
        resultsGridView.emptyView = emptyView
        // enable app bar scrolling out of view
        ViewCompat.setNestedScrollingEnabled(resultsGridView, true)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    protected abstract fun setupEmptyView(buttonEmptyView: EmptyView)

    /**
     * Changes the empty message.
     */
    protected fun setEmptyMessage(message: CharSequence) {
        emptyView.setMessage(message)
    }

    fun updateSearchResults(searchResults: List<SearchResult>) {
        this.searchResults = searchResults
        adapter.clear()
        adapter.addAll(searchResults)
        resultsGridView.adapter = adapter
    }

    /**
     * Hides the content container and shows a progress bar.
     */
    protected fun setProgressVisible(visible: Boolean, animate: Boolean) {
        if (animate) {
            val fadeOut = AnimationUtils.loadAnimation(activity, android.R.anim.fade_out)
            val fadeIn = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in)
            contentContainer.startAnimation(if (visible) fadeOut else fadeIn)
            progressBar.startAnimation(if (visible) fadeIn else fadeOut)
        }
        contentContainer.visibility = if (visible) View.GONE else View.VISIBLE
        progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Called if the user triggers adding a single new show through the add dialog. The show is not
     * actually added, yet.
     *
     * See [onEvent(OnShowAddedEvent)][onEvent].
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: OnAddingShowEvent) {
        if (event.showTmdbId > 0) {
            adapter.setStateForTmdbId(event.showTmdbId, SearchResult.STATE_ADDING)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: OnShowAddedEvent) {
        if (event.successful) {
            setShowAdded(event.showTmdbId)
        } else if (event.showTmdbId > 0) {
            setShowNotAdded(event.showTmdbId)
        } else {
            adapter.setAllPendingNotAdded()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: OnShowRemovedEvent) {
        if (event.resultCode == NetworkResult.SUCCESS) {
            setShowNotAdded(event.showTmdbId)
        }
    }

    private fun setShowAdded(showTmdbId: Int) {
        adapter.setStateForTmdbId(showTmdbId, SearchResult.STATE_ADDED)
    }

    private fun setShowNotAdded(showTmdbId: Int) {
        adapter.setStateForTmdbId(showTmdbId, SearchResult.STATE_ADD)
    }

    class AddAdapter(
        activity: Activity,
        objects: List<SearchResult>,
        private val menuClickListener: OnItemClickListener,
        private val showMenuWatchlist: Boolean
    ) : ArrayAdapter<SearchResult>(activity, 0, objects) {

        interface OnItemClickListener {
            fun onItemClick(item: SearchResult)
            fun onAddClick(item: SearchResult)
            fun onMenuWatchlistClick(view: View, showTmdbId: Int)
        }

        private fun getItemForShowTmdbId(showTmdbId: Int): SearchResult? {
            val count = count
            for (i in 0 until count) {
                val item = getItem(i)
                if (item != null && item.tmdbId == showTmdbId) {
                    return item
                }
            }
            return null
        }

        fun setStateForTmdbId(showTmdbId: Int, state: Int) {
            val item = getItemForShowTmdbId(showTmdbId)
            if (item != null) {
                item.state = state
                notifyDataSetChanged()
            }
        }

        fun setAllPendingNotAdded() {
            val count = count
            for (i in 0 until count) {
                val item = getItem(i)
                if (item != null && item.state == SearchResult.STATE_ADDING) {
                    item.state = SearchResult.STATE_ADD
                }
            }
            notifyDataSetChanged()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val holder: ViewHolder
            if (convertView == null) {
                holder = ViewHolder.inflate(parent, menuClickListener)
                    .also { it.binding.root.tag = it }
                view = holder.binding.root
            } else {
                holder = convertView.tag as ViewHolder
                view = convertView
            }

            val item = getItem(position)
            holder.bindTo(item, context, showMenuWatchlist)

            return view
        }

        class ViewHolder(
            val binding: ItemAddshowBinding,
            onItemClickListener: OnItemClickListener
        ) {
            private var item: SearchResult? = null

            init {
                binding.root.setOnClickListener {
                    item?.let { onItemClickListener.onItemClick(it) }
                }
                binding.addIndicatorAddShow.setOnAddClickListener {
                    item?.let { onItemClickListener.onAddClick(it) }
                }
                binding.buttonItemAddMore.setOnClickListener { v: View ->
                    item?.let { onItemClickListener.onMenuWatchlistClick(v, it.tmdbId) }
                }
            }

            fun bindTo(item: SearchResult?, context: Context, showMenuWatchlist: Boolean) {
                this.item = item

                // hide watchlist menu if not useful
                binding.buttonItemAddMore.visibility =
                    if (showMenuWatchlist) View.VISIBLE else View.GONE

                if (item == null) {
                    binding.addIndicatorAddShow.setState(SearchResult.STATE_ADD)
                    binding.addIndicatorAddShow.setContentDescriptionAdded(null)
                    binding.textViewAddTitle.text = null
                    binding.textViewAddDescription.text = null
                    binding.imageViewAddPoster.setImageDrawable(null)
                } else {
                    // display added indicator instead of add button if already added that show
                    binding.addIndicatorAddShow.setState(item.state)
                    val showTitle = item.title
                    binding.addIndicatorAddShow.setContentDescriptionAdded(
                        context.getString(R.string.add_already_exists, showTitle)
                    )

                    // set text properties immediately
                    binding.textViewAddTitle.text = showTitle
                    binding.textViewAddDescription.text = item.overview

                    // only local shows will have a poster path set
                    // try to fall back to the TMDB poster for all others
                    val posterUrl = ImageTools.posterUrlOrResolve(
                        item.posterPath,
                        item.tmdbId,
                        item.language,
                        context
                    )
                    ImageTools.loadShowPosterUrlResizeCrop(
                        context, binding.imageViewAddPoster,
                        posterUrl
                    )
                }
            }

            companion object {
                fun inflate(parent: ViewGroup, onItemClickListener: OnItemClickListener) =
                    ViewHolder(
                        ItemAddshowBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                        ),
                        onItemClickListener
                    )
            }
        }
    }
}