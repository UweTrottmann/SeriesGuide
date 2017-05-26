package com.battlelancer.seriesguide.ui.dialogs;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.CursorRecyclerViewAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import org.greenrobot.eventbus.EventBus;

/**
 * A dialog displaying a list of shows with switches to turn notifications on or off.
 */
public class NotificationSelectionDialogFragment extends AppCompatDialogFragment {

    private static final int LOADER_ID_SELECTION = 1;

    @BindView(R.id.textViewSelectionEmpty) TextView textViewEmpty;
    @BindView(R.id.recyclerViewSelection) RecyclerView recyclerView;
    private Unbinder unbinder;

    private SelectionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_notification_selection, container, false);
        unbinder = ButterKnife.bind(this, view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SelectionAdapter(onItemClickListener);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(LOADER_ID_SELECTION, null, showsLoaderCallbacks);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        EventBus.getDefault().post(new SeriesGuidePreferences.UpdateSummariesEvent());
    }

    private LoaderManager.LoaderCallbacks<Cursor> showsLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getContext(), SeriesGuideContract.Shows.CONTENT_URI,
                    new String[] {
                            SeriesGuideContract.Shows._ID, // 0
                            SeriesGuideContract.Shows.TITLE, // 1
                            SeriesGuideContract.Shows.NOTIFY // 2
                    }, null, null,
                    DisplaySettings.isSortOrderIgnoringArticles(getContext())
                            ? SeriesGuideContract.Shows.SORT_TITLE_NOARTICLE
                            : SeriesGuideContract.Shows.SORT_TITLE);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            boolean hasNoData = data == null || data.getCount() == 0;
            textViewEmpty.setVisibility(hasNoData ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(hasNoData ? View.GONE : View.VISIBLE);
            adapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            adapter.swapCursor(null);
        }
    };

    private SelectionAdapter.OnItemClickListener onItemClickListener
            = new SelectionAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int showTvdbId, boolean notify) {
            SgApp.getServicesComponent(getContext()).showTools().storeNotify(showTvdbId, notify);
        }
    };

    public static class SelectionAdapter extends
            CursorRecyclerViewAdapter<RecyclerView.ViewHolder> {

        public interface OnItemClickListener {
            void onItemClick(int showTvdbId, boolean notify);
        }

        @NonNull private final OnItemClickListener onItemClickListener;

        public SelectionAdapter(@NonNull OnItemClickListener onItemClickListener) {
            super(false);
            this.onItemClickListener = onItemClickListener;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification_selection, parent, false);
            return new ViewHolder(view, onItemClickListener);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, Cursor cursor) {
            if (holder instanceof ViewHolder) {
                ViewHolder actualHolder = (ViewHolder) holder;
                actualHolder.showTvdbId = cursor.getInt(0);
                actualHolder.switchCompat.setText(cursor.getString(1));
                actualHolder.switchCompat.setChecked(cursor.getInt(2) == 1);
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.switchItemSelection) SwitchCompat switchCompat;
            int showTvdbId;

            public ViewHolder(View itemView, final OnItemClickListener onItemClickListener) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onItemClickListener.onItemClick(showTvdbId, switchCompat.isChecked());
                    }
                });
            }
        }
    }
}
