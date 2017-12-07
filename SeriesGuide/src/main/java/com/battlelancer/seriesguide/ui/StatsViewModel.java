package com.battlelancer.seriesguide.ui;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;

public class StatsViewModel extends AndroidViewModel {

    private final StatsLiveData data;

    public StatsViewModel(Application application) {
        super(application);
        data = new StatsLiveData(application);
    }

    public StatsLiveData getStatsData() {
        return data;
    }
}
