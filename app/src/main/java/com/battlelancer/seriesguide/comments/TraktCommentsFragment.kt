package com.battlelancer.seriesguide.comments

import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentCommentsBinding
import com.battlelancer.seriesguide.traktapi.TraktAction
import com.battlelancer.seriesguide.traktapi.TraktTask
import com.battlelancer.seriesguide.traktapi.TraktTask.TraktActionCompleteEvent
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.WebTools
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.trakt5.TraktLink
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays show or episode comments and supports posting comments.
 */
class TraktCommentsFragment : Fragment() {

    interface InitBundle {
        companion object {
            const val MOVIE_TMDB_ID = "movie"
            const val SHOW_ID = "show"
            const val EPISODE_ID = "episode"
        }
    }

    private var binding: FragmentCommentsBinding? = null
    private lateinit var adapter: TraktCommentsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentCommentsBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding!!

        if (resources.getBoolean(R.bool.isWideCommentsLayout)) {
            ThemeUtils.applyBottomPaddingForNavigationBar(binding.recyclerViewComments)
            ThemeUtils.applyBottomMarginForNavigationBar(binding.textViewPoweredByComments)
        } else {
            ThemeUtils.applyBottomPaddingForNavigationBar(binding.containerComments)
        }

        binding.swipeRefreshLayoutShouts.setSwipeableChildren(
            R.id.scrollViewComments,
            R.id.recyclerViewComments
        )
        binding.swipeRefreshLayoutShouts.setOnRefreshListener { refreshCommentsWithNetworkCheck() }
        binding.swipeRefreshLayoutShouts.setProgressViewOffset(
            false,
            resources.getDimensionPixelSize(
                R.dimen.swipe_refresh_progress_bar_start_margin
            ),
            resources.getDimensionPixelSize(
                R.dimen.swipe_refresh_progress_bar_end_margin
            )
        )
        ViewTools.setSwipeRefreshLayoutColors(
            requireActivity().theme,
            binding.swipeRefreshLayoutShouts
        )

        binding.buttonShouts.setOnClickListener {
            try {
                comment()
            } catch (e: IllegalArgumentException) {
                Toast.makeText(requireContext(), R.string.database_error, Toast.LENGTH_LONG).show()
                Errors.logAndReport("comment", e)
            }
        }

        // disable comment button by default, enable if comment entered
        binding.buttonShouts.isEnabled = false
        binding.textFieldComments.editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.buttonShouts.isEnabled = !TextUtils.isEmpty(s)
            }

            override fun afterTextChanged(s: Editable) {}
        })

        // set initial view states
        binding.recyclerViewComments.isGone = false
        binding.textViewShoutsEmpty.isGone = true
        showProgressBar(true)

        // setup adapter
        adapter = TraktCommentsAdapter(requireContext(), onItemClickListener)
        binding.recyclerViewComments.setHasFixedSize(true)
        binding.recyclerViewComments.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewComments.adapter = adapter

        // load data
        LoaderManager.getInstance(this)
            .initLoader(
                TraktCommentsActivity.LOADER_ID_COMMENTS, arguments,
                commentsLoaderCallbacks
            )

        // enable menu
        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun comment() {
        val binding = binding ?: return

        // prevent empty comments
        val comment = binding.textFieldComments.editText!!.text.toString()
        if (TextUtils.isEmpty(comment)) {
            return
        }

        // disable the comment button
        binding.buttonShouts.isEnabled = false
        val args = requireArguments()
        val isSpoiler = binding.checkBoxShouts.isChecked

        // as determined by "science", episode comments are most likely, so check for them first
        // comment for an episode?
        val episodeId = args.getLong(InitBundle.EPISODE_ID)
        if (episodeId != 0L) {
            TraktTask(context).commentEpisode(episodeId, comment, isSpoiler)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            return
        }

        // comment for a movie?
        val movieTmdbId = args.getInt(InitBundle.MOVIE_TMDB_ID)
        if (movieTmdbId != 0) {
            TraktTask(context).commentMovie(movieTmdbId, comment, isSpoiler)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            return
        }

        // comment for a show?
        val showId = args.getLong(InitBundle.SHOW_ID)
        if (showId != 0L) {
            TraktTask(context).commentShow(showId, comment, isSpoiler)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }

        // if all ids were 0, do nothing
        throw IllegalArgumentException("comment: failed, all IDs 0 ($args)")
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private val optionsMenuProvider: MenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.comments_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val itemId = menuItem.itemId
            if (itemId == R.id.menu_action_comments_refresh) {
                refreshCommentsWithNetworkCheck()
                return true
            }
            return false
        }
    }
    private val onItemClickListener =
        object : TraktCommentsAdapter.OnItemClickListener {
            override fun onOpenWebsite(commentId: Int) {
                WebTools.openAsCustomTab(requireContext(), TraktLink.comment(commentId))
            }
        }

    private val commentsLoaderCallbacks: LoaderManager.LoaderCallbacks<TraktCommentsLoader.Result> =
        object : LoaderManager.LoaderCallbacks<TraktCommentsLoader.Result> {
            override fun onCreateLoader(
                id: Int,
                args: Bundle?
            ): Loader<TraktCommentsLoader.Result> {
                showProgressBar(true)
                return TraktCommentsLoader(requireContext(), args!!)
            }

            override fun onLoadFinished(
                loader: Loader<TraktCommentsLoader.Result>,
                data: TraktCommentsLoader.Result
            ) {
                adapter.submitList(data.results)
                setEmptyMessage(data.emptyText)
                val hasNoData = data.results.isNullOrEmpty()
                binding?.recyclerViewComments?.isGone = hasNoData
                binding?.textViewShoutsEmpty?.isGone = !hasNoData
                showProgressBar(false)
            }

            override fun onLoaderReset(loader: Loader<TraktCommentsLoader.Result>) {
                // keep existing data
            }
        }

    private fun refreshCommentsWithNetworkCheck() {
        if (!AndroidUtils.isNetworkConnected(requireContext())) {
            // keep existing data, but update empty view anyhow
            showProgressBar(false)
            setEmptyMessage(getString(R.string.offline))
            Toast.makeText(requireContext(), R.string.offline, Toast.LENGTH_SHORT).show()
            return
        }
        refreshComments()
    }

    private fun refreshComments() {
        LoaderManager.getInstance(this)
            .restartLoader(
                TraktCommentsActivity.LOADER_ID_COMMENTS, arguments,
                commentsLoaderCallbacks
            )
    }

    /**
     * Changes the empty message.
     */
    private fun setEmptyMessage(stringResourceId: String) {
        binding?.textViewShoutsEmpty?.text = stringResourceId
    }

    /**
     * Show or hide the progress bar of the SwipeRefreshLayout
     * wrapping the comments list.
     */
    private fun showProgressBar(isShowing: Boolean) {
        binding?.swipeRefreshLayoutShouts?.isRefreshing = isShowing
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: TraktActionCompleteEvent) {
        if (event.traktAction != TraktAction.COMMENT || view == null) {
            return
        }

        // reenable the shout button
        val binding = binding ?: return
        binding.buttonShouts.isEnabled = true
        if (event.wasSuccessful) {
            // clear the text field and show recent shout
            binding.textFieldComments.editText!!.setText("")
            refreshCommentsWithNetworkCheck()
        }
    }

    companion object {
        const val liftOnScrollTargetViewId = R.id.listViewShouts
    }
}