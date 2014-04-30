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

package com.battlelancer.seriesguide.adapters.model;

/**
 * Stores information about a header, used by
 * {@link com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter}
 * adapters.
 */
public class HeaderData {

    private int mCount;

    private int mRefPosition;

    public HeaderData(int refPosition) {
        mRefPosition = refPosition;
        mCount = 0;
    }

    public int getCount() {
        return mCount;
    }

    public int getRefPosition() {
        return mRefPosition;
    }

    public void incrementCount() {
        mCount++;
    }
}