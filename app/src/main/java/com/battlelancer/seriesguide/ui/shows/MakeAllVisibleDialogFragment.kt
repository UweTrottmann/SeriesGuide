package com.battlelancer.seriesguide.ui.shows

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Confirms before making all hidden shows visible again.
 */
class MakeAllVisibleDialogFragment : AppCompatDialogFragment(), CoroutineScope {

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var dialog: AlertDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = AlertDialog.Builder(context!!)
            .setMessage(getString(R.string.description_make_all_visible_format, "?"))
            .setPositiveButton(R.string.action_shows_make_all_visible) { _, _ -> unhideAllHiddenShows() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .create()
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        job = Job()
        launch {
            updateHiddenShowCountAsync()
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private suspend fun updateHiddenShowCountAsync() {
        val count = withContext(Dispatchers.IO) {
            SgRoomDatabase.getInstance(context!!).showHelper().countHiddenShows()
        }
        withContext(Dispatchers.Main) {
            dialog.setMessage(getString(R.string.description_make_all_visible_format, count))
        }
    }

    private fun unhideAllHiddenShows() {
        val context = context!!.applicationContext
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                SgRoomDatabase.getInstance(context).showHelper().makeHiddenVisible()
            }
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job.cancel()
    }

}