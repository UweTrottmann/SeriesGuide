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
import com.battlelancer.seriesguide.shows.database.SgShow2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class EditNoteDialogViewModel(application: Application, private val showId: Long) :
    AndroidViewModel(application) {

    // Only load the current note text once on init
    val note = MutableSharedFlow<String?>(replay = 1)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val show = SgRoomDatabase.getInstance(application)
                .sgShow2Helper()
                .getShow(showId)
            if (show != null) {
                note.emit(show.userNote)
            }
        }
    }

    /**
     * Saves the note, but only up to the number of allowed characters.
     */
    fun saveToDatabase(note: String?) {
        SgApp.getServicesComponent(getApplication()).showTools()
            .storeUserNote(showId, note?.take(SgShow2.MAX_USER_NOTE_LENGTH))
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