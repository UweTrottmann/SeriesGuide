package com.battlelancer.seriesguide.ui.dialogs;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
        public final List<LocalizationAdapter.LocalizationItem> items;
        public final int type;

        public ItemsLoadedEvent(
                List<LocalizationAdapter.LocalizationItem> items, int type) {
            this.items = items;
            this.type = type;
        }
    }

    public static void show(FragmentManager fragmentManager) {
        MovieLocalizationDialogFragment dialog = new MovieLocalizationDialogFragment();
        dialog.show(fragmentManager, "dialog-language");
    }

    private Unbinder unbinder;
    @BindView(R.id.buttonPositive) Button buttonOk;
    @BindView(R.id.recyclerViewLocalization) RecyclerView recyclerView;
    @BindView(R.id.textViewLocalizationLanguage) TextView textViewLanguage;
    @BindView(R.id.textViewLocalizationRegion) TextView textViewRegion;
    @BindView(R.id.buttonLocalizationLanguage) Button buttonLanguage;
    @BindView(R.id.buttonLocalizationRegion) Button buttonRegion;

    private LocalizationAdapter adapter;
    int type;
    boolean listVisible;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.dialog_localization, container, false);
        unbinder = ButterKnife.bind(this, view);

        buttonOk.setText(android.R.string.ok);
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        adapter = new LocalizationAdapter(onItemClickListener);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        updateButtonText();

        buttonLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setListVisible(true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String[] languageCodes = getContext().getResources()
                                .getStringArray(R.array.languageCodesMovies);
                        List<LocalizationAdapter.LocalizationItem> items = new ArrayList<>(
                                languageCodes.length);
                        for (String languageCode : languageCodes) {
                            // example: "en-US"
                            String languageDisplayName = new Locale(languageCode.substring(0, 2),
                                    languageCode.substring(3)).getDisplayName();
                            items.add(new LocalizationAdapter.LocalizationItem(languageCode,
                                    languageDisplayName));
                        }
                        final Collator collator = Collator.getInstance();
                        Collections.sort(items,
                                new Comparator<LocalizationAdapter.LocalizationItem>() {
                                    @Override
                                    public int compare(LocalizationAdapter.LocalizationItem left,
                                            LocalizationAdapter.LocalizationItem right) {
                                        return collator.compare(left.displayText,
                                                right.displayText);
                                    }
                                });
                        EventBus.getDefault().postSticky(new ItemsLoadedEvent(items, 0));
                    }
                }).run();
            }
        });
        buttonRegion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setListVisible(true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
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
                                new Comparator<LocalizationAdapter.LocalizationItem>() {
                                    @Override
                                    public int compare(LocalizationAdapter.LocalizationItem left,
                                            LocalizationAdapter.LocalizationItem right) {
                                        return collator.compare(left.displayText,
                                                right.displayText);
                                    }
                                });
                        EventBus.getDefault().postSticky(new ItemsLoadedEvent(items, 1));
                    }
                }).run();
            }
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
        String languageDisplayName = new Locale(languageCode.substring(0, 2),
                languageCode.substring(3)).getDisplayName();
        buttonLanguage.setText(languageDisplayName);
        String regionCode = DisplaySettings.getMoviesRegion(getContext());
        buttonRegion.setText(new Locale("", regionCode).getDisplayCountry());
    }

    public void setListVisible(boolean visible) {
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

    public static class LocalizationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static class LocalizationItem {
            public final String code;
            public final String displayText;

            public LocalizationItem(String code, String displayText) {
                this.code = code;
                this.displayText = displayText;
            }
        }

        public interface OnItemClickListener {
            void onItemClick(String code);
        }

        @NonNull private final List<LocalizationItem> items;
        @NonNull private final OnItemClickListener onItemClickListener;

        public LocalizationAdapter(@NonNull OnItemClickListener onItemClickListener) {
            this.items = new ArrayList<>();
            this.onItemClickListener = onItemClickListener;
        }

        public void updateItems(@NonNull List<LocalizationItem> items) {
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
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onItemClickListener.onItemClick(code);
                    }
                });
            }
        }
    }
}
