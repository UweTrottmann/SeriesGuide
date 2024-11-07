// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditNoteDialogViewModel(application: Application, private val showId: Long) :
    AndroidViewModel(application) {

    data class EditNoteDialogUiState(
        val noteText: String = "",
        val noteTraktId: Long? = null,
        val isEditingEnabled: Boolean = false,
        val isNoteSaved: Boolean = false
    )

    val uiState = MutableStateFlow(EditNoteDialogUiState())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val show = SgRoomDatabase.getInstance(application)
                .sgShow2Helper()
                .getShow(showId)
            if (show != null) {
                uiState.update {
                    it.copy(
                        noteText = show.userNoteOrEmpty,
                        noteTraktId = show.userNoteTraktId,
                        isEditingEnabled = true
                    )
                }
            }
        }
    }

    fun updateNote(text: String?) {
        uiState.update {
            it.copy(noteText = text ?: "")
        }
    }

    /**
     * Saves the note, but only up to the number of allowed characters.
     *
     * Updates UI state depending on success.
     */
    fun saveNote() {
        uiState.update {
            it.copy(isEditingEnabled = false)
        }
        val noteDraft = uiState.value.noteText
        val noteTraktId = uiState.value.noteTraktId
        viewModelScope.launch {
            val result = SgApp.getServicesComponent(getApplication()).showTools()
                .storeUserNote(showId, noteDraft, noteTraktId)
            uiState.update {
                if (result != null) {
                    it.copy(
                        noteText = result.text,
                        noteTraktId = result.traktId,
                        isEditingEnabled = true,
                        isNoteSaved = true
                    )
                } else {
                    // Failed, re-enable buttons
                    it.copy(
                        isEditingEnabled = true
                    )
                }
            }
        }
    }

    companion object {

        private val KEY_SHOW_ID = object : CreationExtras.Key<Long> {}

        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY]!!
                val showId = this[KEY_SHOW_ID]!!
                EditNoteDialogViewModel(application, showId)
            }
        }

        fun creationExtras(defaultExtras: CreationExtras, showId: Long) =
            MutableCreationExtras(defaultExtras).apply {
                set(KEY_SHOW_ID, showId)
            }
    }

}