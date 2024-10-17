// SPDX-License-Identifier: Apache-2.0
// Copyright 2011-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.GridView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentAddshowTraktBinding
import com.battlelancer.seriesguide.databinding.ItemAddshowBinding
import com.battlelancer.seriesguide.enums.NetworkResult
import com.battlelancer.seriesguide.shows.tools.AddShowTask
import com.battlelancer.seriesguide.shows.tools.ShowTools2
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.widgets.EmptyView
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools.setContextAndLongClickListener
import com.battlelancer.seriesguide.util.tasks.BaseShowActionTask.ShowChangedEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.LinkedList

/**
 * Displays the watched shows, show collection or shows watchlist of the connected Trakt user for
 * the purpose of adding them. Shows on the watchlist can also be removed from the watchlist.
 */
class TraktAddFragment : Fragment() {

    private var binding: FragmentAddshowTraktBinding? = null
    private val contentContainer: View
        get() = binding!!.containerAddContent

    private val progressBar: View
        get() = binding!!.progressBarAdd

    private val resultsGridView: GridView
        get() = binding!!.gridViewAdd

    private val emptyView: EmptyView
        get() = binding!!.emptyViewAdd

    private lateinit var listType: TraktAddLoader.Type
    private var searchResults: List<SearchResult>? = null
    private lateinit var adapter: AddAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val typeOrdinal = requireArguments().getInt(ARG_TYPE)
        listType = TraktAddLoader.Type.entries.find { it.ordinal == typeOrdinal }
            ?: throw IllegalArgumentException("Invalid Type ordinal $typeOrdinal")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAddshowTraktBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEmptyView(emptyView)
        // basic setup of grid view
        resultsGridView.emptyView = emptyView
        // enable app bar scrolling out of view
        ViewCompat.setNestedScrollingEnabled(resultsGridView, true)

        ThemeUtils.applyBottomPaddingForNavigationBar(resultsGridView)
        ThemeUtils.applyBottomMarginForNavigationBar(binding!!.textViewPoweredByAddShowTrakt)

        // set initial view states
        setProgressVisible(visible = true, animate = false)

        // set up adapter
        adapter = AddAdapter(
            requireActivity(), ArrayList(), itemClickListener,
            listType == TraktAddLoader.Type.WATCHLIST
        )

        // load data
        LoaderManager.getInstance(this)
            .initLoader(
                ShowsTraktActivity.TRAKT_BASE_LOADER_ID + listType.ordinal, null,
                traktAddCallbacks
            )

        // add menu options
        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private val itemClickListener: AddAdapter.ItemClickListener =
        object : AddAdapter.ItemClickListener {
            override fun onItemClick(item: SearchResult) {
                if (item.state != SearchResult.STATE_ADDING) {
                    if (item.state == SearchResult.STATE_ADDED) {
                        // already in library, open it
                        startActivity(
                            OverviewActivity.intentShowByTmdbId(requireContext(), item.tmdbId)
                        )
                    } else {
                        // display more details in a dialog
                        AddShowDialogFragment.show(parentFragmentManager, item)
                    }
                }
            }

            override fun onAddClick(item: SearchResult) {
                EventBus.getDefault().post(OnAddingShowEvent(item.tmdbId))
                TaskManager.getInstance().performAddTask(requireContext(), item)
            }

            override fun onMoreOptionsClick(view: View, show: SearchResult) {
                AddShowPopupMenu(requireContext(), show, view).apply {
                    // For watchlist only show remove from, for other lists show no watchlist option
                    hideAddToWatchlistAction()
                    if (listType != TraktAddLoader.Type.WATCHLIST) {
                        hideRemoveFromWatchlistAction()
                    }
                }.show()
            }
        }

