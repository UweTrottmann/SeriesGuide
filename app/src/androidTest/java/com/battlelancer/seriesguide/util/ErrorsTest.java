package com.battlelancer.seriesguide.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class ErrorsTest {

    @Test
    public void bendingStackTrace() {
        Throwable throwable = new Throwable();
        StackTraceElement[] stackTraceOriginal = throwable.getStackTrace();

        int lineNumberBelow = 16;
        Errors.bendCauseStackTrace(throwable);

        StackTraceElement[] stackTraceModified = throwable.getStackTrace();

        assertEquals(stackTraceOriginal.length + 1, stackTraceModified.length);

        StackTraceElement newElement = stackTraceModified[0];

        assertNotEquals(stackTraceOriginal[0], newElement);
        for (int i = 0; i < stackTraceOriginal.length; i++) {
            assertEquals(stackTraceOriginal[i], stackTraceModified[i + 1]);
        }

        assertEquals(this.getClass().getName(), newElement.getClassName());
        assertEquals(lineNumberBelow, newElement.getLineNumber());
    }

    @Test
    public void removeErrorToolsFromStackTrace() {
        Throwable throwable = new Throwable();
        StackTraceElement[] stackTraceOriginal = throwable.getStackTrace();

        Errors.removeErrorToolsFromStackTrace(throwable);

        StackTraceElement[] stackTraceModified = throwable.getStackTrace();
        assertEquals(stackTraceOriginal.length - Errors.CALL_STACK_INDEX, stackTraceModified.length);
        for (int i = 0; i < stackTraceModified.length; i++) {
            assertEquals(stackTraceOriginal[i + Errors.CALL_STACK_INDEX], stackTraceModified[i]);
        }
    }

}
