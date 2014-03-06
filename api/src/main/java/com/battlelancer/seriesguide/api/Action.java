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

package com.battlelancer.seriesguide.api;

import android.content.Intent;

public class Action {

    private String mTitle;
    private Intent mViewIntent;

    private Action(String title, Intent viewIntent) {
        mTitle = title;
        mViewIntent = viewIntent;
    }

    /**
     * Returns the user-visible title for this action.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the activity {@link Intent} that will be started when the user clicks on the button
     * for this action.
     */
    public Intent getViewIntent() {
        return mViewIntent;
    }

    public static class Builder {

        private String mTitle;
        private Intent mViewIntent;

        /**
         * Sets the user-visible title of the button for this action, returns a builder
         * instance to add further properties.
         */
        public Builder(String title) {
            if (title == null || title.length() == 0) {
                throw new IllegalArgumentException("Title may not be null or empty.");
            }
            mTitle = title;
        }

        /**
         * Sets the (optional) activity {@link Intent} that will be {@linkplain
         * android.content.Context#startActivity(Intent)
         * started} when the user clicks the button for the action.<br/>
         * <br/>
         * The activity that this intent resolves to must have <code>android:exported</code> set
         * to <code>true</code>.
         */
        public Builder viewIntent(Intent viewIntent) {
            mViewIntent = viewIntent;
            return this;
        }

        public Action build() {
            return new Action(mTitle, mViewIntent);
        }
    }
}
