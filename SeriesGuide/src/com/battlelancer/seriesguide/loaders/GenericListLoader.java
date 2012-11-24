
package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import java.util.List;

/**
 * A generic {@link AsyncTaskLoader} loading any {@link List} of things. It
 * takes care of delivering and reseting results, so you only have to implement
 * <code>loadInBackground()</code>.
 */
public abstract class GenericListLoader<T> extends AsyncTaskLoader<List<T>> {

    protected List<T> mItems;

    public GenericListLoader(Context context) {
        super(context);
    }

    /**
     * Called when there is new data to deliver to the client. The super class
     * will take care of delivering it; the implementation here just adds a
     * little more logic.
     */
    @Override
    public void deliverResult(List<T> items) {
        if (isReset()) {
            // An async query came in while the loader is stopped. We
            // don't need the result.
            if (items != null) {
                onReleaseResources(items);
            }
        }
        List<T> oldItems = items;
        mItems = items;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(items);
        }

        if (oldItems != null) {
            onReleaseResources(oldItems);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mItems != null) {
            deliverResult(mItems);
        } else {
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<T> items) {
        super.onCanceled(items);

        onReleaseResources(items);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release resources
        if (mItems != null) {
            onReleaseResources(mItems);
            mItems = null;
        }
    }

    /**
     * Helper function to take care of releasing resources associated with an
     * actively loaded data set.
     */
    protected void onReleaseResources(List<T> apps) {
        // For a simple List<> there is nothing to do. For something
        // like a Cursor, we would close it here.
    }

}
