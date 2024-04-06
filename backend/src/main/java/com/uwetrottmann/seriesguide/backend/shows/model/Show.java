/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * This code was generated by https://github.com/google/apis-client-generator/
 * (build: 1969-12-31 23:59:59 UTC)
 * on 2023-08-04 at 10:37:28 UTC 
 * Modify at your own risk.
 */

package com.uwetrottmann.seriesguide.backend.shows.model;

/**
 * Model definition for Show.
 *
 * <p> This is the Java data model class that specifies how to parse/serialize into the JSON that is
 * transmitted over HTTP when working with the shows. For a detailed explanation see:
 * <a href="https://developers.google.com/api-client-library/java/google-http-java-client/json">https://developers.google.com/api-client-library/java/google-http-java-client/json</a>
 * </p>
 *
 * @author Google, Inc.
 */
@SuppressWarnings("javadoc")
public final class Show extends com.google.api.client.json.GenericJson {

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private com.google.api.client.util.DateTime createdAt;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.String id;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Boolean isFavorite;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Boolean isHidden;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Boolean isRemoved;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.String language;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.String note;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Boolean notify;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Integer tvdbId;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private com.google.api.client.util.DateTime updatedAt;

  /**
   * @return value or {@code null} for none
   */
  public com.google.api.client.util.DateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * @param createdAt createdAt or {@code null} for none
   */
  public Show setCreatedAt(com.google.api.client.util.DateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.String getId() {
    return id;
  }

  /**
   * @param id id or {@code null} for none
   */
  public Show setId(java.lang.String id) {
    this.id = id;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Boolean getIsFavorite() {
    return isFavorite;
  }

  /**
   * @param isFavorite isFavorite or {@code null} for none
   */
  public Show setIsFavorite(java.lang.Boolean isFavorite) {
    this.isFavorite = isFavorite;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Boolean getIsHidden() {
    return isHidden;
  }

  /**
   * @param isHidden isHidden or {@code null} for none
   */
  public Show setIsHidden(java.lang.Boolean isHidden) {
    this.isHidden = isHidden;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Boolean getIsRemoved() {
    return isRemoved;
  }

  /**
   * @param isRemoved isRemoved or {@code null} for none
   */
  public Show setIsRemoved(java.lang.Boolean isRemoved) {
    this.isRemoved = isRemoved;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.String getLanguage() {
    return language;
  }

  /**
   * @param language language or {@code null} for none
   */
  public Show setLanguage(java.lang.String language) {
    this.language = language;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.String getNote() {
    return note;
  }

  /**
   * @param note note or {@code null} for none
   */
  public Show setNote(java.lang.String note) {
    this.note = note;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Boolean getNotify() {
    return notify;
  }

  /**
   * @param notify notify or {@code null} for none
   */
  public Show setNotify(java.lang.Boolean notify) {
    this.notify = notify;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Integer getTvdbId() {
    return tvdbId;
  }

  /**
   * @param tvdbId tvdbId or {@code null} for none
   */
  public Show setTvdbId(java.lang.Integer tvdbId) {
    this.tvdbId = tvdbId;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public com.google.api.client.util.DateTime getUpdatedAt() {
    return updatedAt;
  }

  /**
   * @param updatedAt updatedAt or {@code null} for none
   */
  public Show setUpdatedAt(com.google.api.client.util.DateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  @Override
  public Show set(String fieldName, Object value) {
    return (Show) super.set(fieldName, value);
  }

  @Override
  public Show clone() {
    return (Show) super.clone();
  }

}