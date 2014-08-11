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
import android.os.StatFs;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.OkHttpDownloader;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Custom {@link com.squareup.picasso.OkHttpDownloader} that loads only from local cache if user
 * wishes to conserve mobile data.
 */
public class LocalOnlyOkHttpDownloader implements Downloader {

    static final int DEFAULT_READ_TIMEOUT = 20 * 1000; // 20s
    static final int DEFAULT_CONNECT_TIMEOUT = 15 * 1000; // 15s
    private static final String PICASSO_CACHE = "picasso-cache";
    private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    static final String RESPONSE_SOURCE_ANDROID = "X-Android-Response-Source";
    static final String RESPONSE_SOURCE_OKHTTP = "OkHttp-Response-Source";

    private final Context context;
    private final OkUrlFactory urlFactory;

    public LocalOnlyOkHttpDownloader(final Context context) {
        this(context, createDefaultCacheDir(context));
    }

    public LocalOnlyOkHttpDownloader(final Context context, final File cacheDir) {
        this(context, cacheDir, calculateDiskCacheSize(cacheDir));
    }

    private LocalOnlyOkHttpDownloader(final Context context, final File cacheDir,
            final long maxSize) {
        this.context = context.getApplicationContext();
        this.urlFactory = new OkUrlFactory(new OkHttpClient());
        try {
            urlFactory.client().setCache(new com.squareup.okhttp.Cache(cacheDir, maxSize));
        } catch (IOException ignored) {
        }
    }

    protected HttpURLConnection openConnection(Uri uri) throws IOException {
        HttpURLConnection connection = urlFactory.open(new URL(uri.toString()));
        connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
        return connection;
    }

    protected OkHttpClient getClient() {
        return urlFactory.client();
    }

    @Override
    public Response load(Uri uri, boolean localCacheOnly) throws IOException {
        if (!Utils.isAllowedLargeDataConnection(context, false)) {
            localCacheOnly = true;
        }

        HttpURLConnection connection = openConnection(uri);
        connection.setUseCaches(true);
        if (localCacheOnly) {
            connection.setRequestProperty("Cache-Control",
                    "only-if-cached,max-age=" + Integer.MAX_VALUE);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode >= 300) {
            connection.disconnect();
            throw new ResponseException(responseCode + " " + connection.getResponseMessage());
        }

        String responseSource = connection.getHeaderField(RESPONSE_SOURCE_OKHTTP);
        if (responseSource == null) {
            responseSource = connection.getHeaderField(RESPONSE_SOURCE_ANDROID);
        }

        boolean fromCache = parseResponseSourceHeader(responseSource);

        return new Response(connection.getInputStream(), fromCache);
    }

    static File createDefaultCacheDir(Context context) {
        File cache = new File(context.getApplicationContext().getCacheDir(), PICASSO_CACHE);
        if (!cache.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cache.mkdirs();
        }
        return cache;
    }

    static long calculateDiskCacheSize(File dir) {
        long size = MIN_DISK_CACHE_SIZE;

        try {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
            // Target 2% of the total space.
            size = available / 50;
        } catch (IllegalArgumentException ignored) {
        }

        // Bound inside min/max size for disk cache.
        return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
    }

    /** Returns {@code true} if header indicates the response body was loaded from the disk cache. */
    static boolean parseResponseSourceHeader(String header) {
        if (header == null) {
            return false;
        }
        String[] parts = header.split(" ", 2);
        if ("CACHE".equals(parts[0])) {
            return true;
        }
        if (parts.length == 1) {
            return false;
        }
        try {
            return "CONDITIONAL_CACHE".equals(parts[0]) && Integer.parseInt(parts[1]) == 304;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
