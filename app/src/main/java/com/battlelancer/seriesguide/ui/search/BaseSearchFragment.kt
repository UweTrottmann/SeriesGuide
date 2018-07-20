package com.battlelancer.seriesguide.ui.search

import android.app.SearchManager
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.GridView
import butterknife.BindView
import butterknife.ButterKnife
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.SearchActivity
import com.uwetrottmann.androidutils.AndroidUtils
import org.greenrobot.eventbus.EventBus

abstract class BaseSearchFragment : Fragment(), AdapterView.OnItemClickListener {

    @BindView(R.id.textViewSearchEmpty)
    lateinit var emptyView: View
    @BindView(R.id.gridViewSearch)
    lateinit var gridView: GridView

    var loaderArgs: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            // restore last query
            loaderArgs = savedInstanceState.getBundle(STATE_LOADER_ARGS)
        } else {
            // use initial query (if any)
            val queryEvent = EventBus.getDefault()
                    .getStickyEvent(SearchActivity.SearchQueryEvent::class.java)
            if (queryEvent != null) {
                loaderArgs = queryEvent.args
            }
        }
        if (loaderArgs == null) {
            loaderArgs = Bundle()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ButterKnife.bind(this, view)

        // enable app bar scrolling out of view only on L or higher
        ViewCompat.setNestedScrollingEnabled(gridView, AndroidUtils.isLollipopOrHigher())
        gridView.onItemClickListener = this

        emptyView.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()

        EventBus.getDefault().unregister(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // loader args are not saved if fragment is killed, so do it manually
        outState.putBundle(STATE_LOADER_ARGS, loaderArgs)
    }

    protected fun updateEmptyState(data: Cursor) {
        val query = loaderArgs?.getString(SearchManager.QUERY)
        if (data.count == 0 && !(query.isNullOrEmpty())) {
            emptyView.visibility = View.VISIBLE
            gridView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            gridView.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val STATE_LOADER_ARGS = "loaderArgs"
    }

}
