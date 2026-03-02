// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.dataliberation

import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import java.io.BufferedWriter
import java.io.OutputStream

/**
 * Transforms and writes items one by one as JSON to the given output stream.
 *
 * Adds a new line between each item to make files (especially with many items) easier to open and
 * edit. This is a compromise between writing a single line and pretty formatting with indents that
 * is still fast. This is also compliant with [RFC 8259](https://www.rfc-editor.org/rfc/rfc8259):
 * "Insignificant whitespace is allowed before or after any of the six structural characters."
 */
class SgJsonWriter<T, O>(
    private val itemsToTransform: List<T>,
    private val outputClass: Class<O>,
    private val outputStream: OutputStream,
    private val transform: (T) -> O,
) {

    suspend fun write(progressCallback: suspend (Int, Int) -> Unit) {
        val gson = Gson()
        val outputWriter = outputStream.bufferedWriter()
        val jsonWriter = JsonWriter(outputWriter)

        val countTotal = itemsToTransform.size
        var countDone = 0

        progressCallback(countTotal, countDone)

        jsonWriter.beginArray()

        for (item in itemsToTransform) {
            // Write each item on a new line. Due to how JsonWriter works new lines for all but the
            // first item will start with a comma followed by the JSON object. This is an OK
            // trade-off to writing a completely custom JSON writer.
            outputWriter.unixLineFeed()

            val transformedItem = transform(item)

            // Note: Gson caches the type adapters
            gson.toJson(transformedItem, outputClass, jsonWriter)

            progressCallback(countTotal, ++countDone)
        }

        // If items were written, write the end-array character on a new line
        if (itemsToTransform.isNotEmpty()) {
            outputWriter.unixLineFeed()
        }

        jsonWriter.endArray()
        jsonWriter.close()
    }

    private fun BufferedWriter.unixLineFeed() {
        write("\n")
    }

}