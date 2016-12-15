package com.battlelancer.seriesguide.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.tmdb2.entities.Movie;
import java.text.DateFormat;
import java.util.List;

/**
 * Displays movie titles of the given {@link Movie} array.
 */
public class MoviesAdapter extends ArrayAdapter<Movie> {

    private final LayoutInflater inflater;

    private String imageBaseUrl;
    private DateFormat dateFormatMovieReleaseDate = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private PopupMenuClickListener popupMenuClickListener;

    public interface PopupMenuClickListener {
        void onPopupMenuClick(View v, int movieTmdbId);
    }

    public MoviesAdapter(Context context, PopupMenuClickListener listener) {
        super(context, 0);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        popupMenuClickListener = listener;

        // figure out which size of posters to load based on screen density
        if (DisplaySettings.isVeryHighDensityScreen(context)) {
            imageBaseUrl = TmdbSettings.getImageBaseUrl(context)
                    + TmdbSettings.POSTER_SIZE_SPEC_W342;
        } else {
            imageBaseUrl = TmdbSettings.getImageBaseUrl(context)
                    + TmdbSettings.POSTER_SIZE_SPEC_W154;
        }
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid
        // unnecessary calls to findViewById() on each row.
        ViewHolder holder;

        if (convertView == null) {
            // do not use parent layout params to avoid padding issues
            convertView = inflater.inflate(R.layout.item_movie, null);

            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.textViewMovieTitle);
            holder.date = (TextView) convertView.findViewById(R.id.textViewMovieDate);
            holder.poster = (ImageView) convertView.findViewById(R.id.imageViewMoviePoster);
            holder.contextMenu = (ImageView) convertView
                    .findViewById(R.id.imageViewMovieItemContextMenu);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        Movie movie = getItem(position);
        if (movie == null) {
            return convertView;
        }

        holder.title.setText(movie.title);
        if (movie.release_date != null) {
            holder.date.setText(dateFormatMovieReleaseDate.format(movie.release_date));
        } else {
            holder.date.setText("");
        }

        // poster
        // use fixed size so bitmaps can be re-used on config change
        ServiceUtils.loadWithPicasso(getContext(), imageBaseUrl + movie.poster_path)
                .resizeDimen(R.dimen.movie_poster_width, R.dimen.movie_poster_height)
                .centerCrop()
                .into(holder.poster);

        // context menu
        final int movieTmdbId = movie.id;
        holder.contextMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popupMenuClickListener != null) {
                    popupMenuClickListener.onPopupMenuClick(v, movieTmdbId);
                }
            }
        });

        // set unique transition names
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.poster.setTransitionName("moviesAdapterPoster_" + position);
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

    public static class ViewHolder {
        public TextView title;
        public TextView date;
        public ImageView poster;
        public ImageView contextMenu;
    }
}
