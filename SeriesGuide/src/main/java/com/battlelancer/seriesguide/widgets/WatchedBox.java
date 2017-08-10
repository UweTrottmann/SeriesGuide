package com.battlelancer.seriesguide.widgets;

import android.content.Context;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.ViewTools;

/**
 * Image view that displays a watched, skipped or watch icon depending on the given episode flag.
 */
public class WatchedBox extends AppCompatImageView {

    private int mEpisodeFlag;
    private VectorDrawableCompat drawableWatch;

    public WatchedBox(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (isInEditMode()) {
            mEpisodeFlag = EpisodeFlags.UNWATCHED;
            updateStateImage();
        }
    }

    public int getEpisodeFlag() {
        return mEpisodeFlag;
    }

    /**
     * Set an {@link com.battlelancer.seriesguide.enums.EpisodeFlags} flag.
     */
    public void setEpisodeFlag(int episodeFlag) {
        EpisodeTools.validateFlags(episodeFlag);
        mEpisodeFlag = episodeFlag;
        updateStateImage();
    }

    private void updateStateImage() {
        switch (mEpisodeFlag) {
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
                if (drawableWatch == null) {
                    drawableWatch = ViewTools.vectorIconActive(getContext(),
                            getContext().getTheme(), R.drawable.ic_watch_black_24dp);
                }
                setImageDrawable(drawableWatch);
                break;
            }
        }
    }
}
