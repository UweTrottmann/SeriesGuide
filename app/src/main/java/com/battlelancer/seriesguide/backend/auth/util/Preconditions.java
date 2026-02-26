// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2016 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import com.battlelancer.seriesguide.backend.auth.FirebaseAuthUI;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;

/**
 * Precondition checking utility methods.
 */
public final class Preconditions {
    private Preconditions() {
        // Helper classes shouldn't be instantiated
    }

    /**
     * Ensures that the provided value is not null, and throws a {@link NullPointerException} if it
     * is null, with a message constructed from the provided error template and arguments.
     */
    @NonNull
    public static <T> T checkNotNull(
            @Nullable T val,
            @NonNull String errorMessageTemplate,
            @Nullable Object... errorMessageArgs) {
        if (val == null) {
            if (errorMessageArgs == null) {
                throw new NullPointerException(errorMessageTemplate);
            } else {
                throw new NullPointerException(String.format(errorMessageTemplate, errorMessageArgs));
            }
        }
        return val;
    }

    /**
     * Ensures that the provided identifier matches a known style resource, and throws an {@link
     * IllegalArgumentException} if the resource cannot be found, or is not a style resource, with a
     * message constructed from the provided error template and arguments.
     */
    @StyleRes
    public static int checkValidStyle(
            @NonNull Context context,
            int styleId,
            @NonNull String errorMessageTemplate,
            @Nullable Object... errorMessageArguments) {
        try {
            String resourceType = context.getResources().getResourceTypeName(styleId);
            if (!"style".equals(resourceType)) {
                throw new IllegalArgumentException(
                        String.format(errorMessageTemplate, errorMessageArguments));
            }
            return styleId;
        } catch (Resources.NotFoundException ex) {
            throw new IllegalArgumentException(
                    String.format(errorMessageTemplate, errorMessageArguments));
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void checkUnset(@NonNull Bundle b,
                                  @Nullable String message,
                                  @NonNull String... keys) {
        for (String key : keys) {
            if (b.containsKey(key)) { throw new IllegalStateException(message); }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void checkConfigured(@NonNull Context context,
                                       @Nullable String message,
                                       @StringRes int... ids) {
        for (int id : ids) {
            if (context.getString(id).equals(FirebaseAuthUI.UNCONFIGURED_CONFIG_VALUE)) {
                throw new IllegalStateException(message);
            }
        }
    }

    /**
     * Ensures the truth of an expression involving parameters to the calling method.
     *
     * @param expression a boolean expression
     * @param errorMessage the exception message to use if the check fails
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
