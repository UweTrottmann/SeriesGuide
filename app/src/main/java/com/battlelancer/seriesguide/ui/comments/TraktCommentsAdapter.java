
package com.battlelancer.seriesguide.ui.comments;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.trakt5.entities.Comment;
import java.util.List;

/**
 * Custom ArrayAdapter which binds {@link Comment} items to views using the ViewHolder pattern.
 */
class TraktCommentsAdapter extends ArrayAdapter<Comment> {

    TraktCommentsAdapter(Context context) {
        super(context, R.layout.item_comment);
    }

    void setData(List<Comment> data) {
        clear();
        if (data != null) {
            for (Comment item : data) {
                add(item);
            }
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid
        // unnecessary calls to findViewById() on each row.
        TraktCommentsAdapter.ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment, parent, false);

            holder = new ViewHolder();
            holder.username = convertView.findViewById(R.id.textViewCommentUsername);
            holder.comment = convertView.findViewById(R.id.textViewComment);
            holder.timestamp = convertView.findViewById(R.id.textViewCommentTimestamp);
            holder.replies = convertView.findViewById(R.id.textViewCommentReplies);
            holder.avatar = convertView.findViewById(R.id.imageViewCommentAvatar);

            convertView.setTag(holder);
        } else {
            holder = (TraktCommentsAdapter.ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        final Comment comment = getItem(position);
        if (comment == null) {
            return convertView;
        }

        String name = null;
        String avatarPath = null;
        if (comment.user != null) {
            name = comment.user.username;
            if (comment.user.images != null && comment.user.images.avatar != null) {
                avatarPath = comment.user.images.avatar.full;
            }
        }
        holder.username.setText(name);
        ServiceUtils.loadWithPicasso(getContext(), avatarPath).into(holder.avatar);

        if (comment.spoiler) {
            holder.comment.setText(R.string.isspoiler);
            TextViewCompat.setTextAppearance(holder.comment,
                    R.style.TextAppearance_SeriesGuide_Body2_Error);
        } else {
            holder.comment.setText(comment.comment);
            TextViewCompat.setTextAppearance(holder.comment, R.style.TextAppearance_SeriesGuide_Body2);
        }

        String timestamp = (String) DateUtils.getRelativeTimeSpanString(
                comment.created_at.toInstant().toEpochMilli(), System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
        holder.timestamp.setText(timestamp);

        if (comment.replies == null || comment.replies <= 0) {
            // no replies
            holder.replies.setVisibility(View.GONE);
        } else {
            holder.replies.setVisibility(View.VISIBLE);
            holder.replies.setText(getContext().getResources()
                    .getQuantityString(R.plurals.replies_plural, comment.replies, comment.replies));
        }

        return convertView;
    }

    static class ViewHolder {
        TextView username;
        TextView comment;
        TextView timestamp;
        TextView replies;
        ImageView avatar;
    }
}
