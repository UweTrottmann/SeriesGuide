package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.model.HeaderData;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
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

    protected final LayoutInflater mInflater;

    private List<HeaderData> mHeaders;
    private Calendar mCalendar;
    private final int mResIdDrawableWatched;
    private final int mResIdDrawableCheckin;

    public SectionedHistoryAdapter(Context context) {
        super(context, 0);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCalendar = Calendar.getInstance();
        mResIdDrawableWatched = Utils.resolveAttributeToResourceId(getContext().getTheme(),
                R.attr.drawableWatch);
        mResIdDrawableCheckin = Utils.resolveAttributeToResourceId(getContext().getTheme(),
                R.attr.drawableCheckin);
    }

    public void setData(List<HistoryEntry> data) {
        clear();
        if (data != null) {
            addAll(data);
        }
    }

    public int getResIdDrawableWatched() {
        return mResIdDrawableWatched;
    }

    public int getResIdDrawableCheckin() {
        return mResIdDrawableCheckin;
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
        mCalendar.setTimeInMillis(item.watched_at.getTime());
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
}
