package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.model.HeaderData;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.ViewTools;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A sectioned {@link HistoryEntry} adapter, grouping watched items by day.
 */
public abstract class SectionedHistoryAdapter extends ArrayAdapter<HistoryEntry> implements
        StickyGridHeadersBaseAdapter {

    public interface OnItemClickListener {
        void onItemClick(View view, HistoryEntry item);
    }

    protected final LayoutInflater mInflater;
    protected final OnItemClickListener itemClickListener;
    private final VectorDrawableCompat drawableWatched;
    private final VectorDrawableCompat drawableCheckin;
    private List<HeaderData> mHeaders;
    private Calendar mCalendar;

    public SectionedHistoryAdapter(@NonNull Context context,
            OnItemClickListener itemClickListener) {
        super(context, 0);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.itemClickListener = itemClickListener;
        mCalendar = Calendar.getInstance();
        drawableWatched = ViewTools.createVectorIconInactive(getContext(),
                getContext().getTheme(),
                R.drawable.ic_check_black_16dp);
        drawableCheckin = ViewTools.createVectorIconInactive(getContext(),
                getContext().getTheme(),
                R.drawable.ic_message_black_16dp);
    }

    public void setData(List<HistoryEntry> data) {
        clear();
        if (data != null) {
            addAll(data);
        }
    }

    public VectorDrawableCompat getDrawableWatched() {
        return drawableWatched;
    }

    public VectorDrawableCompat getDrawableCheckin() {
        return drawableCheckin;
    }

    @Override
    public int getCountForHeader(int position) {
        if (mHeaders != null) {
            return mHeaders.get(position).getCount();
        }
        return 0;
    }

    @Override
    public int getNumHeaders() {
        if (mHeaders != null) {
            return mHeaders.size();
        }
        return 0;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // A ViewHolder keeps references to child views to avoid
        // unnecessary calls to findViewById() on each row.
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_history, parent, false);
            holder = new ViewHolder(convertView, itemClickListener);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HistoryEntry item = getItem(position);
        if (item == null) {
            return convertView; // all bets are off!
        }
        holder.item = item;

        // action type indicator
        if ("watch".equals(item.action)) {
            // marked watched
            holder.type.setImageDrawable(getDrawableWatched());
        } else {
            // check-in, scrobble
            holder.type.setImageDrawable(getDrawableCheckin());
        }

        bindViewHolder(holder, item);

        return convertView;
    }

    abstract void bindViewHolder(ViewHolder holder, HistoryEntry item);

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        // get header position for item position
        position = mHeaders.get(position).getRefPosition();

        HistoryEntry item = getItem(position);
        if (item == null) {
            return null;
        }

        HeaderViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_grid_header, parent, false);

            holder = new HeaderViewHolder();
            holder.day = (TextView) convertView.findViewById(R.id.textViewGridHeader);

            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        long headerTime = getHeaderTime(item);
        // display headers like "Mon in 3 days", also "today" when applicable
        holder.day.setText(
                TimeTools.formatToLocalDayAndRelativeTime(getContext(), new Date(headerTime)));

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        // re-create headers before letting notifyDataSetChanged reach the AdapterView
        mHeaders = generateHeaderList();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        // remove headers before letting notifyDataSetChanged reach the AdapterView
        mHeaders = null;
        super.notifyDataSetInvalidated();
    }

    protected List<HeaderData> generateHeaderList() {
        int count = getCount();
        if (count == 0) {
            return null;
        }

        LongSparseArray<HeaderData> mapping = new LongSparseArray<>();
        List<HeaderData> headers = new ArrayList<>();

        for (int position = 0; position < count; position++) {
            long headerId = getHeaderId(position);
            HeaderData headerData = mapping.get(headerId);
            if (headerData == null) {
                headerData = new HeaderData(position);
                headers.add(headerData);
            }
            headerData.incrementCount();
            mapping.put(headerId, headerData);
        }

        return headers;
    }

    /**
     * Maps all actions of the same day in the device time zone to the same id (which equals the
     * time in ms close to midnight of that day).
     */
    private long getHeaderId(int position) {
        HistoryEntry item = getItem(position);
        if (item != null) {
            return getHeaderTime(item);
        }
        return 0;
    }

    /**
     * Extracts the action timestamp and "rounds" it down to shortly after midnight in the current
     * device time zone.
     */
    private long getHeaderTime(HistoryEntry item) {
        mCalendar.setTimeInMillis(item.watched_at.toInstant().toEpochMilli());
        //
        mCalendar.set(Calendar.HOUR_OF_DAY, 0);
        mCalendar.set(Calendar.MINUTE, 0);
        mCalendar.set(Calendar.SECOND, 0);
        mCalendar.set(Calendar.MILLISECOND, 1);

        return mCalendar.getTimeInMillis();
    }

    static class HeaderViewHolder {

        public TextView day;
    }

    public static class ViewHolder {
        @BindView(R.id.textViewHistoryShow) TextView show;
        @BindView(R.id.textViewHistoryEpisode) TextView episode;
        @BindView(R.id.imageViewHistoryPoster) ImageView poster;
        @BindView(R.id.textViewHistoryInfo) TextView info;
        @BindView(R.id.imageViewHistoryAvatar) ImageView avatar;
        @BindView(R.id.imageViewHistoryType) ImageView type;
        HistoryEntry item;

        public ViewHolder(View itemView, final OnItemClickListener listener) {
            ButterKnife.bind(this, itemView);
            avatar.setVisibility(View.GONE);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onItemClick(v, item);
                    }
                }
            });
        }
    }
}
