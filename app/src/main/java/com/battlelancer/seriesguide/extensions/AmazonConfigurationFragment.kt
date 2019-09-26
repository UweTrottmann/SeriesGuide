package com.battlelancer.seriesguide.extensions

import android.os.Bundle
import android.preference.ListPreference
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.AmazonSettings

class AmazonConfigurationFragment : BaseSettingsFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use the Amazon extension settings file.
        preferenceManager.sharedPreferencesName = AmazonSettings.SETTINGS_FILE
        preferenceManager.sharedPreferencesMode = 0

        // Create a preference screen.
        val preferenceScreen = preferenceManager.createPreferenceScreen(
            activity
        )

        val countryPreference = ListPreference(activity).apply {
            key = AmazonSettings.KEY_COUNTRY
            setTitle(R.string.pref_amazon_domain)
            setEntries(R.array.amazonDomainsData)
            setEntryValues(R.array.amazonDomainsData)
            setDefaultValue(AmazonSettings.DEFAULT_DOMAIN)
            positiveButtonText = null
            negativeButtonText = null
        }
        preferenceScreen.addPreference(countryPreference)

        setPreferenceScreen(preferenceScreen)

        bindPreferenceSummaryToValue(
            preferenceManager.sharedPreferences,
            countryPreference
        )
    }
}
