package com.battlelancer.seriesguide.ui.people

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ListView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.widgets.EmptyView
import com.uwetrottmann.androidutils.AndroidUtils

/**
 * A fragment loading and showing a list of cast or crew members.
 */
class PeopleFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var emptyView: EmptyView
    private lateinit var adapter: PeopleAdapter
    private lateinit var progressBar: ProgressBar

    private lateinit var mediaType: PeopleActivity.MediaType
    private lateinit var peopleType: PeopleActivity.PeopleType
    private var tmdbId: Int = 0
    private var onShowPersonListener = sDummyListener
    private var activateOnItemClick: Boolean = false
    /** The current activated item position. Only used on tablets.  */
    private var activatedPosition = ListView.INVALID_POSITION

    private val model by viewModels<PeopleViewModel> {
        PeopleViewModelFactory(requireActivity().application, tmdbId, mediaType)
    }

    internal interface OnShowPersonListener {
        fun showPerson(tmdbId: Int)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaType = PeopleActivity.MediaType.valueOf(
                arguments?.getString(PeopleActivity.InitBundle.MEDIA_TYPE)
                        ?: throw IllegalArgumentException(
                                "Missing arg ${PeopleActivity.InitBundle.MEDIA_TYPE}"))
        peopleType = PeopleActivity.PeopleType.valueOf(
                arguments?.getString(PeopleActivity.InitBundle.PEOPLE_TYPE)
                        ?: throw IllegalArgumentException(
                                "Missing arg ${PeopleActivity.InitBundle.PEOPLE_TYPE}"))
        tmdbId = arguments?.getInt(PeopleActivity.InitBundle.ITEM_TMDB_ID)
                ?: throw IllegalArgumentException(
                "Missing arg ${PeopleActivity.InitBundle.ITEM_TMDB_ID}")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_people, container, false)

        listView = rootView.findViewById(R.id.listViewPeople)
        emptyView = rootView.findViewById(R.id.emptyViewPeople)
        progressBar = rootView.findViewById(R.id.progressBarPeople)

        emptyView.setContentVisibility(View.GONE)
        listView.emptyView = emptyView

        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        listView.choiceMode = if (activateOnItemClick) {
            ListView.CHOICE_MODE_SINGLE
        } else {
            ListView.CHOICE_MODE_NONE
        }


        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION))
        }

        adapter = PeopleAdapter(context)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            adapter.getItem(position)?.let {
                onShowPersonListener.showPerson(it.tmdbId)
            }
        }

        emptyView.setButtonClickListener { refresh() }

        model.credits.observe(viewLifecycleOwner, Observer {
            setProgressVisibility(false)
            setEmptyMessage()

            if (it == null) {
                adapter.setData(null)
                return@Observer
            }
            if (peopleType == PeopleActivity.PeopleType.CAST) {
                adapter.setData(PeopleListHelper.transformCastToPersonList(it.cast))
            } else {
                adapter.setData(PeopleListHelper.transformCrewToPersonList(it.crew))
            }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            onShowPersonListener = context as OnShowPersonListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnShowPersonListener")
        }

    }

    override fun onDetach() {
        super.onDetach()

        onShowPersonListener = sDummyListener
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (activatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition)
        }
    }

    private fun refresh() {
        model.loadCredits()
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be given the
     * 'activated' state when touched.
     */
    fun setActivateOnItemClick() {
        activateOnItemClick = true
    }

    private fun setActivatedPosition(position: Int) {
        if (position == ListView.INVALID_POSITION) {
            listView.setItemChecked(activatedPosition, false)
        } else {
            listView.setItemChecked(position, true)
        }

        activatedPosition = position
    }

    /**
     * Shows or hides a custom indeterminate progress indicator inside this activity layout.
     */
    private fun setProgressVisibility(isVisible: Boolean) {
        if (progressBar.visibility == (if (isVisible) View.VISIBLE else View.GONE)) {
            // already in desired state, avoid replaying animation
            return
        }
        progressBar.startAnimation(AnimationUtils.loadAnimation(progressBar.context,
                if (isVisible) R.anim.fade_in else R.anim.fade_out))
        progressBar.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun setEmptyMessage() {
        // display error message if we are offline
        if (!AndroidUtils.isNetworkConnected(requireContext())) {
            emptyView.setMessage(R.string.offline)
        } else {
            emptyView.setMessage(R.string.people_empty)
        }
        emptyView.setContentVisibility(View.VISIBLE)
    }

    companion object {

        /**
         * The serialization (saved instance state) Bundle key representing the activated item position.
         * Only used on tablets.
         */
        private const val STATE_ACTIVATED_POSITION = "activated_position"

        private val sDummyListener: OnShowPersonListener = object : OnShowPersonListener {
            override fun showPerson(tmdbId: Int) {}
        }
    }
}
