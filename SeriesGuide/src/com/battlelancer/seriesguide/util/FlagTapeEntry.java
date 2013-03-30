
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.util.FlagTask.FlagAction;

import java.io.Serializable;
import java.util.List;

/**
 * Stores everything needed for {@link FlagTapedTask} to execute a trakt action.
 * Is taped onto disk with {@link FlagTapeEntryQueue}.
 */
public class FlagTapeEntry implements Serializable {
    private static final long serialVersionUID = 1659483526310123582L;

    public static class Flag {
        int season;
        int episode;

        public Flag(int season, int episode) {
            this.season = season;
            this.episode = episode;
        }
    }

    public FlagTapeEntry(FlagAction action, int showId, List<FlagTapeEntry.Flag> flags,
            boolean isFlag) {
        this.action = action;
        this.showId = showId;
        this.flags = flags;
        this.isFlag = isFlag;
    }

    public FlagAction action;
    public int showId;
    public List<FlagTapeEntry.Flag> flags;
    public boolean isFlag;
}
