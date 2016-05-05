/*
 * Copyright 2010 Kevin Gaudin
 * Copyright 2016 Uwe Trottmann
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import timber.log.Timber;

public class LogCatCollector {

    /**
     * Default number of latest lines kept from the logcat output.
     */
    private static final int DEFAULT_TAIL_COUNT = 100;
    private static final int DEFAULT_BUFFER_SIZE_IN_BYTES = 8192;

    /**
     * Executes the logcat command and returns the last 100 entries.
     */
    public static String collectLogCat() {
        final List<String> commandLine = new ArrayList<>();
        commandLine.add("logcat");
        commandLine.add("-t");
        commandLine.add(String.valueOf(DEFAULT_TAIL_COUNT));
        commandLine.add("-v");
        commandLine.add("time");

        final LinkedList<String> logcatBuf = new BoundedLinkedList<>(DEFAULT_TAIL_COUNT);

        try {
            final Process process = Runtime.getRuntime()
                    .exec(commandLine.toArray(new String[commandLine.size()]));

            Timber.d("collectLogCat: retrieving logcat output...");

            // Dump stderr to null
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        streamToString(process.getErrorStream());
                    } catch (IOException ignored) {
                    }
                }
            }).start();

            logcatBuf.add(streamToString(process.getInputStream()));
        } catch (IOException e) {
            Timber.e(e, "collectLogCat: could not retrieve data.");
        }

        return logcatBuf.toString();
    }

    /**
     * Reads an InputStream into a string
     *
     * @param input the stream
     * @return the read string
     */
    @NonNull
    private static String streamToString(@NonNull InputStream input) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input),
                DEFAULT_BUFFER_SIZE_IN_BYTES);
        try {
            String line;
            final List<String> buffer = new LinkedList<>();
            while ((line = reader.readLine()) != null) {
                if (!line.contains("ConnectivityManager")) {
                    buffer.add(line);
                }
            }
            return TextUtils.join("\n", buffer);
        } finally {
            safeClose(reader);
        }
    }

    private static void safeClose(@Nullable Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }
}
