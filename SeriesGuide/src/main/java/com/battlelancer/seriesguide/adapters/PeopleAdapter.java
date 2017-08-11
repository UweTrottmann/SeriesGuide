package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.PeopleListHelper;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TmdbTools;
import java.util.List;

/**
 * Shows a list of people in rows with headshots, name and description.
 */
public class PeopleAdapter extends ArrayAdapter<PeopleListHelper.Person> {

    private static int LAYOUT = R.layout.item_person;

    private final LayoutInflater inflater;

    public PeopleAdapter(Context context) {
        super(context, LAYOUT);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(LAYOUT, parent, false);

            viewHolder = new ViewHolder();

            viewHolder.name = convertView.findViewById(R.id.textViewPerson);
            viewHolder.description = convertView.findViewById(
                    R.id.textViewPersonDescription);
            viewHolder.headshot = convertView.findViewById(R.id.imageViewPerson);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        PeopleListHelper.Person person = getItem(position);
        if (person == null) {
            return convertView;
        }

        // name and description
        viewHolder.name.setText(person.name);
        viewHolder.description.setText(person.description);

        // load headshot
        ServiceUtils.loadWithPicasso(getContext(),
                TmdbTools.buildProfileImageUrl(getContext(), person.profilePath,
                        TmdbTools.ProfileImageSize.W185))
                .resizeDimen(R.dimen.person_headshot_size, R.dimen.person_headshot_size)
                .centerCrop()
                .error(R.color.protection_dark)
                .into(viewHolder.headshot);

        // set unique transition names
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            viewHolder.headshot.setTransitionName("peopleAdapterPoster_" + person.tmdbId);
        }

        return convertView;
    }

    /**
     * Replace the data in this {@link android.widget.ArrayAdapter} with the given list.
     */
    public void setData(List<PeopleListHelper.Person> data) {
        clear();
        if (data != null) {
            addAll(data);
        }
    }

    public static class ViewHolder {
        public TextView name;
        public TextView description;
        public ImageView headshot;
    }
}
