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

import android.content.Context;
import android.net.Uri;
import com.squareup.picasso.OkHttpDownloader;
import java.io.IOException;

/**
 * Custom {@link com.squareup.picasso.OkHttpDownloader} that loads only from local cache if user
 * wishes to conserve mobile data.
 */
public class LocalOnlyOkHttpDownloader extends OkHttpDownloader {

    private final Context mContext;

    public LocalOnlyOkHttpDownloader(Context context) {
        super(context);
        mContext = context.getApplicationContext();
    }

    @Override
    public Response load(Uri uri, boolean localCacheOnly) throws IOException {
        if (!Utils.isAllowedLargeDataConnection(mContext, false)) {
            localCacheOnly = true;
        }
        return super.load(uri, localCacheOnly);
    }
}
