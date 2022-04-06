package com.battlelancer.seriesguide.ui.shows

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.databinding.DialogRemoveBinding
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dialog to remove a show from the database.
 */
class RemoveShowDialogFragment : AppCompatDialogFragment() {

    private var binding: DialogRemoveBinding? = null
    private var showId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showId = requireArguments().getLong(ARG_LONG_SHOW_ID)
        if (showId == 0L) dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogRemoveBinding.inflate(layoutInflater)
        this.binding = binding

        showProgressBar(true)
        binding.buttonNegative.setText(android.R.string.cancel)
        binding.buttonNegative.setOnClickListener { dismiss() }
        binding.buttonPositive.setText(R.string.delete_show)

        lifecycleScope.launchWhenStarted {
            val titleOrNull = withContext(Dispatchers.IO) {
                SgRoomDatabase.getInstance(requireContext()).sgShow2Helper().getShowTitle(showId)
            }
            withContext(Dispatchers.Main) {
                if (titleOrNull == null) {
                    // Failed to find show.
                    Toast.makeText(context, R.string.delete_error, Toast.LENGTH_LONG).show()
                    dismiss()
                    return@withContext
                }
                binding.also {
                    it.textViewRemove.text = getString(R.string.confirm_delete, titleOrNull)
                    it.buttonPositive.setOnClickListener {
                        if (!SgSyncAdapter.isSyncActive(requireContext(), true)) {
                            SgApp.getServicesComponent(requireContext()).showTools()
                                .removeShow(showId)
                            dismiss()
                        }
                    }
                    showProgressBar(false)
                }
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun showProgressBar(isVisible: Boolean) {
        binding?.also {
            it.progressBarRemove.isGone = !isVisible
            it.textViewRemove.isGone = isVisible
            it.buttonPositive.isEnabled = !isVisible
        }
    }

    companion object {
        private const val ARG_LONG_SHOW_ID = "show_id"

        @JvmStatic
        fun show(showId: Long, fragmentManager: FragmentManager, context: Context): Boolean {
            if (SgSyncAdapter.isSyncActive(context, true)) {
                return false
            }
            return RemoveShowDialogFragment().apply {
                arguments = bundleOf(ARG_LONG_SHOW_ID to showId)
            }.safeShow(fragmentManager, "remove-show-dialog")
        }
    }

}