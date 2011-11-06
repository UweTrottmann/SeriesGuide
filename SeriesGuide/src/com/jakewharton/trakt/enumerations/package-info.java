/**
 * <p>All Trakt-defined enumerations which are used both in request responses
 * as well as building remote requests.</p>
 *
 * <p>Serialization of these enumerations is handled by overriding the
 * <code>toString()</code> method. Additionally, all enumerations must implement
 * the {@link com.jakewharton.trakt.TraktEnumeration} interface in order to
 * be properly serialized.</p>
 *
 * <p>Deserialization of an enumerable value requires registration with the
 * GSON builder in the
 * {@link com.jakewharton.trakt.TraktApiService#getGsonBuilder()} method.
 * An appropriate method of deserialization should be handled by a static
 * method defined within the type, by convention,
 * <code>fromString(String)</code>.</p>
 */
package com.jakewharton.trakt.enumerations;