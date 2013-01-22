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

import android.os.Handler;
import android.os.Looper;

import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktException;
import com.squareup.tape.Task;

public class FlagTapedTask implements Task<FlagTapedTask.Callback> {
    private static final long serialVersionUID = -7699031818939574225L;

    public interface Callback {
        void onSuccess();

        void onFailure();
    }

    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());

    private TraktApiBuilder<Void> mBuilder;

    public FlagTapedTask(TraktApiBuilder<Void> builder) {
        mBuilder = builder;
    }

    @Override
    public void execute(final Callback callback) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    mBuilder.fire();

                    // Get back to the main thread before invoking a callback.
                    MAIN_THREAD.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess();
                        }
                    });
                } catch (TraktException e) {
                    postFailure(callback);
                } catch (ApiException e) {
                    postFailure(callback);
                }
            }

            public void postFailure(final Callback callback) {
                // Get back to the main thread before invoking a callback.
                MAIN_THREAD.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure();
                    }
                });
            }
        }).start();
    }

}
