package com.battlelancer.seriesguide.extensions

import android.content.Context
import com.uwetrottmann.androidutils.GenericSimpleLoader

/**
 * Queries for any installed [com.battlelancer.seriesguide.api.SeriesGuideExtension]
 * extensions.
 */
class AvailableExtensionsLoader(context: Context) :
    GenericSimpleLoader<MutableList<Extension>>(context) {

    override fun loadInBackground(): MutableList<Extension> {
        return ExtensionManager.get(context).queryAllAvailableExtensions(context)
    }

}