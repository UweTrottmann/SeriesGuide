package com.battlelancer.seriesguide.ui.movies;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.DialogTools;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A dialog displaying a list of languages and regions to choose from, posting a {@link
 * LocalizationChangedEvent} if a language or region different from the given ones was chosen.
 */
public class MovieLocalizationDialogFragment extends AppCompatDialogFragment {

    private static final String STATE_LIST_VISIBLE = "listVisible";

    public static class LocalizationChangedEvent {
    }

    public static class ItemsLoadedEvent {
        final List<LocalizationAdapter.LocalizationItem> items;
        final int type;

        ItemsLoadedEvent(List<LocalizationAdapter.LocalizationItem> items, int type) {
            this.items = items;
            this.type = type;
        }
    }

    static void show(FragmentManager fragmentManager) {
        DialogTools.safeShow(new MovieLocalizationDialogFragment(), fragmentManager,
                "movieLanguageDialog");
    }

    private Unbinder unbinder;
    @BindView(R.id.buttonPositive) Button buttonOk;
    @BindView(R.id.recyclerViewLocalization) RecyclerView recyclerView;
    @BindView(R.id.textViewLocalizationLanguage) TextView textViewLanguage;
    @BindView(R.id.textViewLocalizationRegion) TextView textViewRegion;
    @BindView(R.id.buttonLocalizationLanguage) Button buttonLanguage;
    @BindView(R.id.buttonLocalizationRegion) Button buttonRegion;

    private LocalizationAdapter adapter;
    private int type;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.dialog_localization, container, false);
        unbinder = ButterKnife.bind(this, view);

        buttonOk.setText(android.R.string.ok);
        buttonOk.setOnClickListener(v -> dismiss());

        adapter = new LocalizationAdapter(onItemClickListener);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        updateButtonText();

        buttonLanguage.setOnClickListener(v -> {
            setListVisible(true);
            new Thread(() -> {
                String[] languageCodes = getContext().getResources()
                        .getStringArray(R.array.languageCodesMovies);
                List<LocalizationAdapter.LocalizationItem> items = new ArrayList<>(
                        languageCodes.length);
                for (String languageCode : languageCodes) {
                    // example: "en-US"
                    String languageDisplayName = new Locale(languageCode.substring(0, 2),
                            "").getDisplayName();
                    items.add(new LocalizationAdapter.LocalizationItem(languageCode,
                            languageDisplayName));
                }
                final Collator collator = Collator.getInstance();
                Collections.sort(items,
                        (left, right) -> collator.compare(left.displayText, right.displayText));
                EventBus.getDefault().postSticky(new ItemsLoadedEvent(items, 0));
            }).run();
        });
        buttonRegion.setOnClickListener(v -> {
            setListVisible(true);
            new Thread(() -> {
                String[] regionCodes = Locale.getISOCountries();
                List<LocalizationAdapter.LocalizationItem> items = new ArrayList<>(
                        regionCodes.length);
                for (String regionCode : regionCodes) {
                    // example: "en-US"
                    String displayCountry = new Locale("", regionCode).getDisplayCountry();
                    items.add(new LocalizationAdapter.LocalizationItem(regionCode,
                            displayCountry));
                }
                final Collator collator = Collator.getInstance();
                Collections.sort(items,
                        (left, right) -> collator.compare(left.displayText, right.displayText));
                EventBus.getDefault().postSticky(new ItemsLoadedEvent(items, 1));
            }).run();
        });

        return view;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null) {
            setListVisible(false);
        } else {
            setListVisible(savedInstanceState.getBoolean(STATE_LIST_VISIBLE, false));
        }
        ItemsLoadedEvent loadedEvent = EventBus.getDefault().getStickyEvent(ItemsLoadedEvent.class);
        if (loadedEvent != null) {
            adapter.updateItems(loadedEvent.items);
            type = loadedEvent.type;
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_LIST_VISIBLE, recyclerView.getVisibility() == View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        EventBus.getDefault().post(new LocalizationChangedEvent());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventItemsLoaded(ItemsLoadedEvent event) {
        recyclerView.scrollToPosition(0);
        type = event.type;
        adapter.updateItems(event.items);
    }

    private void updateButtonText() {
        // example: "en-US"
        String languageCode = DisplaySettings.getMoviesLanguage(getContext());
        String languageDisplayName = new Locale(languageCode.substring(0, 2), "").getDisplayName();
        buttonLanguage.setText(languageDisplayName);
        String regionCode = DisplaySettings.getMoviesRegion(getContext());
        buttonRegion.setText(new Locale("", regionCode).getDisplayCountry());
    }

    private void setListVisible(boolean visible) {
        recyclerView.setVisibility(visible ? View.VISIBLE : View.GONE);
        int visibility = visible ? View.GONE : View.VISIBLE;
        buttonLanguage.setVisibility(visibility);
        textViewLanguage.setVisibility(visibility);
        buttonRegion.setVisibility(visibility);
        textViewRegion.setVisibility(visibility);
    }

    private LocalizationAdapter.OnItemClickListener onItemClickListener
            = new LocalizationAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(String code) {
            setListVisible(false);
            if (type == 0) {
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                        .putString(DisplaySettings.KEY_MOVIES_LANGUAGE, code)
                        .apply();
            } else if (type == 1) {
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                        .putString(DisplaySettings.KEY_MOVIES_REGION, code)
                        .apply();
            }
            updateButtonText();
        }
    };

    static class LocalizationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        static class LocalizationItem {
            final String code;
            final String displayText;

            LocalizationItem(String code, String displayText) {
                this.code = code;
                this.displayText = displayText;
            }
        }

        interface OnItemClickListener {
            void onItemClick(String code);
        }

        @NonNull private final List<LocalizationItem> items;
        @NonNull private final OnItemClickListener onItemClickListener;

        LocalizationAdapter(@NonNull OnItemClickListener onItemClickListener) {
            this.items = new ArrayList<>();
            this.onItemClickListener = onItemClickListener;
        }

        void updateItems(@NonNull List<LocalizationItem> items) {
            this.items.clear();
            this.items.addAll(items);
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dropdown, parent, false);
            return new ViewHolder(view, onItemClickListener);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ViewHolder) {
                ViewHolder actualHolder = (ViewHolder) holder;
                LocalizationItem item = items.get(position);
                actualHolder.code = item.code;
                actualHolder.title.setText(item.displayText);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            @BindView(android.R.id.text1) TextView title;
            String code;

            public ViewHolder(View itemView, final OnItemClickListener onItemClickListener) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                itemView.setOnClickListener(v -> onItemClickListener.onItemClick(code));
            }
        }
    }
}
