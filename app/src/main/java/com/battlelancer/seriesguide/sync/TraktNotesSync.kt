// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 Uwe Trottmann

package com.battlelancer.seriesguide.sync

import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgShow2Helper
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktTools4
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktErrorResponse
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse
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
 * Note: an initial sync will fail if the note limit of a Trakt free user would be exceeded when
 * uploading notes not on Trakt.
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
     *
     * Note: this calls [uploadNotesForShows] which may throw [InterruptedException].
     */
    @Throws(InterruptedException::class)
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
            Timber.d(
                "syncForShows: remove notes no longer on Trakt for %s shows",
                showIdsWithNotesToUploadOrRemove.size
            )
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
            Timber.d("processShowNotes: nothing to process")
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

            // Not sure how it could happen, but also update when the Trakt ID has changed to
            // always have the correct one.
            if (localShow.userNote != noteText || localShow.userNoteTraktId != noteTraktId) {
                noteUpdates[localShowId] = SgShow2Helper.NoteUpdate(noteText, noteTraktId)
            }
        }

        Timber.d("processShowNotes: updating note text or ID for %s shows", noteUpdates.size)
        showHelper.updateUserNotes(noteUpdates)
    }

    /**
     * Adds the notes at Trakt and updates the local show with the saved text (Trakt may modify it)
     * and note ID.
     *
     * Returns whether all notes were successfully uploaded.
     *
     * Note: this uses [runBlocking], so if the calling thread is interrupted this will throw
     * [InterruptedException].
     */
    @Throws(InterruptedException::class)
    private fun uploadNotesForShows(showIdsWithNotesToUpload: MutableList<Long>): Boolean {
        Timber.d("uploadNotesForShows: uploading for %s shows", showIdsWithNotesToUpload.size)

        val trakt = traktSync.trakt
        // Cache service
        val traktNotes = trakt.notes()

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

                val storedNote = runBlocking(Dispatchers.Default) {
                    val response = trakt.awaitAndHandleAuthErrorNonNull {
                        TraktTools4.saveNoteForShow(traktNotes, showTmdbId, noteText)
                    }
                    when (response) {
                        is TraktNonNullResponse.Success -> response.data

                        is TraktErrorResponse.IsAccountLimitExceeded -> {
                            traktSync.progress.setImportantErrorIfNone(
                                context.getString(R.string.trakt_error_limit_exceeded_upload)
                            )
                            null
                        }

                        // For other errors just fail to display generic error
                        is TraktErrorResponse.IsNotVip,
                        is TraktErrorResponse.IsUnauthorized,
                        is TraktErrorResponse.Other -> null
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
            Timber.d("uploadNotesForShows: uploaded for %s shows", noteUpdates.size)
            // In any case, save updates to any already sent notes
            showHelper.updateUserNotes(noteUpdates)
        }

        return true
    }

}