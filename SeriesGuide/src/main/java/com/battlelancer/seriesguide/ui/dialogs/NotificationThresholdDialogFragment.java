package com.battlelancer.seriesguide.ui.dialogs;

import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.NotificationSettings;
import java.util.regex.Pattern;
import timber.log.Timber;

/**
 * Dialog which allows to select the number of minutes, hours or days when a notification should
 * appear before an episode is released.
 */
public class NotificationThresholdDialogFragment extends AppCompatDialogFragment {

    @BindView(R.id.buttonNegative) View buttonNegative;
    @BindView(R.id.buttonPositive) Button buttonPositive;
    @BindView(R.id.editTextThresholdValue) EditText editTextValue;
    @BindView(R.id.radioGroupThreshold) RadioGroup radioGroup;
    @BindView(R.id.radioButtonThresholdMinutes) RadioButton radioButtonMinutes;
    @BindView(R.id.radioButtonThresholdHours) RadioButton radioButtonHours;
    @BindView(R.id.radioButtonThresholdDays) RadioButton radioButtonDays;
    private Unbinder unbinder;

    private int value;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_notification_threshold, container, false);
        unbinder = ButterKnife.bind(this, view);

        buttonNegative.setVisibility(View.GONE);
        buttonPositive.setText(android.R.string.ok);
        buttonPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndDismiss();
            }
        });

        editTextValue.addTextChangedListener(textWatcher);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                // trigger text watcher, takes care of validating the value based on the new unit
                editTextValue.setText(editTextValue.getText());
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
        int minutes = NotificationSettings.getLatestToIncludeTreshold(getContext());

        int value;
        if (minutes % (24 * 60) == 0) {
            value = minutes / (24 * 60);
            radioGroup.check(R.id.radioButtonThresholdDays);
        } else if (minutes % 60 == 0) {
            value = minutes / 60;
            radioGroup.check(R.id.radioButtonThresholdHours);
        } else {
            value = minutes;
            radioGroup.check(R.id.radioButtonThresholdMinutes);
        }

        editTextValue.setText(String.valueOf(value));
        updateRadioButtons(value);
    }

    private void parseAndUpdateValue(Editable s) {
        int value = 0;
        try {
            value = Integer.parseInt(s.toString());
        } catch (NumberFormatException ignored) {
        }

        // do not allow values bigger than a threshold, based on the selected unit
        boolean resetValue = false;
        if (radioGroup.getCheckedRadioButtonId() == radioButtonMinutes.getId()
                && value > 600) {
            resetValue = true;
            value = 600;
        } else if (radioGroup.getCheckedRadioButtonId() == radioButtonHours.getId()
                && value > 120) {
            resetValue = true;
            value = 120;
        } else if (radioGroup.getCheckedRadioButtonId() == radioButtonDays.getId()
                && value > 7) {
            resetValue = true;
            value = 7;
        } else if (value < 0) {
            // should never happen due to text filter, but better safe than sorry
            resetValue = true;
            value = 0;
        }

        if (resetValue) {
            s.replace(0, s.length(), String.valueOf(value));
        }

        this.value = value;
        updateRadioButtons(value);
    }

    private void updateRadioButtons(int value) {
        Pattern placeholderPattern = Pattern.compile("%d\\s*");
        Resources res = getResources();
        radioButtonMinutes.setText(getQuantityStringWithoutPlaceholder(placeholderPattern, res,
                R.plurals.minutes_before_plural, value));
        radioButtonHours.setText(getQuantityStringWithoutPlaceholder(placeholderPattern, res,
                R.plurals.hours_before_plural, value));
        radioButtonDays.setText(getQuantityStringWithoutPlaceholder(placeholderPattern, res,
                R.plurals.days_before_plural, value));
    }

    private String getQuantityStringWithoutPlaceholder(Pattern pattern, Resources res,
            @PluralsRes int pluralsRes, int value) {
        return pattern.matcher(res.getQuantityString(pluralsRes, value)).replaceAll("");
    }

    private void saveAndDismiss() {
        int minutes = this.value;

        // if not already, convert to minutes
        if (radioGroup.getCheckedRadioButtonId() == radioButtonHours.getId()) {
            minutes *= 60;
        } else if (radioGroup.getCheckedRadioButtonId() == radioButtonDays.getId()) {
            minutes *= 60 * 24;
        }

        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                .putString(NotificationSettings.KEY_THRESHOLD, String.valueOf(minutes))
                .apply();
        Timber.i("Notification threshold set to %d minutes", minutes);

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
