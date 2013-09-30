
package com.battlelancer.seriesguide.items;

/**
 * Object representation of a show for easier handling in code.
 */
public class Series {

    private String id;

    private String language;

    private String title;

    private String banner;

    private String overview;

    private String firstAired;

    private String imdbId;

    private String actors;

    private String airsDayOfWeek;

    private long airsTime;

    private String contentRating;

    private String genres;

    private String network;

    private String rating;

    private String runtime;

    private int status;

    private String fanart;

    private String lastUpdated;

    private String poster;

    private long nextEpisode;

    private long lastEdit;

    public String getId() {
        return id;
    }

    /**
     * Equivalent to the theTVDb.com series id.
     * 
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getFirstAired() {
        return firstAired;
    }

    public void setFirstAired(String firstAired) {
        this.firstAired = firstAired;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public String getActors() {
        return actors;
    }

    public void setActors(String actors) {
        this.actors = actors;
    }

    public String getAirsDayOfWeek() {
        return airsDayOfWeek;
    }

    public void setAirsDayOfWeek(String airsDayOfWeek) {
        this.airsDayOfWeek = airsDayOfWeek;
    }

    public long getAirsTime() {
        return airsTime;
    }

    public void setAirsTime(long l) {
        this.airsTime = l;
    }

    public String getContentRating() {
        return contentRating;
    }

    public void setContentRating(String contentRating) {
        this.contentRating = contentRating;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    /**
     * The shows running status: returns 1 for Continuing and 0 for Ended. Might
     * be -1 if the status could not be parsed.
     */
    public int getStatus() {
        return status;
    }

    /**
     * The shows running status: set 1 for Continuing and 0 for Ended. Set to -1
     * if the status could not be parsed.
     */
    public void setStatus(int status) {
        this.status = status;
    }

    public String getFanart() {
        return fanart;
    }

    public void setFanart(String fanart) {
        this.fanart = fanart;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    @Override
    public String toString() {
        return this.title;
    }

    public void setNextEpisode(long nextEpisode) {
        this.nextEpisode = nextEpisode;
    }

    public long getNextEpisode() {
        return nextEpisode;
    }

    public long getLastEdit() {
        return lastEdit;
    }

    public void setLastEdit(long lastEdit) {
        this.lastEdit = lastEdit;
    }

}
