package com.battlelancer.seriesguide.ui.shows

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Confirms before making all hidden shows visible again.
 */
class MakeAllVisibleDialogFragment : AppCompatDialogFragment() {

    private lateinit var dialog: AlertDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.description_make_all_visible_format, "?"))
            .setPositiveButton(R.string.action_shows_make_all_visible) { _, _ ->
                SgApp.getServicesComponent(requireContext()).showTools().storeAllHiddenVisible()
                dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .create()
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        lifecycleScope.launch {
            updateHiddenShowCountAsync()
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @SuppressLint("StringFormatMatches") // Int as format arg is intentional.
    private suspend fun updateHiddenShowCountAsync() {
        val count = withContext(Dispatchers.IO) {
            SgRoomDatabase.getInstance(requireContext()).sgShow2Helper().countHiddenShows()
        }
        withContext(Dispatchers.Main) {
            dialog.setMessage(getString(R.string.description_make_all_visible_format, count))
        }
    }

}