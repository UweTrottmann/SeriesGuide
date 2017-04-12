package com.battlelancer.seriesguide.ui.dialogs;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import timber.log.Timber;

/**
 * Dialog which allows to select the number of hours (between +/-24) to offset show release times
 * by.
 */
public class TimeOffsetDialogFragment extends AppCompatDialogFragment {

    @BindView(R.id.buttonNegative) View buttonNegative;
    @BindView(R.id.buttonPositive) Button buttonPositive;
    @BindView(R.id.editTextOffsetValue) EditText editTextValue;
    @BindView(R.id.textViewOffsetRange) TextView textViewRange;
    @BindView(R.id.textViewOffsetSummary) TextView textViewSummary;
    @BindView(R.id.textViewOffsetExample) TextView textViewExample;
    private Unbinder unbinder;

    private int hours;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_time_offset, container, false);
        unbinder = ButterKnife.bind(this, view);

        buttonNegative.setVisibility(View.GONE);
        buttonPositive.setText(android.R.string.ok);
        buttonPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndDismiss();
            }
        });

        textViewRange.setText(getString(R.string.format_time_offset_range, -24, 24));

        editTextValue.addTextChangedListener(textWatcher);
        editTextValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editTextValue.hasSelection()) {
                    editTextValue.selectAll();
                }
            }
        });

        bindViews();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void bindViews() {
        int hours = DisplaySettings.getShowsTimeOffset(getContext());

        editTextValue.setText(String.valueOf(hours));
        // text views are updated by text watcher
    }

    private void parseAndUpdateValue(Editable s) {
        int value = 0;
        try {
            value = Integer.parseInt(s.toString());
        } catch (NumberFormatException ignored) {
        }

        // do only allow values between +/-24
        boolean resetValue = false;
        if (value < -24) {
            resetValue = true;
            value = -24;
        } else if (value > 24) {
            resetValue = true;
            value = 24;
        }

        if (resetValue) {
            s.replace(0, s.length(), String.valueOf(value));
        }

        this.hours = value;
        updateSummaryAndExample(value);
    }

    private void updateSummaryAndExample(int value) {
        textViewSummary.setText(getString(R.string.pref_offsetsummary, value));

        LocalDateTime original = LocalDateTime.of(LocalDate.now(), LocalTime.of(20, 0));
        LocalDateTime offset = original.plusHours(value);

        textViewExample.setText(formatToTimeString(original) + " -> " + formatToTimeString(offset));
    }

    private CharSequence formatToTimeString(LocalDateTime localDateTime) {
        return DateUtils.getRelativeDateTimeString(getContext(),
                localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
    }

    private void saveAndDismiss() {
        int hours = this.hours;

        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                .putString(DisplaySettings.KEY_SHOWS_TIME_OFFSET, String.valueOf(hours))
                .apply();
        Timber.i("Time offset set to %d hours", hours);

        dismiss();
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            parseAndUpdateValue(s);
        }
    };
}
