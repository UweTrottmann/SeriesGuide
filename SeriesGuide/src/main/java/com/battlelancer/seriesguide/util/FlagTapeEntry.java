
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
