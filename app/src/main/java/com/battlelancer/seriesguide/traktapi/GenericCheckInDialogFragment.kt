package com.battlelancer.seriesguide.traktapi

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogCheckinBinding
import com.battlelancer.seriesguide.traktapi.TraktTask.TraktActionCompleteEvent
import com.battlelancer.seriesguide.traktapi.TraktTask.TraktCheckInBlockedEvent
import com.battlelancer.seriesguide.util.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.math.max
import kotlin.math.min

abstract class GenericCheckInDialogFragment : AppCompatDialogFragment() {

    class CheckInDialogDismissedEvent

    private var binding: DialogCheckinBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogCheckinBinding.inflate(layoutInflater)
        this.binding = binding

        // Paste episode button
        val itemTitle = requireArguments().getString(ARG_ITEM_TITLE)
        val editTextMessage = binding.textInputLayoutCheckIn.editText
        if (!itemTitle.isNullOrEmpty()) {
            binding.buttonCheckInPasteTitle.setOnClickListener {
                if (editTextMessage == null) {
                    return@setOnClickListener
                }
                val start = editTextMessage.selectionStart
                val end = editTextMessage.selectionEnd
                editTextMessage.text.replace(
                    min(start, end), max(start, end),
                    itemTitle, 0, itemTitle.length
                )
            }
        }

        // Clear button
        binding.buttonCheckInClear.setOnClickListener {
            if (editTextMessage == null) {
                return@setOnClickListener
            }
            editTextMessage.text = null
        }

        // Checkin Button
        binding.buttonCheckIn.setOnClickListener { checkIn() }

        setProgressLock(false)

        lifecycleScope.launchWhenStarted {
            // immediately start to check-in if the user has opted to skip entering a check-in message
            if (TraktSettings.useQuickCheckin(requireContext())) {
                checkIn()
            }
        }

        return MaterialAlertDialogBuilder(requireContext(), R.style.Theme_SeriesGuide_Dialog_CheckIn)
            .setView(binding.root)
            .create()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        EventBus.getDefault().post(CheckInDialogDismissedEvent())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @Subscribe
    fun onEvent(event: TraktActionCompleteEvent) {
        // done with checking in, unlock UI
        setProgressLock(false)
        if (event.wasSuccessful) {
            // all went well, dismiss ourselves
            dismissAllowingStateLoss()
        }
    }

    @Subscribe
    fun onEvent(event: TraktCheckInBlockedEvent) {
        // launch a check-in override dialog
        TraktCancelCheckinDialogFragment
            .show(parentFragmentManager, event.traktTaskArgs, event.waitMinutes)
    }

    private fun checkIn() {
        // lock down UI
        setProgressLock(true)

        // connected?
        if (Utils.isNotConnected(requireContext())) {
            // no? abort
            setProgressLock(false)
            return
        }

        // launch connect flow if trakt is not connected
        if (!TraktCredentials.ensureCredentials(requireContext())) {
            // not connected? abort
            setProgressLock(false)
            return
        }

        // try to check in
        val editText = binding?.textInputLayoutCheckIn?.editText
        if (editText != null) {
            checkInTrakt(editText.text.toString())
        }
    }

    /**
     * Start the Trakt check-in task.
     */
    protected abstract fun checkInTrakt(message: String)

    /**
     * Disables all interactive UI elements and shows a progress indicator.
     */
    private fun setProgressLock(lock: Boolean) {
        val binding = binding ?: return
        binding.progressBarCheckIn.visibility = if (lock) View.VISIBLE else View.GONE
        binding.textInputLayoutCheckIn.isEnabled = !lock
        binding.buttonCheckInPasteTitle.isEnabled = !lock
        binding.buttonCheckInClear.isEnabled = !lock
        binding.buttonCheckIn.isEnabled = !lock
    }

    companion object {
        /**
         * Title of episode or movie. **Required.**
         */
        const val ARG_ITEM_TITLE = "itemtitle"

        /**
         * Movie TMDb id. **Required for movies.**
         */
        const val ARG_MOVIE_TMDB_ID = "movietmdbid"
        const val ARG_EPISODE_ID = "episodeid"
    }
}