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
import android.os.Bundle;
import android.text.TextUtils;
import java.net.URISyntaxException;
import org.json.JSONException;
import org.json.JSONObject;

public class Action {
    private static final String KEY_TITLE = "title";
    private static final String KEY_VIEW_INTENT = "viewIntent";
    private static final String KEY_ENTITY_IDENTIFIER = "entityIdentifier";

    private String mTitle;
    private Intent mViewIntent;
    private int mEntityIdentifier;

    private Action(String title, Intent viewIntent, int entityIdentifier) {
        mTitle = title;
        mViewIntent = viewIntent;
        mEntityIdentifier = entityIdentifier;
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

    /**
     * Returns an identifier unique within the entity type. E.g. for episodes the TVDb id.
     * Extensions may use it to determine if there was an action already built for this
     * entity and consequently just re-send it.
     *
     * <p> SeriesGuide will use the identifier to match a published action to its request, e.g. which
     * episode the action belongs to.
     */
    public int getEntityIdentifier() {
        return mEntityIdentifier;
    }

    public static class Builder {

        private String mTitle;
        private Intent mViewIntent;
        private int mEntityIdentifier;

        /**
         * Builds an action with all required properties, ready to {@link #build()}. Most
         * extensions will want to add a {@link #viewIntent(android.content.Intent)}.
         *
         * @param title            The title to be used for the user-visible button.
         * @param entityIdentifier The entity identifier of the update request this action is
         *                         published for.
         */
        public Builder(String title, int entityIdentifier) {
            if (title == null || title.length() == 0) {
                throw new IllegalArgumentException("Title may not be null or empty.");
            }
            if (entityIdentifier <= 0) {
                throw new IllegalArgumentException(
                        "Entity identifier may not be negative or zero.");
            }
            mTitle = title;
            mEntityIdentifier = entityIdentifier;
        }

        /**
         * Sets the (optional) activity {@link Intent} that will be {@linkplain
         * android.content.Context#startActivity(Intent)
         * started} when the user clicks the button for the action.
         *
         * <p> The activity that this intent resolves to must have <code>android:exported</code> set
         * to <code>true</code>.
         */
        public Builder viewIntent(Intent viewIntent) {
            mViewIntent = viewIntent;
            return this;
        }

        public Action build() {
            return new Action(mTitle, mViewIntent, mEntityIdentifier);
        }
    }

    /**
     * Serializes this {@link Action} object to a {@link android.os.Bundle} representation.
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TITLE, mTitle);
        bundle.putString(KEY_VIEW_INTENT, (mViewIntent != null)
                ? mViewIntent.toUri(Intent.URI_INTENT_SCHEME) : null);
        bundle.putInt(KEY_ENTITY_IDENTIFIER, mEntityIdentifier);
        return bundle;
    }

    /**
     * Deserializes a {@link Bundle} into a {@link Action} object.
     */
    public static Action fromBundle(Bundle bundle) {
        String title = bundle.getString(KEY_TITLE);
        if (TextUtils.isEmpty(title)) {
            return null;
        }
        int entityIdentifier = bundle.getInt(KEY_ENTITY_IDENTIFIER);
        if (entityIdentifier <= 0) {
            return null;
        }
        Builder builder = new Builder(title, entityIdentifier);

        try {
            String viewIntent = bundle.getString(KEY_VIEW_INTENT);
            if (!TextUtils.isEmpty(viewIntent)) {
                builder.viewIntent(Intent.parseUri(viewIntent, Intent.URI_INTENT_SCHEME));
            }
        } catch (URISyntaxException ignored) {
        }

        return builder.build();
    }

    /**
     * Serializes this {@link Action} object to a {@link org.json.JSONObject} representation.
     */
    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_TITLE, mTitle);
        jsonObject.put(KEY_VIEW_INTENT, (mViewIntent != null)
                ? mViewIntent.toUri(Intent.URI_INTENT_SCHEME) : null);
        jsonObject.put(KEY_ENTITY_IDENTIFIER, mEntityIdentifier);
        return jsonObject;
    }

    /**
     * Deserializes a {@link JSONObject} into an {@link Action} object.
     */
    public static Action fromJson(JSONObject jsonObject) throws JSONException {
        String title = jsonObject.optString(KEY_TITLE);
        if (TextUtils.isEmpty(title)) {
            return null;
        }
        int entityIdentifier = jsonObject.optInt(KEY_ENTITY_IDENTIFIER);
        if (entityIdentifier <= 0) {
            return null;
        }
        Builder builder = new Builder(title, entityIdentifier);

        try {
            String viewIntent = jsonObject.optString(KEY_VIEW_INTENT);
            if (!TextUtils.isEmpty(viewIntent)) {
                builder.viewIntent(Intent.parseUri(viewIntent, Intent.URI_INTENT_SCHEME));
            }
        } catch (URISyntaxException ignored) {
        }

        return builder.build();
    }
}
