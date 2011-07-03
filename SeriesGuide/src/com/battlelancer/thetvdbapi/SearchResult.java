package com.battlelancer.thetvdbapi;

/**
 * @author trottmann.uwe
 */
public class SearchResult {

	private int id;
    private String seriesName;
	private String overview;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }
    
	public void setOverview(String overview){
		this.overview = overview;
	}

	public String getOverview() {
		return this.overview;
	}

	public SearchResult copy() {
		SearchResult copy = new SearchResult();
		copy.id = this.id;
		copy.seriesName = this.seriesName;
		copy.overview = this.overview;
		return copy;
	}

	@Override
	public String toString() {
		return this.seriesName;
	}   
	
}
