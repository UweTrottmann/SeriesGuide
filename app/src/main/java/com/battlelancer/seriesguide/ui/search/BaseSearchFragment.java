package com.battlelancer.seriesguide.ui.search;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.SearchActivity;
import com.uwetrottmann.androidutils.AndroidUtils;
import org.greenrobot.eventbus.EventBus;

public abstract class BaseSearchFragment extends Fragment
        implements AdapterView.OnItemClickListener {

    private static final String STATE_LOADER_ARGS = "loaderArgs";

    @BindView(R.id.textViewSearchEmpty) TextView textViewEmpty;
    @BindView(R.id.gridViewSearch) GridView gridView;

    protected Bundle loaderArgs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            // restore last query
            loaderArgs = savedInstanceState.getBundle(STATE_LOADER_ARGS);
        } else {
            // use initial query (if any)
            SearchActivity.SearchQueryEvent queryEvent = EventBus.getDefault()
                    .getStickyEvent(SearchActivity.SearchQueryEvent.class);
            if (queryEvent != null) {
                loaderArgs = queryEvent.args;
            }
        }
        if (loaderArgs == null) {
            loaderArgs = new Bundle();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, view);

        // enable app bar scrolling out of view only on L or higher
        ViewCompat.setNestedScrollingEnabled(gridView, AndroidUtils.isLollipopOrHigher());
        gridView.setOnItemClickListener(this);
        gridView.setEmptyView(textViewEmpty);

        return view;
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // loader args are not saved if fragment is killed, so do it manually
        outState.putBundle(STATE_LOADER_ARGS, loaderArgs);
    }

}
