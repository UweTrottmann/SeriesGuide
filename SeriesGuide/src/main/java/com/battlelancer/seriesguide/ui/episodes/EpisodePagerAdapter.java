package com.battlelancer.seriesguide.ui.episodes;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import com.battlelancer.seriesguide.util.TextTools;
import java.util.ArrayList;

class EpisodePagerAdapter extends FragmentStatePagerAdapter {

    @NonNull private final ArrayList<Episode> episodes;
    private final Context context;
    private final boolean isMultiPane;

    EpisodePagerAdapter(Context context, FragmentManager fm,
            @NonNull ArrayList<Episode> episodes, boolean isMultiPane) {
        super(fm);
        this.context = context;
        this.episodes = episodes;
        this.isMultiPane = isMultiPane;
    }

    @Override
    public int getCount() {
        return episodes.size();
    }

    @Override
    public Fragment getItem(int position) {
        return EpisodeDetailsFragment.newInstance(episodes.get(position).episodeId, isMultiPane);
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        /*
         * This breaks the FragmentStatePagerAdapter (see
         * http://code.google.com/p/android/issues/detail?id=37990), so we
         * just destroy everything!
         */
        // EpisodeDetailsFragment fragment = (EpisodeDetailsFragment)
        // object;
        // int episodeId = fragment.getEpisodeId();
        // for (int i = 0; i < mEpisodes.size(); i++) {
        // if (episodeId == mEpisodes.get(i).episodeId) {
        // return i;
        // }
        // }
        return POSITION_NONE;
    }

    @Nullable
    Integer getItemEpisodeTvdbId(int position) {
        if (position < episodes.size()) {
            return episodes.get(position).episodeId;
        } else {
            return null;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Episode episode = episodes.get(position);
        return TextTools.getEpisodeNumber(context, episode.seasonNumber, episode.episodeNumber);
    }

    void updateEpisodeList(@NonNull ArrayList<Episode> list) {
        episodes.clear();
        episodes.addAll(list);
        notifyDataSetChanged();
    }
}
