// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.sync

import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.TimeTools
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.AddNoteRequest
import com.uwetrottmann.trakt5.entities.NoteResponse
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.entities.UserSlug
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
            showHelper.updateUserNotes(showIdsWithNotesToUploadOrRemove.associateWith { null })
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

        val noteUpdates = mutableMapOf<Long, String>()

        for (note in response) {
            val showTmdbId = note.show?.ids?.tmdb
                ?: continue // Need a TMDB ID
            val noteText = note.note?.notes
                ?: continue // Need a note

            val localShowId = tmdbIdsToLocalShowIds[showTmdbId]
                ?: continue // Show not in database
            showIdsWithNotesToUploadOrRemove.remove(localShowId)
            val localShow = showHelper.getShowWithNote(localShowId)
                ?: continue // Show was removed in the meantime

            if (localShow.userNote != noteText) {
                noteUpdates[localShowId] = noteText
            }
        }

        showHelper.updateUserNotes(noteUpdates)
    }

    /**
     * Returns whether all notes were successfully added.
     */
    private fun uploadNotesForShows(showIdsWithNotesToUpload: MutableList<Long>): Boolean {
        // Cache service
        val notes = traktSync.trakt.notes()

        for (showId in showIdsWithNotesToUpload) {

            val show = showHelper.getShowWithNote(showId)
                ?: continue // Show was removed in the meantime
            val showTmdbId = show.tmdbId
                ?: continue // Need a TMDB ID to upload
            val noteText = show.userNote
                ?: continue // Note got removed in the meantime

            try {
                val response = notes.addNote(
                    AddNoteRequest(
                        Show().apply {
                            ids = ShowIds.tmdb(showTmdbId)
                        },
                        noteText
                    )
                ).execute()
                if (!response.isSuccessful) {
                    // Note: downloaded notes before, which would have required VIP;
                    // so assume it expired when getting until this point.
                    if (TraktV2.isNotVip(response) || SgTrakt.isUnauthorized(context, response)) {
                        return false // Stop uploading
                    }
                    Errors.logAndReport("add note", response)
                    return false // Stop uploading
                }
            } catch (e: Exception) {
                Errors.logAndReport("add note", e)
                return false // Stop uploading
            }
        }

        return true
    }

}