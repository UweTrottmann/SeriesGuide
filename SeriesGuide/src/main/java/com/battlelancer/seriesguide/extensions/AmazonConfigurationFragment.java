/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
