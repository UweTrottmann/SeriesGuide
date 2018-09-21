package android.support.v4.app;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Improved version of {@link JobIntentService} which actually stops de-queuing work after the
 * associated job was stopped by using {@link SafeCommandProcessor}.
 *
 * Potential fix for {@link SecurityException} when de-queuing work, assuming they are caused by
 * the processor trying to dequeue work after the job was already stopped by the system.
 *
 * https://issuetracker.google.com/issues/63622293
 */
public abstract class SafeJobIntentService extends JobIntentService {

    private SafeCommandProcessor curProcessor;

    @Override
    boolean doStopCurrentWork() {
        if (curProcessor != null) {
            curProcessor.cancel(mInterruptIfStopped);
        }
        mStopped = true;
        return onStopCurrentWork();
    }

    @Override
    void ensureProcessorRunningLocked(boolean reportStarted) {
        if (curProcessor == null) {
            curProcessor = new SafeCommandProcessor();
            if (mCompatWorkEnqueuer != null && reportStarted) {
                mCompatWorkEnqueuer.serviceProcessingStarted();
            }
            if (DEBUG) {
                Log.d(TAG, "Starting processor: " + curProcessor);
            }
            curProcessor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    void processorFinished() {
        if (mCompatQueue != null) {
            synchronized (mCompatQueue) {
                curProcessor = null;
                // The async task has finished, but we may have gotten more work scheduled in the
                // meantime.  If so, we need to restart the new processor to execute it.  If there
                // is no more work at this point, either the service is in the process of being
                // destroyed (because we called stopSelf on the last intent started for it), or
                // someone has already called startService with a new Intent that will be
                // arriving shortly.  In either case, we want to just leave the service
                // waiting -- either to get destroyed, or get a new onStartCommand() callback
                // which will then kick off a new processor.
                if (mCompatQueue != null && mCompatQueue.size() > 0) {
                    ensureProcessorRunningLocked(false);
                } else if (!mDestroyed) {
                    mCompatWorkEnqueuer.serviceProcessingFinished();
                }
            }
        }
    }

    /**
     * Copy of {@link android.support.v4.app.JobIntentService.CommandProcessor} which actually
     * checks for {@link AsyncTask#isCancelled()} as suggested by the example implementation at
     * https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/app/JobWorkService.java
     */
    final class SafeCommandProcessor extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            GenericWorkItem work;

            if (DEBUG) {
                Log.d(TAG, "Starting to dequeue work...");
            }

            // stop de-queuing work if the processor was cancelled
            while (!isCancelled() && (work = dequeueWork()) != null) {
                if (DEBUG) {
                    Log.d(TAG, "Processing next work: " + work);
                }
                onHandleWork(work.getIntent());
                if (DEBUG) {
                    Log.d(TAG, "Completing work: " + work);
                }
                work.complete();
            }

            if (DEBUG) {
                Log.d(TAG, "Done processing work!");
            }

            return null;
        }

        @Override
        protected void onCancelled(Void aVoid) {
            processorFinished();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            processorFinished();
        }
    }
}
