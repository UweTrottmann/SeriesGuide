// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.sync

import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgShow2Helper
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktTools2
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.TimeTools
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.NoteResponse
import com.uwetrottmann.trakt5.entities.UserSlug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.threeten.bp.OffsetDateTime
import timber.log.Timber

/**
 * Syncs notes, currently only for shows, with Trakt.
 *
 * This will do nothing if the user is not a Trakt VIP.
 */
class TraktNotesSync(
    private val traktSync: TraktSync
) {
    private val context = traktSync.context
    private val showHelper = SgRoomDatabase.getInstance(context).sgShow2Helper()

    /**
     * When a note is new or different at Trakt, updates the local show (if it is added).
     * So if the local show has a note, it is overwritten (server is source of truth).
     * When a note is removed (or rather does not exist) at Trakt, will either on the initial sync
     * upload the note or for consecutive syncs remove the note on the local show.
     */
    fun syncForShows(updatedAt: OffsetDateTime?): Boolean {
        if (updatedAt == null) {
            Timber.e("syncForShows: null updatedAt")
            return false
        }

        val isInitialSync = TraktSettings.isInitialSyncShowNotes(context)

        // Do not sync if notes have not changed, or is not initial sync
        val lastUpdatedAt = TraktSettings.getLastNotesUpdatedAt(context)
        if (!isInitialSync && !TimeTools.isAfterMillis(updatedAt, lastUpdatedAt)) {
            Timber.d("syncForShows: no changes since %tF %tT", lastUpdatedAt, lastUpdatedAt)
            return true
        }

        // As this runs only if a note changes, optimize for memory, not for CPU
        val tmdbIdsToLocalShowIds = traktSync.showTools2.getTmdbIdsToShowIds()
        val showIdsWithNotesToUploadOrRemove = showHelper.getShowIdsWithNotes()

        val action = "get notes"
        var page = 1
        var pageCount: Int
        do {
            try {
                val response = traktSync.users
                    .notes(UserSlug.ME, "shows", page, null, null)
                    .execute()

                if (!response.isSuccessful) {
                    if (TraktV2.isNotVip(response)) {
                        // Do not error; also do not set initial sync complete or last updated in
                        // case the user gets VIP later, or loses VIP and gets it again.
                        return true
                    }
                    if (SgTrakt.isUnauthorized(context, response)) {
                        return false
                    }
                    Errors.logAndReport(action, response)
                    return false
                }

                val noteResponses = response.body()
                if (noteResponses == null) {
                    Errors.logAndReport(action, response, "body is null")
                    return false
                }

                processShowNotes(
                    noteResponses,
                    tmdbIdsToLocalShowIds,
                    showIdsWithNotesToUploadOrRemove
                )

                pageCount = TraktV2.getPageCount(response) ?: 1
                page++
            } catch (e: Exception) {
                Errors.logAndReport(action, e)
                return false
            }
        } while (page <= pageCount)

        if (isInitialSync) {
            if (!uploadNotesForShows(showIdsWithNotesToUploadOrRemove)) return false
        } else {
            // Remove notes from shows that are not on Trakt, meaning their note got removed
            showHelper.updateUserNotes(showIdsWithNotesToUploadOrRemove
                .associateWith { SgShow2Helper.NoteUpdate("", null) })
        }

        if (isInitialSync) {
            TraktSettings.setInitialSyncShowNotesCompleted(context)
        }
        TraktSettings.storeLastNotesUpdatedAt(context, updatedAt)

        return true
    }

    private fun processShowNotes(
        response: List<NoteResponse>,
        tmdbIdsToLocalShowIds: Map<Int, Long>,
        showIdsWithNotesToUploadOrRemove: MutableList<Long>
    ) {
        if (response.isEmpty()) {
            return
        }

        val noteUpdates = mutableMapOf<Long, SgShow2Helper.NoteUpdate>()

        for (note in response) {
            val showTmdbId = note.show?.ids?.tmdb
                ?: continue // Need a TMDB ID
            val noteText = note.note?.notes
                ?: continue // Need a note
            val noteTraktId = note.note?.id
                ?: continue // Need its Trakt ID

            val localShowId = tmdbIdsToLocalShowIds[showTmdbId]
                ?: continue // Show not in database
            showIdsWithNotesToUploadOrRemove.remove(localShowId)
            val localShow = showHelper.getShowWithNote(localShowId)
                ?: continue // Show was removed in the meantime

            if (localShow.userNote != noteText) {
                noteUpdates[localShowId] = SgShow2Helper.NoteUpdate(noteText, noteTraktId)
            }
        }

        showHelper.updateUserNotes(noteUpdates)
    }

    /**
     * Adds the notes at Trakt and updates the local show with the saved text (Trakt may modify it)
     * and note ID.
     *
     * Returns whether all notes were successfully uploaded.
     */
    private fun uploadNotesForShows(showIdsWithNotesToUpload: MutableList<Long>): Boolean {
        // Cache service
        val traktNotes = traktSync.trakt.notes()

        val noteUpdates = mutableMapOf<Long, SgShow2Helper.NoteUpdate>()
        try {
            for (showId in showIdsWithNotesToUpload) {
                val show = showHelper.getShowWithNote(showId)
                    ?: continue // Show was removed in the meantime
                val showTmdbId = show.tmdbId
                    ?: continue // Need a TMDB ID to upload
                val noteText = show.userNote
                    ?.ifBlank { null } // Trakt does not allow blank text
                    ?: continue // Note got removed in the meantime

                // If this thread is interrupted throws InterruptedException
                val storedNote = runBlocking(Dispatchers.Default) {
                    val response = TraktTools2
                        .saveNoteForShow(traktNotes, showTmdbId, noteText)
                    when (response) {
                        is TraktTools2.TraktResponse.Success -> response.data
                        // Note: if failing due to not VIP, downloaded notes before, which would
                        // have required VIP; so assume it expired when getting until this point.
                        else -> null
                    }
                }

                if (storedNote == null) {
                    return false // Stop uploading
                }

                val storedText = storedNote.notes ?: ""
                val noteTraktId = storedNote.id
                noteUpdates[showId] = SgShow2Helper.NoteUpdate(storedText, noteTraktId)
            }
        } finally {
            // In any case, save updates to any already sent notes
            showHelper.updateUserNotes(noteUpdates)
        }

        return true
    }

}