package com.battlelancer.seriesguide.extensions;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.AmazonSettings;
import com.battlelancer.seriesguide.ui.BaseSettingsFragment;

public class AmazonConfigurationFragment extends BaseSettingsFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // use the Amazon extension settings file
        getPreferenceManager().setSharedPreferencesName(
                AmazonSettings.SETTINGS_FILE);
        getPreferenceManager().setSharedPreferencesMode(0);

        // create a preference screen
        PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                getActivity());

        ListPreference countryPreference = new ListPreference(getActivity());
        countryPreference.setKey(AmazonSettings.KEY_COUNTRY);
        countryPreference.setTitle(R.string.pref_amazon_domain);
        countryPreference.setEntries(R.array.amazonDomainsData);
        countryPreference.setEntryValues(R.array.amazonDomainsData);
        countryPreference.setDefaultValue(AmazonSettings.DEFAULT_DOMAIN);
        countryPreference.setPositiveButtonText(null);
        countryPreference.setNegativeButtonText(null);
        preferenceScreen.addPreference(countryPreference);

        setPreferenceScreen(preferenceScreen);

        bindPreferenceSummaryToValue(getPreferenceManager().getSharedPreferences(),
                countryPreference);
    }
}
