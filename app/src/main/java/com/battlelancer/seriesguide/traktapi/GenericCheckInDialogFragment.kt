package com.battlelancer.seriesguide.traktapi

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.traktapi.TraktTask.TraktActionCompleteEvent
import com.battlelancer.seriesguide.traktapi.TraktTask.TraktCheckInBlockedEvent
import com.battlelancer.seriesguide.util.Utils
import com.google.android.material.textfield.TextInputLayout
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.math.max
import kotlin.math.min

abstract class GenericCheckInDialogFragment : AppCompatDialogFragment() {

    interface InitBundle {
        companion object {
            /**
             * Title of episode or movie. **Required.**
             */
            const val ITEM_TITLE = "itemtitle"

            /**
             * Movie TMDb id. **Required for movies.**
             */
            const val MOVIE_TMDB_ID = "movietmdbid"
            const val EPISODE_ID = "episodeid"
        }
    }

    class CheckInDialogDismissedEvent

    @BindView(R.id.textInputLayoutCheckIn)
    var textInputLayout: TextInputLayout? = null

    @BindView(R.id.buttonCheckIn)
    var buttonCheckIn: View? = null

    @BindView(R.id.buttonCheckInPasteTitle)
    var buttonPasteTitle: View? = null

    @BindView(R.id.buttonCheckInClear)
    var buttonClear: View? = null

    @BindView(R.id.progressBarCheckIn)
    var progressBar: View? = null
    private var unbinder: Unbinder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // hide title, use special theme with exit animation
        setStyle(STYLE_NO_TITLE, R.style.Theme_SeriesGuide_Dialog_CheckIn)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_checkin, container, false)
        unbinder = ButterKnife.bind(this, view)

        // Paste episode button
        val itemTitle = requireArguments().getString(InitBundle.ITEM_TITLE)
        val editTextMessage = textInputLayout!!.editText
        if (!itemTitle.isNullOrEmpty()) {
            buttonPasteTitle!!.setOnClickListener {
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
        buttonClear!!.setOnClickListener {
            if (editTextMessage == null) {
                return@setOnClickListener
            }
            editTextMessage.text = null
        }

        // Checkin Button
        buttonCheckIn!!.setOnClickListener { checkIn() }

        setProgressLock(false)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // immediately start to check-in if the user has opted to skip entering a check-in message
        if (TraktSettings.useQuickCheckin(context)) {
            checkIn()
        }
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
        unbinder!!.unbind()
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
        val editText = textInputLayout!!.editText
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
        progressBar!!.visibility = if (lock) View.VISIBLE else View.GONE
        textInputLayout!!.isEnabled = !lock
        buttonPasteTitle!!.isEnabled = !lock
        buttonClear!!.isEnabled = !lock
        buttonCheckIn!!.isEnabled = !lock
    }
}