
package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.battlelancer.seriesguide.util.ImageDownloader;
import com.jakewharton.trakt.entities.Comment;
import com.uwetrottmann.seriesguide.R;

import java.util.List;

/**
 * Custom ArrayAdapter which binds {@link Comment} items to views using the
 * ViewHolder pattern and downloads avatars using the {@link ImageDownloader}.
 */
public class TraktCommentsAdapter extends ArrayAdapter<Comment> {
    private final ImageDownloader mImageDownloader;

    private final LayoutInflater mInflater;

    public TraktCommentsAdapter(Context context) {
        super(context, R.layout.shout);
        mImageDownloader = ImageDownloader.getInstance(context);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setData(List<Comment> data) {
        clear();
        if (data != null) {
            for (Comment item : data) {
                add(item);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid
        // unneccessary calls to findViewById() on each row.
        TraktCommentsAdapter.ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.shout, null);

            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.shout = (TextView) convertView.findViewById(R.id.shout);
            holder.timestamp = (TextView) convertView.findViewById(R.id.timestamp);
            holder.avatar = (ImageView) convertView.findViewById(R.id.avatar);

            convertView.setTag(holder);
        } else {
            holder = (TraktCommentsAdapter.ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        final Comment shout = getItem(position);

        holder.name.setText(shout.user.username);
        mImageDownloader.download(shout.user.avatar, holder.avatar, false);

        if (shout.spoiler) {
            holder.shout.setText(R.string.isspoiler);
        } else {
            holder.shout.setText(shout.text);
        }

        String timestamp = (String) DateUtils.getRelativeTimeSpanString(
                shout.inserted.getTimeInMillis(), System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
        holder.timestamp.setText(timestamp);

        return convertView;
    }

    static class ViewHolder {
        TextView name;

        TextView shout;

        TextView timestamp;

        ImageView avatar;
    }
}
