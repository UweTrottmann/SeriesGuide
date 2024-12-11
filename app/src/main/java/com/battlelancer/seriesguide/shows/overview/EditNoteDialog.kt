// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.overview

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogEditNoteBinding
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import timber.log.Timber

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

        // Text field
        binding.textFieldEditNote.counterMaxLength = SgShow2.MAX_USER_NOTE_LENGTH
        val editText = binding.textFieldEditNote.editText!!
        // Disable save button if text is too long to save
        editText.doAfterTextChanged { text ->
            setSaveEnabled(model.uiState.value.isEditingEnabled, text.textHasNoError())
        }

        // Buttons
        // Can not use dialog buttons as they dismiss the dialog right away,
        // but need to keep it visible if saving fails.
        binding.buttonPositive.apply {
            setText(R.string.action_save)
            setOnClickListener {
                model.updateNote(editText.text?.toString())
                model.saveNote()
            }
        }
        binding.buttonNegative.apply {
            setText(android.R.string.cancel)
            setOnClickListener { dismiss() }
        }

        // UI state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.uiState.collect { state ->
                    Timber.d("Display note")
                    editText.setText(state.noteText)
                    setViewsEnabled(state.isEditingEnabled, editText.text.textHasNoError())
                    if (state.isNoteSaved) {
                        dismiss()
                    }
                }
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_edit_note)
            .setView(binding.root)
            .create()
    }

    private fun setViewsEnabled(isEditingEnabled: Boolean, hasNoError: Boolean) {
        binding?.apply {
            textFieldEditNote.isEnabled = isEditingEnabled
            setSaveEnabled(isEditingEnabled, hasNoError)
        }
    }

    private fun Editable?.textHasNoError(): Boolean {
        val textLength = this?.length ?: 0
        return textLength <= SgShow2.MAX_USER_NOTE_LENGTH
    }

    private fun setSaveEnabled(isEditingEnabled: Boolean, hasNoError: Boolean) {
        binding?.buttonPositive?.isEnabled = isEditingEnabled && hasNoError
    }

    override fun onPause() {
        super.onPause()
        // Note: can not update using TextWatcher due to infinite loop
        model.updateNote(binding?.textFieldEditNote?.editText?.text?.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val ARG_SHOW_ID = "showId"
    }

}