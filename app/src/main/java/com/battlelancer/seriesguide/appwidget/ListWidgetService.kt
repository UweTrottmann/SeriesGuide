package com.battlelancer.seriesguide.appwidget

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * [RemoteViewsService] that accompanies [ListWidgetProvider] by supplying
 * item contents and layouts through [ListWidgetRemoteViewsFactory].
 */
class ListWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ListWidgetRemoteViewsFactory(applicationContext, intent)
    }
}

