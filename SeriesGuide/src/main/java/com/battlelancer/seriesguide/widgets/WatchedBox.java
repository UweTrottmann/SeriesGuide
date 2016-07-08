package com.battlelancer.seriesguide.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.Utils;

public class WatchedBox extends ImageView {

    private int mEpisodeFlag;

    private final int mResIdWatchDrawable;
    private final int mResIdWatchedDrawable;
    private final int mResIdWatchSkippedDrawable;

    public WatchedBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        mEpisodeFlag = EpisodeFlags.UNWATCHED;
        mResIdWatchDrawable = Utils.resolveAttributeToResourceId(context.getTheme(),
                R.attr.drawableWatch);
        mResIdWatchedDrawable = Utils.resolveAttributeToResourceId(context.getTheme(),
                R.attr.drawableWatched);
        mResIdWatchSkippedDrawable = Utils.resolveAttributeToResourceId(context.getTheme(),
                R.attr.drawableWatchSkipped);
        updateStateImage();
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
                setImageResource(mResIdWatchedDrawable);
                break;
            }
            case EpisodeFlags.SKIPPED: {
                setImageResource(mResIdWatchSkippedDrawable);
                break;
            }
            case EpisodeFlags.UNWATCHED:
            default: {
                setImageResource(mResIdWatchDrawable);
                break;
            }
        }
    }
}
