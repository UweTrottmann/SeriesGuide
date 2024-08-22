// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.overview

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogEditNoteBinding
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Edits a note of a show, enforcing a maximum length, allows to save or discard changes.
 */
class EditNoteDialog() : AppCompatDialogFragment() {

    constructor(showId: Long) : this() {
        arguments = bundleOf(
            ARG_SHOW_ID to showId
        )
    }

    private var binding: DialogEditNoteBinding? = null
    private val model: EditNoteDialogViewModel by viewModels(
        extrasProducer = {
            EditNoteDialogViewModel.creationExtras(
                defaultViewModelCreationExtras,
                requireArguments().getLong(ARG_SHOW_ID)
            )
        },
        factoryProducer = { EditNoteDialogViewModel.Factory }
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogEditNoteBinding.inflate(layoutInflater)
            .also { this.binding = it }

        binding.textFieldEditNote.counterMaxLength = SgShow2.MAX_USER_NOTE_LENGTH
        val editText = binding.textFieldEditNote.editText!!

        // Prevent editing until current note is loaded
        binding.textFieldEditNote.isEnabled = false

        lifecycleScope.launch {
            model.note.collect {
                editText.setText(it)
                // Enable editing once note is loaded
                binding.textFieldEditNote.isEnabled = true
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_note)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val note = editText.text.toString()
                model.saveToDatabase(note)
            }
            .setNegativeButton(android.R.string.cancel, null /* just dismiss */)
            .create()
    }

    override fun onPause() {
        super.onPause()
        // The EditText would keep the current text on config changes if an ID is set,
        // but keep it with the model instead.
        model.note.tryEmit(binding!!.textFieldEditNote.editText!!.text.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val ARG_SHOW_ID = "showId"
    }

}