package com.battlelancer.seriesguide.extensions;

import android.content.Context;
import androidx.annotation.NonNull;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.List;

/**
 * Queries for any installed {@link com.battlelancer.seriesguide.api.SeriesGuideExtension}
 * extensions.
 */
class AvailableExtensionsLoader extends GenericSimpleLoader<List<Extension>> {

    AvailableExtensionsLoader(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public List<Extension> loadInBackground() {
        return ExtensionManager.get(getContext()).queryAllAvailableExtensions(getContext());
    }
}
