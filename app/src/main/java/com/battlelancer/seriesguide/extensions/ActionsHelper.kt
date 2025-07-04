// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2025 Uwe Trottmann

package com.battlelancer.seriesguide.extensions

import android.content.Intent
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.TooltipCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.startActivityWithAnimation
import timber.log.Timber

object ActionsHelper {

    /**
     * Replaces all child views of the given [android.view.ViewGroup] with a [android.widget.Button]
     * per action plus one linking to [com.battlelancer.seriesguide.extensions.ExtensionsConfigurationActivity].
     * Sets up [android.view.View.OnClickListener] if [com.battlelancer.seriesguide.api.Action.getViewIntent]
     * of an [com.battlelancer.seriesguide.api.Action] is not null.
     */
    fun populateActions(
        layoutInflater: LayoutInflater,
        theme: Resources.Theme,
        actionsContainer: ViewGroup?,
        data: List<Action>?
    ) {
        if (actionsContainer == null) {
            // nothing to do, view is already gone
            Timber.d("populateActions: action view container gone, aborting")
            return
        }
        actionsContainer.removeAllViews()

        // add a view per action
        data?.forEach { action ->
            val actionView = layoutInflater.inflate(R.layout.item_action, actionsContainer, false) as Button
            actionView.text = action.title

            TooltipCompat.setTooltipText(actionView, action.title)

            val viewIntent = action.viewIntent
            if (viewIntent != null) {
                viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                actionView.setOnClickListener { v ->
                    Utils.tryStartActivity(v.context, viewIntent, true)
                }
            }

            actionsContainer.addView(actionView)
        }

        // link to extensions configuration
        val configureView = layoutInflater.inflate(R.layout.item_action_add, actionsContainer, false) as Button
        configureView.setText(R.string.action_extensions_configure)
        configureView.setOnClickListener { v ->
            val intent = Intent(v.context, ExtensionsConfigurationActivity::class.java)
            v.context.startActivityWithAnimation(intent, v)
        }
        actionsContainer.addView(configureView)
    }
}
