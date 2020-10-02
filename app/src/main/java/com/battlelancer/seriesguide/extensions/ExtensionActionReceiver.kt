package com.battlelancer.seriesguide.extensions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.api.constants.IncomingConstants
import com.battlelancer.seriesguide.api.constants.OutgoingConstants
import com.battlelancer.seriesguide.api.constants.OutgoingConstants.ACTION_PUBLISH_ACTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Receives a published extension action and processes it in a coroutine.
 */
class ExtensionActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        if (ACTION_PUBLISH_ACTION == intent.action) {
            SgApp.coroutineScope.launch {
                withContext(Dispatchers.Default) {
                    // An extension published a new action.
                    val token = intent.getStringExtra(IncomingConstants.EXTRA_TOKEN)

                    // Extract the action.
                    var action: Action? = null
                    if (intent.hasExtra(OutgoingConstants.EXTRA_ACTION)) {
                        val bundle = intent.getBundleExtra(OutgoingConstants.EXTRA_ACTION)
                        if (bundle != null) {
                            action = Action.fromBundle(bundle)
                        }
                    }

                    // Extensions may send either movie or episode actions as of API 1.3.0.
                    var type = OutgoingConstants.ACTION_TYPE_EPISODE
                    if (intent.hasExtra(OutgoingConstants.EXTRA_ACTION_TYPE)) {
                        type = intent.getIntExtra(
                            OutgoingConstants.EXTRA_ACTION_TYPE,
                            OutgoingConstants.ACTION_TYPE_EPISODE
                        )
                    }

                    ExtensionManager.get(context)
                        .handlePublishedAction(appContext, token, action, type)
                }
            }
        }
    }

}
