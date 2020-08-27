package com.battlelancer.seriesguide.ui.dialogs;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.widget.SwitchCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.CursorRecyclerViewAdapter;
import com.battlelancer.seriesguide.databinding.DialogNotificationSelectionBinding;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import org.greenrobot.eventbus.EventBus;

/**
 * A dialog displaying a list of shows with switches to turn notifications on or off.
 */
public class NotificationSelectionDialogFragment extends AppCompatDialogFragment {

    private static final int LOADER_ID_SELECTION = 1;

    private DialogNotificationSelectionBinding binding;

    private SelectionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogNotificationSelectionBinding.inflate(inflater, container, false);

        binding.recyclerViewSelection.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SelectionAdapter(onItemClickListener);
        binding.recyclerViewSelection.setAdapter(adapter);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LoaderManager.getInstance(this)
                .initLoader(LOADER_ID_SELECTION, null, showsLoaderCallbacks);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        EventBus.getDefault().post(new SeriesGuidePreferences.UpdateSummariesEvent());
    }

    private LoaderManager.LoaderCallbacks<Cursor> showsLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(requireContext(), SeriesGuideContract.Shows.CONTENT_URI,
                    new String[]{
                            SeriesGuideContract.Shows._ID, // 0
                            SeriesGuideContract.Shows.TITLE, // 1
                            SeriesGuideContract.Shows.NOTIFY // 2
                    }, null, null,
                    DisplaySettings.isSortOrderIgnoringArticles(getContext())
                            ? SeriesGuideContract.Shows.SORT_TITLE_NOARTICLE
                            : SeriesGuideContract.Shows.SORT_TITLE);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            boolean hasNoData = data == null || data.getCount() == 0;
            binding.textViewSelectionEmpty.setVisibility(hasNoData ? View.VISIBLE : View.GONE);
            binding.recyclerViewSelection.setVisibility(hasNoData ? View.GONE : View.VISIBLE);
            adapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            adapter.swapCursor(null);
        }
    };

    private final SelectionAdapter.OnItemClickListener onItemClickListener
            = (showTvdbId, notify) -> SgApp.getServicesComponent(requireContext()).showTools()
            .storeNotify(showTvdbId, notify);

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

        @NonNull
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

            private final SwitchCompat switchCompat;
            int showTvdbId;

            public ViewHolder(View itemView, final OnItemClickListener onItemClickListener) {
                super(itemView);
                switchCompat = itemView.findViewById(R.id.switchItemSelection);
                itemView.setOnClickListener(
                        v -> onItemClickListener.onItemClick(showTvdbId, switchCompat.isChecked()));
            }
        }
    }
}
