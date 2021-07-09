package com.battlelancer.seriesguide.util;

import static com.google.common.truth.Truth.assertThat;

import com.battlelancer.seriesguide.EmptyTestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = EmptyTestApplication.class)
public class ErrorsTest {

    @Test
    public void bendingStackTrace() {
        Throwable throwable = new Throwable();
        StackTraceElement[] stackTraceOriginal = throwable.getStackTrace();

        int lineNumberBelow = stackTraceOriginal[0].getLineNumber() + 4 /* lines below */;
        Errors.logAndReport("dummy", throwable);

        StackTraceElement[] stackTraceModified = throwable.getStackTrace();

        assertThat(stackTraceModified.length).isEqualTo(stackTraceOriginal.length + 1);

        StackTraceElement newElement = stackTraceModified[0];

        assertThat(newElement).isNotEqualTo(stackTraceOriginal[0]);
        for (int i = 0; i < stackTraceOriginal.length; i++) {
            assertThat(stackTraceModified[i + 1]).isEqualTo(stackTraceOriginal[i]);
        }

        assertThat(newElement.getClassName()).isEqualTo(this.getClass().getName());
        assertThat(newElement.getLineNumber()).isEqualTo(lineNumberBelow);
    }

    @Test
    public void removeErrorToolsFromStackTrace() {
        Throwable throwable = Errors.testCreateThrowable();
        StackTraceElement[] stackTraceOriginal = throwable.getStackTrace();

        Errors.removeErrorToolsFromStackTrace(throwable);

        StackTraceElement[] stackTraceModified = throwable.getStackTrace();
        assertThat(stackTraceModified.length < stackTraceOriginal.length).isTrue();
        int sizeDiff = stackTraceOriginal.length - stackTraceModified.length;
        for (int i = 0; i < stackTraceModified.length; i++) {
            assertThat(stackTraceModified[i]).isEqualTo(stackTraceOriginal[i + sizeDiff]);
        }
    }
}
