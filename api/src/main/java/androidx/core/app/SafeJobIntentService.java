package androidx.core.app;

import android.annotation.SuppressLint;
import android.app.job.JobServiceEngine;

/**
 * Improved version of {@link JobIntentService} which also explicitly stops de-queuing work if the
 * service is to be destroyed.
 * <p>
 * Potential fix for {@link SecurityException} when de-queuing work, assuming they are caused by
 * the processor trying to dequeue work after the service is destroyed by the system.
 * <p>
 * https://issuetracker.google.com/issues/63622293
 */
public abstract class SafeJobIntentService extends JobIntentService {

    @SuppressLint("NewApi")
    @Override
    public void onDestroy() {
        super.onDestroy();
        // stop de-queuing work if the service is to be destroyed
        if (this.mJobImpl != null) {
            ((JobServiceEngine) mJobImpl).onStopJob(null);
        }
    }
}
