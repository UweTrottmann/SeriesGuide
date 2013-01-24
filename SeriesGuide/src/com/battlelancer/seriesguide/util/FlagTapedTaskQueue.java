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
import com.google.myjson.GsonBuilder;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.FileObjectQueue.Converter;
import com.squareup.tape.ObjectQueue;
import com.squareup.tape.TaskQueue;

import java.io.File;
import java.io.IOException;

public class FlagTapedTaskQueue extends TaskQueue<FlagTapedTask> {

    private static final String FILENAME = "trakt_flag_task_queue";

    private static FlagTapedTaskQueue _instance;

    private Context mContext;

    public static synchronized FlagTapedTaskQueue getInstance(Context context) {
        if (_instance == null) {
            _instance = FlagTapedTaskQueue.create(context, new GsonBuilder().create());
        }
        return _instance;
    }

    private FlagTapedTaskQueue(ObjectQueue<FlagTapedTask> delegate, Context context) {
        super(delegate);
        mContext = context;

        if (size() > 0) {
            startService();
        }
    }

    private void startService() {
        mContext.startService(new Intent(mContext, TraktFlagService.class));
    }

    @Override
    public void add(FlagTapedTask entry) {
        super.add(entry);
        startService();
    }

    private static FlagTapedTaskQueue create(Context context, Gson gson) {
        Converter<FlagTapedTask> converter = new GsonConverter<FlagTapedTask>(gson,
                FlagTapedTask.class);
        File queueFile = new File(context.getFilesDir(), FILENAME);
        FileObjectQueue<FlagTapedTask> delegate;
        try {
            delegate = new FileObjectQueue<FlagTapedTask>(queueFile, converter);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create file queue.", e);
        }
        return new FlagTapedTaskQueue(delegate, context);
    }

}
