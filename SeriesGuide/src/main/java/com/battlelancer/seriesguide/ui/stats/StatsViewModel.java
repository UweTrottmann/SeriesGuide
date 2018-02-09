package com.battlelancer.seriesguide.ui.stats;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;

@SuppressWarnings("WeakerAccess") // ViewModelProviders needs access
public class StatsViewModel extends AndroidViewModel {

    private final StatsLiveData data;

    public StatsViewModel(Application application) {
        super(application);
        data = new StatsLiveData(application);
    }

    StatsLiveData getStatsData() {
        return data;
    }
}