    private val optionsMenuProvider: MenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.trakt_library_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val itemId = menuItem.itemId
            if (itemId == R.id.menu_add_all) {
                val searchResults = searchResults
                if (searchResults != null) {
                    val showsToAdd = LinkedList<SearchResult>()
                    // only include shows not already added
                    for (result in searchResults) {
                        if (result.state == SearchResult.STATE_ADD) {
                            showsToAdd.add(result)
                            result.state = SearchResult.STATE_ADDING
                        }
                    }
                    EventBus.getDefault().post(OnAddingShowEvent())
                    TaskManager.getInstance()
                        .performAddTask(context, showsToAdd, false, false)
                }
                // disable the item so the user has to come back
                menuItem.isEnabled = false
                return true
            }
            return false
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: ShowChangedEvent?) {
        if (listType == TraktAddLoader.Type.WATCHLIST) {
            // reload watchlist if a show was removed
            LoaderManager.getInstance(this)
                .restartLoader(
                    ShowsTraktActivity.TRAKT_BASE_LOADER_ID + listType.ordinal, null,
                    traktAddCallbacks
                )
        }
    }

    private fun setupEmptyView(buttonEmptyView: EmptyView) {
        buttonEmptyView.setButtonClickListener {
            setProgressVisible(visible = true, animate = false)
            LoaderManager.getInstance(this@TraktAddFragment)
                .restartLoader(
                    ShowsTraktActivity.TRAKT_BASE_LOADER_ID + listType.ordinal, null,
                    traktAddCallbacks
                )
        }
    }

    /**
     * Changes the empty message.
     */
    private fun setEmptyMessage(message: CharSequence) {
        emptyView.setMessage(message)
    }

    private fun updateSearchResults(searchResults: List<SearchResult>) {
        this.searchResults = searchResults
        adapter.clear()
        adapter.addAll(searchResults)
        resultsGridView.adapter = adapter
    }

    /**
     * Hides the content container and shows a progress bar.
     */
    private fun setProgressVisible(visible: Boolean, animate: Boolean) {
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
    fun onEvent(event: AddShowTask.OnShowAddedEvent) {
        if (event.successful) {
            setShowAdded(event.showTmdbId)
        } else if (event.showTmdbId > 0) {
            setShowNotAdded(event.showTmdbId)
        } else {
            adapter.setAllPendingNotAdded()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ShowTools2.OnShowRemovedEvent) {
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

    private val traktAddCallbacks: LoaderManager.LoaderCallbacks<TraktAddLoader.Result> =
        object : LoaderManager.LoaderCallbacks<TraktAddLoader.Result> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<TraktAddLoader.Result> {
                return TraktAddLoader(requireContext(), listType)
            }

            override fun onLoadFinished(
                loader: Loader<TraktAddLoader.Result>,
                data: TraktAddLoader.Result
            ) {
                updateSearchResults(data.results)
                setEmptyMessage(data.emptyText)
                setProgressVisible(false, true)
            }

            override fun onLoaderReset(loader: Loader<TraktAddLoader.Result>) {
                // keep currently displayed data
            }
        }

    companion object {
        /**
         * Which Trakt list should be shown. Ordinal of one of [TraktAddLoader.Type].
         */
        private const val ARG_TYPE = "traktListType"

        val liftOnScrollTargetViewId = R.id.gridViewAdd

        fun newInstance(type: TraktAddLoader.Type): TraktAddFragment {
            val f = TraktAddFragment()
            val args = Bundle()
            args.putInt(ARG_TYPE, type.ordinal)
            f.arguments = args
            return f
        }
    }

    class AddAdapter(
        activity: Activity,
        objects: List<SearchResult>,
        private val itemClickListener: ItemClickListener,
        private val showWatchlistActions: Boolean
    ) : ArrayAdapter<SearchResult>(activity, 0, objects) {

        interface ItemClickListener {
            fun onItemClick(item: SearchResult)
            fun onAddClick(item: SearchResult)
            fun onMoreOptionsClick(view: View, show: SearchResult)
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
                holder = ViewHolder.inflate(parent, itemClickListener)
                    .also { it.binding.root.tag = it }
                view = holder.binding.root
            } else {
                holder = convertView.tag as ViewHolder
                view = convertView
            }

            val item = getItem(position)
            holder.bindTo(item, context, showWatchlistActions)

            return view
        }

        class ViewHolder(
            val binding: ItemAddshowBinding,
            private val itemClickListener: ItemClickListener
        ) {
            private var item: SearchResult? = null

            init {
                binding.root.setOnClickListener {
                    item?.let { itemClickListener.onItemClick(it) }
                }
                binding.addIndicatorAddShow.setOnAddClickListener {
                    item?.let { itemClickListener.onAddClick(it) }
                }
                binding.buttonItemAddMoreOptions.also {
                    TooltipCompat.setTooltipText(it, it.contentDescription)
                }
            }

            private fun onMoreOptionsClick(anchor: View) {
                item?.let {
                    itemClickListener.onMoreOptionsClick(anchor, it)
                }
            }

            fun bindTo(item: SearchResult?, context: Context, showWatchlistActions: Boolean) {
                this.item = item

                if (item == null) {
                    binding.addIndicatorAddShow.isGone = true
                    binding.textViewAddTitle.text = null
                    binding.textViewAddDescription.text = null
                    binding.imageViewAddPoster.setImageDrawable(null)
                } else {
                    val canBeAdded = item.state == SearchResult.STATE_ADD
                    // Even if not displaying more options button, if not added, always display add action on
                    // long press for accessibility.
                    binding.root.apply {
                        if (showWatchlistActions || canBeAdded) {
                            setContextAndLongClickListener {
                                onMoreOptionsClick(binding.root)
                            }
                        } else {
                            // Remove listener so there is no long press feedback
                            setContextAndLongClickListener(null)
                        }
                    }
                    // Only display more options button when displaying remove from watchlist action
                    binding.buttonItemAddMoreOptions.apply {
                        if (showWatchlistActions) {
                            setOnClickListener {
                                onMoreOptionsClick(binding.buttonItemAddMoreOptions)
                            }
                            isVisible = true
                        } else {
                            setOnClickListener(null)
                            isGone = true
                        }
                    }

                    // add indicator
                    val showTitle = item.title
                    binding.addIndicatorAddShow.apply {
                        setState(item.state)
                        setNameOfAssociatedItem(showTitle)
                        isVisible = true
                    }

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
                fun inflate(parent: ViewGroup, itemClickListener: ItemClickListener) =
                    ViewHolder(
                        ItemAddshowBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                        ),
                        itemClickListener
                    )
            }
        }
    }
}