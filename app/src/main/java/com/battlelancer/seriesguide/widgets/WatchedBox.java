package com.battlelancer.seriesguide.widgets;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;

/**
 * Image view that displays a watched, skipped or watch icon depending on the given episode flag.
 */
public class WatchedBox extends AppCompatImageView {

    private int episodeFlag;

    public WatchedBox(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (isInEditMode()) {
            episodeFlag = EpisodeFlags.UNWATCHED;
            updateStateImage();
        }
    }

    public int getEpisodeFlag() {
        return episodeFlag;
    }

    /**
     * Set an {@link EpisodeFlags} flag.
     */
    public void setEpisodeFlag(int episodeFlag) {
        EpisodeTools.validateFlags(episodeFlag);
        this.episodeFlag = episodeFlag;
        updateStateImage();
    }

    private void updateStateImage() {
        switch (episodeFlag) {
            case EpisodeFlags.WATCHED: {
                setImageResource(R.drawable.ic_watched_24dp);
                break;
            }
            case EpisodeFlags.SKIPPED: {
                setImageResource(R.drawable.ic_skipped_24dp);
                break;
            }
            case EpisodeFlags.UNWATCHED:
            default: {
                setImageResource(R.drawable.ic_watch_black_24dp);
                break;
            }
        }
    }
}
