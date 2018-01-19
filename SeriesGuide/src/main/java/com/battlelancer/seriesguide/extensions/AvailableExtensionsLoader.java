package com.battlelancer.seriesguide.extensions;

import android.content.Context;
import android.support.annotation.NonNull;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.List;

/**
 * Queries for any installed {@link com.battlelancer.seriesguide.api.SeriesGuideExtension}
 * extensions.
 */
class AvailableExtensionsLoader extends GenericSimpleLoader<List<ExtensionManager.Extension>> {

    AvailableExtensionsLoader(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public List<ExtensionManager.Extension> loadInBackground() {
        return ExtensionManager.get().queryAllAvailableExtensions(getContext());
    }
}
