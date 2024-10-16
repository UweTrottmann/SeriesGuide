// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.ui.menus

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.core.view.MenuProvider
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.sync.SgSyncAdapter

/**
 * Adds actions for manual syncing at the last position in the menu.
 *
 * As this requires a [context], probably only instantiate when it is available,
 * for example using lazy initialization.
 */
open class ManualSyncMenu(
    private val context: Context,
    @MenuRes private val menuRes: Int
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(menuRes, menu)

        menu.addSubMenu(Menu.NONE, 0, Menu.CATEGORY_SECONDARY, R.string.sync_manually).apply {
            add(Menu.NONE, MENU_ITEM_SYNC_UPDATE_ID, Menu.NONE, R.string.sync_and_update)
            add(Menu.NONE, MENU_ITEM_SYNC_DOWNLOAD_ID, Menu.NONE, R.string.sync_and_download_all)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            MENU_ITEM_SYNC_UPDATE_ID -> {
                SgSyncAdapter.requestSyncDeltaImmediate(context, true)
                true
            }

            MENU_ITEM_SYNC_DOWNLOAD_ID -> {
                SgSyncAdapter.requestSyncFullImmediate(context, true)
                true
            }

            else -> false
        }
    }

    companion object {
        // Choose IDs unlikely to collide with generated menu item IDs
        private const val MENU_ITEM_SYNC_UPDATE_ID = 1
        private const val MENU_ITEM_SYNC_DOWNLOAD_ID = 2
    }

}