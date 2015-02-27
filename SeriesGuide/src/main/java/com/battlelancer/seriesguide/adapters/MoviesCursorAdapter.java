/*
 * Copyright 2014 Uwe Trottmann
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
 */

package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import java.text.DateFormat;
import java.util.Date;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

public class MoviesCursorAdapter extends CursorAdapter {

    private final int LAYOUT = R.layout.item_movie;

    private LayoutInflater mLayoutInflater;

    private final String mImageBaseUrl;

    private DateFormat dateFormatMovieReleaseDate = DateFormat.getDateInstance(DateFormat.MEDIUM);

    private PopupMenuClickListener mPopupMenuClickListener;

    public interface PopupMenuClickListener {
        public void onPopupMenuClick(View v, int movieTmdbId);
    }

    public MoviesCursorAdapter(Context context, PopupMenuClickListener popupMenuClickListener) {
        super(context, null, 0);
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupMenuClickListener = popupMenuClickListener;

        // figure out which size of posters to load based on screen density
        if (DisplaySettings.isVeryHighDensityScreen(context)) {
            mImageBaseUrl = TmdbSettings.getImageBaseUrl(context)
                    + TmdbSettings.POSTER_SIZE_SPEC_W342;
        } else {
            mImageBaseUrl = TmdbSettings.getImageBaseUrl(context)
                    + TmdbSettings.POSTER_SIZE_SPEC_W154;
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = mLayoutInflater.inflate(LAYOUT, null);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.title = (TextView) v.findViewById(R.id.textViewMovieTitle);
        viewHolder.releaseDate = (TextView) v.findViewById(R.id.textViewMovieDate);
        viewHolder.poster = (ImageView) v.findViewById(R.id.imageViewMoviePoster);
        viewHolder.contextMenu = v.findViewById(R.id.imageViewMovieItemContextMenu);

        v.setTag(viewHolder);

        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();

        // title
        holder.title.setText(cursor.getString(MoviesQuery.TITLE));

        // release date
        long released = cursor.getLong(MoviesQuery.RELEASED_UTC_MS);
        if (released != Long.MAX_VALUE) {
            holder.releaseDate.setText(dateFormatMovieReleaseDate.format(new Date(released)));
        } else {
            holder.releaseDate.setText("");
        }

        // load poster, cache on external storage
        String posterPath = cursor.getString(MoviesQuery.POSTER);
        ServiceUtils.loadWithPicasso(context, TextUtils.isEmpty(posterPath)
                ? null : mImageBaseUrl + posterPath)
                .into(holder.poster);

        // context menu
        final int movieTmdbId = cursor.getInt(MoviesQuery.TMDB_ID);
        holder.contextMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPopupMenuClickListener != null) {
                    mPopupMenuClickListener.onPopupMenuClick(v, movieTmdbId);
                }
            }
        });
    }

    static class ViewHolder {

        TextView title;

        TextView releaseDate;

        ImageView poster;

        View contextMenu;
    }

    public interface MoviesQuery {

        String[] PROJECTION = { Movies._ID, Movies.TMDB_ID, Movies.TITLE, Movies.POSTER,
                Movies.RELEASED_UTC_MS };

        int ID = 0;
        int TMDB_ID = 1;
        int TITLE = 2;
        int POSTER = 3;
        int RELEASED_UTC_MS = 4;
    }
}
