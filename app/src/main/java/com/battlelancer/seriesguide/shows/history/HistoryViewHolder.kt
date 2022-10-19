package com.battlelancer.seriesguide.shows.history;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import com.battlelancer.seriesguide.databinding.ItemHistoryBinding;
import com.battlelancer.seriesguide.util.ImageTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.SgPicassoRequestHandler;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import java.util.Date;

/**
 * Binds either a show or movie history item.
 */
public class HistoryViewHolder extends RecyclerView.ViewHolder {
    private final ItemHistoryBinding binding;

    public HistoryViewHolder(ItemHistoryBinding binding,
            final NowAdapter.ItemClickListener listener) {
        super(binding.getRoot());
        this.binding = binding;
        binding.constaintLayoutHistory.setOnClickListener(v -> {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && listener != null) {
                listener.onItemClick(v, position);
            }
        });
        // Only used in history-only view.
        binding.textViewHistoryHeader.setVisibility(View.GONE);
    }

    private void bindCommonInfo(Context context, NowAdapter.NowItem item, Drawable drawableWatched,
            Drawable drawableCheckin) {
        String time = TimeTools.formatToLocalRelativeTime(context, new Date(item.timestamp));
        if (item.type == NowAdapter.ItemType.HISTORY) {
            // user history entry
            binding.imageViewHistoryAvatar.setVisibility(View.GONE);
            binding.textViewHistoryInfo.setText(time);
        } else {
            // friend history entry
            binding.imageViewHistoryAvatar.setVisibility(View.VISIBLE);
            binding.textViewHistoryInfo.setText(TextTools.dotSeparate(item.username, time));

            // user avatar
            ServiceUtils.loadWithPicasso(context, item.avatar)
                    .into(binding.imageViewHistoryAvatar);
        }

        // action type indicator (only if showing Trakt history)
        ImageView typeView = binding.imageViewHistoryType;
        if (NowAdapter.TRAKT_ACTION_WATCH.equals(item.action)) {
            typeView.setImageDrawable(drawableWatched);
            typeView.setVisibility(View.VISIBLE);
        } else if (item.action != null) {
            // check-in, scrobble
            typeView.setImageDrawable(drawableCheckin);
            typeView.setVisibility(View.VISIBLE);
        } else {
            typeView.setVisibility(View.GONE);
        }
        // Set disabled for darker icon (non-interactive).
        typeView.setEnabled(false);
    }

    public void bindToShow(Context context, NowAdapter.NowItem item, Drawable drawableWatched,
            Drawable drawableCheckin) {
        bindCommonInfo(context, item, drawableWatched, drawableCheckin);

        ImageTools.loadShowPosterUrlResizeSmallCrop(context, binding.imageViewHistoryPoster,
                item.posterUrl);

        binding.textViewHistoryShow.setText(item.title);
        binding.textViewHistoryEpisode.setText(item.description);
    }

    public void bindToMovie(Context context, NowAdapter.NowItem item, Drawable drawableWatched,
            Drawable drawableCheckin) {
        bindCommonInfo(context, item, drawableWatched, drawableCheckin);

        // TMDb poster (resolved on demand as Trakt does not have them)
        ImageTools.loadShowPosterUrlResizeSmallCrop(context, binding.imageViewHistoryPoster,
                SgPicassoRequestHandler.SCHEME_MOVIE_TMDB + "://" + item.movieTmdbId);

        binding.textViewHistoryShow.setText(item.title);
    }
}
