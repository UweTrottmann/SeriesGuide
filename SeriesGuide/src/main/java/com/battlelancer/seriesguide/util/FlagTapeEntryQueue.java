/*
 * Copyright 2013 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.content.Intent;

import com.battlelancer.seriesguide.service.TraktFlagService;
import com.google.myjson.Gson;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.FileObjectQueue.Converter;
import com.squareup.tape.InMemoryObjectQueue;
import com.squareup.tape.ObjectQueue;

import java.io.File;
import java.io.IOException;

public class FlagTapeEntryQueue implements ObjectQueue<FlagTapeEntry> {

    private static final String FILENAME = "trakt_flag_queue";

    private static FlagTapeEntryQueue _instance;

    private final ObjectQueue<FlagTapeEntry> mDelegate;

    private Context mContext;

    public static synchronized FlagTapeEntryQueue getInstance(Context context) {
        if (_instance == null) {
            // Make sure to use the application context as this is a singleton
            _instance = new FlagTapeEntryQueue(new InMemoryObjectQueue<FlagTapeEntry>(),
                    context.getApplicationContext());

            /*
             * Causes problems (MalformedJsonException, EOFException) on various
             * devices, including Google devices.
             */
            // _instance =
            // FlagTapeEntryQueue.create(context.getApplicationContext(),
            // new GsonBuilder().create());
        }
        return _instance;
    }

    private FlagTapeEntryQueue(ObjectQueue<FlagTapeEntry> delegate, Context context) {
        mDelegate = delegate;
        mContext = context;

        if (size() > 0) {
            startService();
        }
    }

    private void startService() {
        mContext.startService(new Intent(mContext, TraktFlagService.class));
    }

    @Override
    public void add(FlagTapeEntry entry) {
        mDelegate.add(entry);
        startService();
    }

    @Override
    public FlagTapeEntry peek() {
        return mDelegate.peek();
    }

    @Override
    public int size() {
        return mDelegate.size();
    }

    @Override
    public void remove() {
        mDelegate.remove();
    }

    @Override
    public void setListener(ObjectQueue.Listener<FlagTapeEntry> listener) {
        throw new UnsupportedOperationException("Listeners not yet implemented.");
    }

    private static FlagTapeEntryQueue create(Context context, Gson gson) {
        Converter<FlagTapeEntry> converter = new GsonConverter<FlagTapeEntry>(gson,
                FlagTapeEntry.class);
        File queueFile = new File(context.getFilesDir(), FILENAME);

        FileObjectQueue<FlagTapeEntry> delegate;
        try {
            delegate = new FileObjectQueue<FlagTapeEntry>(queueFile, converter);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create file queue.", e);
        }

        return new FlagTapeEntryQueue(delegate, context);
    }

}
