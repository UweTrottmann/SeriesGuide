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

package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class WatchedBox extends ImageView {

    private int mEpisodeFlag;

    private final int mResIdWatchDrawable;

    private int mResIdWatchSkippedDrawable;

    public WatchedBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        mEpisodeFlag = EpisodeFlags.UNWATCHED;
        mResIdWatchDrawable = Utils.resolveAttributeToResourceId(context.getTheme(),
                R.attr.drawableWatch);
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

    /**
     * If box was in watched state, sets to unwatched. Otherwise sets to watched.
     */
    public void toggleWatched() {
        mEpisodeFlag = EpisodeTools.isWatched(mEpisodeFlag)
                ? EpisodeFlags.UNWATCHED : EpisodeFlags.WATCHED;
        updateStateImage();
    }

    private void updateStateImage() {
        switch (mEpisodeFlag) {
            case EpisodeFlags.WATCHED: {
                setImageResource(R.drawable.ic_ticked);
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
