/*
 * Copyright 2013 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.ImageDownloader;
import com.uwetrottmann.seriesguide.R;
import com.uwetrottmann.tmdb.entities.Movie;

import java.util.List;

/**
 * Displays movie titles of the given {@link Movie} array.
 */
public class MoviesAdapter extends ArrayAdapter<Movie> {

    private static int LAYOUT = R.layout.movie_item;

    private LayoutInflater mInflater;

    private ImageDownloader mImageDownloader;

    private String mBaseUrl;

    public MoviesAdapter(Context context) {
        super(context, LAYOUT);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageDownloader = ImageDownloader.getInstance(context);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mBaseUrl = prefs.getString(SeriesGuidePreferences.KEY_TMDB_BASE_URL,
                "http://cf2.imgobject.com/t/p/") + "w185";
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid
        // unneccessary calls to findViewById() on each row.
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(LAYOUT, null);

            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.textViewMovieTitle);
            holder.date = (TextView) convertView.findViewById(R.id.textViewMovieDate);
            holder.poster = (ImageView) convertView.findViewById(R.id.imageViewMoviePoster);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        Movie movie = getItem(position);

        holder.title.setText(movie.title);
        if (movie.release_date != null) {
            holder.date.setText(DateUtils.formatDateTime(getContext(),
                    movie.release_date.getTime(),
                    DateUtils.FORMAT_SHOW_DATE));
        } else {
            holder.date.setText("");
        }
        if (!TextUtils.isEmpty(movie.poster_path)) {
            String posterPath = mBaseUrl + movie.poster_path;
            mImageDownloader.download(posterPath, holder.poster, false);
        }

        return convertView;
    }

    public void setData(List<Movie> data) {
        clear();
        if (data != null) {
            for (Movie item : data) {
                if (item != null) {
                    add(item);
                }
            }
        }
    }

    static class ViewHolder {
        TextView title;
        TextView date;
        ImageView poster;
    }

}
